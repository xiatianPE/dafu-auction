package com.dafu.auction.domain.usecase

import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.InferPart
import com.dafu.auction.domain.model.InferResult
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min

/**
 * 金均反推引擎
 *
 * 思路：
 * 1. 大金按“价格元”分组（同价的大金合并处理），把指数级的排列压缩成“每组取几件”的解空间；
 * 2. 每组件数上限 = 该组件数 × [MAX_PER_ITEM]；已见大金作为固定项参与统计；
 * 3. DFS 枚举 + 上下界剪枝（剩余最大总价 / 目标区间），边搜边用大顶堆保留“误差最小”的 N 条；
 * 4. 命中的组合再按件数上限展开成具体大金明细。
 *
 * 结果统一以“万”为单位展示，误差 = 组合均价 - 用户读数。
 */
class InferEngine {

    /** 搜索结果 */
    data class SearchResult(
        val solutions: List<InferResult> = emptyList(),
        val zeroFound: Boolean = false,
        val message: String = "",
        val elapsedMs: Long = 0L,
        val truncated: Boolean = false
    )

    /**
     * 搜索可行组合
     *
     * @param goldRedCount 金红数（总数 - 紫件数），作为组合件数上限
     * @param avgWan 金均读数（万）
     * @param goldItems 可选的大金列表（price 单位：元）
     * @param seenCounts 已确认看到的大金 <id, 件数>，这些会作为固定项直接参与计算
     * @param cfg 计算配置
     */
    fun search(
        goldRedCount: Int,
        avgWan: Double,
        goldItems: List<CatalogItem>,
        seenCounts: Map<String, Int> = emptyMap(),
        cfg: CalcConfig = CalcConfig()
    ): SearchResult {
        val startAt = System.currentTimeMillis()

        if (goldRedCount < 1) {
            return SearchResult(message = "金红数至少为 1 件")
        }
        if (!(avgWan > 0.0)) {
            return SearchResult(message = "请输入有效的金均读数")
        }

        val usable = goldItems.filter { it.isGold && it.price > 0.0 }
        if (usable.isEmpty()) {
            return SearchResult(message = "请先添加大金价格数据")
        }

        val maxN = min(goldRedCount, MAX_ITEM_COUNT)
        val minN = cfg.minGoldCount.coerceIn(1, maxN)

        // ---- 固定项（已见大金） ----
        var forcedCount = 0
        var forcedSum = 0L
        val forcedParts = ArrayList<InferPart>()
        val freeItems = ArrayList<CatalogItem>(usable.size)

        for (item in usable) {
            val count = (seenCounts[item.id] ?: 0).coerceIn(0, MAX_PER_ITEM)
            if (count > 0) {
                forcedCount += count
                forcedSum += item.price.toLong() * count
                forcedParts += InferPart(item.id, item.name, count, item.priceWan())
            } else {
                freeItems += item
            }
        }

        if (forcedCount > maxN) {
            return SearchResult(
                message = "已见大金共 $forcedCount 件，超过金红数 $maxN 件"
            )
        }

        // ---- 按价格分组，同价合并以压缩搜索空间 ----
        val groups = buildGroups(freeItems)
        val groupCount = groups.size
        val prices = LongArray(groupCount) { groups[it].priceYuan }
        val maxCounts = IntArray(groupCount) { groups[it].maxCount }

        // 后缀可加最大件数 / 最大总价（用于剪枝）
        val suffixCount = IntArray(groupCount + 1)
        val suffixMaxSum = LongArray(groupCount + 1)
        for (g in groupCount - 1 downTo 0) {
            suffixCount[g] = suffixCount[g + 1] + maxCounts[g]
            suffixMaxSum[g] = suffixMaxSum[g + 1] + maxCounts[g] * prices[g]
        }

        val avgYuan = avgWan * 10000.0
        val tolYuan = cfg.toleranceWan.coerceAtLeast(0.0) * 10000.0
        // 件数 n 对应的合法总价区间（元）
        val lowerBound = LongArray(maxN + 1) { n -> Math.round(n * (avgYuan - tolYuan)) }
        val upperBound = LongArray(maxN + 1) { n -> Math.round(n * (avgYuan + tolYuan)) }

        val capacity = (cfg.zeroLimit + cfg.errorLimit).coerceAtLeast(MIN_CAPACITY)
        val heap = PriorityQueue<InferResult>(capacity + 1, compareByDescending<InferResult> { it.absoluteError })

        val counts = IntArray(groupCount)
        var nodes = 0L
        var truncated = false
        val deadline = startAt + cfg.maxMillis.coerceAtLeast(200L)

        fun record(taken: Int, sum: Long) {
            if (taken < minN || taken > maxN) return
            val avg = sum.toDouble() / taken / 10000.0
            val error = avg - avgWan
            if (abs(error) > cfg.toleranceWan + EPS) return

            val composition = ArrayList<InferPart>(forcedParts.size + groupCount)
            composition.addAll(forcedParts)
            for (g in 0 until groupCount) {
                var left = counts[g]
                if (left <= 0) continue
                for (item in groups[g].items) {
                    if (left <= 0) break
                    val take = min(MAX_PER_ITEM, left)
                    composition += InferPart(item.id, item.name, take, item.priceWan())
                    left -= take
                }
            }
            composition.sortByDescending { it.unitPriceWan }

            val result = InferResult(
                itemCount = taken,
                totalPrice = sum,
                averagePrice = avg,
                error = error,
                composition = composition
            )
            if (heap.size < capacity) {
                heap.offer(result)
            } else {
                val worst = heap.peek()
                if (worst == null || result.absoluteError < worst.absoluteError) {
                    heap.poll()
                    heap.offer(result)
                }
            }
        }

        fun dfs(g: Int, taken: Int, sum: Long) {
            if (truncated) return
            nodes++
            if (nodes % 4096L == 0L) {
                if (nodes > cfg.maxNodes) {
                    truncated = true
                    return
                }
                if (System.currentTimeMillis() > deadline) {
                    truncated = true
                    return
                }
            }

            val minFinalCount = if (taken > minN) taken else minN
            if (minFinalCount > maxN) return
            // 即使后面全选最贵的也够不到最低要求 → 剪枝
            if (sum + suffixMaxSum[g] < lowerBound[minFinalCount]) return

            val maxFinalCount = min(maxN, taken + suffixCount[g])
            if (maxFinalCount < minFinalCount) return
            // 当前总价已经超过最大件数下的上界 → 剪枝
            if (sum > upperBound[maxFinalCount]) return

            if (g == groupCount) {
                record(taken, sum)
                return
            }

            val price = prices[g]
            for (c in 0..maxCounts[g]) {
                counts[g] = c
                dfs(g + 1, taken + c, sum + price * c)
                if (truncated) break
            }
            counts[g] = 0
        }

        dfs(0, forcedCount, forcedSum)

        // ---- 结果整理：零误差优先，其次误差最小 ----
        val sorted = heap.toList().sortedWith(
            compareBy({ it.absoluteError }, { it.itemCount }, { it.totalPrice })
        )
        val zeroResults = sorted.filter { it.absoluteError < EPS }
        val errorResults = sorted.filter { it.absoluteError >= EPS }

        val picked = ArrayList<InferResult>(cfg.zeroLimit + cfg.errorLimit)
        picked.addAll(zeroResults.take(cfg.zeroLimit.coerceAtLeast(0)))
        picked.addAll(errorResults.take(cfg.errorLimit.coerceAtLeast(0)))

        val indexed = picked.mapIndexed { index, result ->
            result.copy(index = index + 1)
        }

        val elapsed = System.currentTimeMillis() - startAt
        val message = buildMessage(
            size = indexed.size,
            zeroCount = zeroResults.size,
            toleranceWan = cfg.toleranceWan,
            truncated = truncated
        )

        return SearchResult(
            solutions = indexed,
            zeroFound = zeroResults.isNotEmpty(),
            message = message,
            elapsedMs = elapsed,
            truncated = truncated
        )
    }

    private fun buildMessage(
        size: Int,
        zeroCount: Int,
        toleranceWan: Double,
        truncated: Boolean
    ): String {
        if (size == 0) {
            return "未找到符合条件的组合，试试放宽误差范围（当前 ±${CatalogItem.formatWan(toleranceWan)}万）"
        }
        val base = if (zeroCount > 0) {
            "找到 $zeroCount 个零误差组合，共 $size 条结果"
        } else {
            "未找到零误差组合，以下为误差最小的 $size 条结果"
        }
        return if (truncated) "$base（搜索量较大，结果可能不完整，可开启狂暴模式）" else base
    }

    /** 按价格分组（价格降序） */
    private fun buildGroups(items: List<CatalogItem>): List<Group> {
        if (items.isEmpty()) return emptyList()
        val sorted = items.sortedByDescending { it.price }
        val groups = ArrayList<Group>()
        var current = sorted.first()
        var bucket = ArrayList<CatalogItem>()
        bucket += current
        for (item in sorted.drop(1)) {
            if (item.price.toLong() == current.price.toLong()) {
                bucket += item
            } else {
                groups += Group(current.price.toLong(), bucket)
                current = item
                bucket = ArrayList<CatalogItem>()
                bucket += item
            }
        }
        groups += Group(current.price.toLong(), bucket)
        return groups
    }

    private class Group(val priceYuan: Long, val items: List<CatalogItem>) {
        val maxCount: Int = items.size * MAX_PER_ITEM
    }

    companion object {
        /** 每种大金最多 3 件 */
        const val MAX_PER_ITEM = 3

        /** 组合件数上限，避免搜索失控 */
        const val MAX_ITEM_COUNT = 90

        private const val MIN_CAPACITY = 40

        /** 判定“零误差”的阈值（万），1 元以内视为精确命中 */
        private const val EPS = 1e-4
    }
}

/**
 * 反推计算配置
 *
 * @param toleranceWan 允许的误差范围（万）
 * @param maxMillis 单次搜索的最长耗时
 * @param maxNodes 搜索节点上限，防止极端数据卡死
 */
data class CalcConfig(
    val minGoldCount: Int = 1,
    val zeroLimit: Int = 50,
    val errorLimit: Int = 50,
    val berserkMode: Boolean = false,
    val toleranceWan: Double = 5.0,
    val maxMillis: Long = 5_000L,
    val maxNodes: Long = 2_000_000L
) {
    companion object {
        fun fromSettings(
            minGoldCount: Int,
            zeroLimit: Int,
            errorLimit: Int,
            berserkMode: Boolean,
            toleranceWan: Double,
            seconds: Int
        ): CalcConfig = CalcConfig(
            minGoldCount = minGoldCount,
            zeroLimit = zeroLimit,
            errorLimit = errorLimit,
            berserkMode = berserkMode,
            toleranceWan = toleranceWan,
            maxMillis = (seconds.coerceIn(1, 60) * 1000L),
            maxNodes = if (berserkMode) 8_000_000L else 2_000_000L
        )
    }
}
