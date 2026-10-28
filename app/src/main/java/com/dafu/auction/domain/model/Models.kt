package com.dafu.auction.domain.model

import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor

/**
 * 藏品分类
 */
enum class ItemCategory {
    RED,      // 大红
    PURPLE,   // 紫件
    GOLD;     // 大金

    val label: String
        get() = when (this) {
            RED -> "大红"
            PURPLE -> "紫件"
            GOLD -> "大金"
        }

    val key: String
        get() = when (this) {
            RED -> "red"
            PURPLE -> "purple"
            GOLD -> "gold"
        }

    companion object {
        fun fromKey(key: String): ItemCategory = when (key.lowercase(Locale.ROOT)) {
            "red", "r" -> RED
            "purple", "p" -> PURPLE
            "gold", "g" -> GOLD
            else -> RED
        }
    }
}

/**
 * 藏品基础数据
 *
 * @param name 藏品名称
 * @param category 分类
 * @param bonus 大红/紫件加成（单位：万）
 * @param price 大金价格（单位：元）
 * @param builtIn 是否内置数据（内置数据不可删除）
 */
data class CatalogItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: ItemCategory,
    val bonus: Double = 0.0,
    val price: Double = 0.0,
    val builtIn: Boolean = false
) {
    val isGold: Boolean get() = category == ItemCategory.GOLD

    /** 价格折算为“万” */
    fun priceWan(): Double = price / 10000.0

    /** 统一折算为“万”，便于展示比较 */
    fun displayWan(): Double = if (isGold) priceWan() else bonus

    /** 展示文本，例如 “12万” / “20.28万” */
    fun displayText(): String = if (isGold) {
        "¥" + formatNumber(price)
    } else {
        formatWan(bonus) + "万"
    }

    companion object {
        /** 保留两位小数并去掉多余的 0 */
        fun formatWan(value: Double): String = formatNumber(value)

        fun formatNumber(value: Double): String {
            val rounded = Math.round(value * 100.0) / 100.0
            return if (abs(rounded - floor(rounded)) < 1e-9) {
                Math.round(rounded).toString()
            } else {
                String.format(Locale.CHINA, "%.2f", rounded).trimEnd('0').trimEnd('.')
            }
        }
    }
}

/**
 * 单档建议出价
 */
data class BidSuggestion(
    val tier: BidTier,
    val value: Double
)

/**
 * 普通估价结果
 */
data class EstimationResult(
    val goldRedCount: Int,
    val multiplier: Int,
    /** 金红数量 × 倍率 */
    val baseValue: Double,
    /** 已选大红/紫件加成合计（万） */
    val bonusTotal: Double,
    /** 最终估价（万） */
    val estimateValue: Double,
    /** 浮动范围（万） */
    val fluctuationRange: Int,
    /** 各档位建议出价（万） */
    val bidTiers: List<BidSuggestion>
) {
    fun bidOf(tier: BidTier): Double = bidTiers.firstOrNull { it.tier == tier }?.value ?: 0.0
}

/**
 * 反推结果中，某个大金的具体件数
 */
data class InferPart(
    val itemId: String,
    val name: String,
    val count: Int,
    val unitPriceWan: Double
) {
    fun totalWan(): Double = unitPriceWan * count
}

/**
 * 反推搜索结果
 */
data class InferResult(
    val index: Int = 0,
    /** 组合件数 n */
    val itemCount: Int,
    /** 组合总价（元） */
    val totalPrice: Long,
    /** 组合均价（万） */
    val averagePrice: Double,
    /** 误差 = 均价 - 读数（万） */
    val error: Double,
    /** 组合明细 */
    val composition: List<InferPart> = emptyList()
) {
    val absoluteError: Double get() = abs(error)

    fun compositionText(): String =
        if (composition.isEmpty()) "-"
        else composition.joinToString("，") { "${it.name} ×${it.count}" }

    fun totalWan(): Double = totalPrice / 10000.0
}

/**
 * 结果 Tab
 */
enum class ResultTab(val label: String) {
    PRICE_DETAIL("价格明细"),
    GOLD_SEEN("所见大金"),
    SUGGESTED_BID("建议出价"),
    INFER_TABLE("反推表")
}

/**
 * 主题
 */
enum class ThemeType(val label: String, val key: String) {
    LIGHT("浅色", "light"),
    DARK("深色", "dark"),
    AUTO("跟随系统", "auto");

    companion object {
        fun fromKey(key: String?): ThemeType = values().firstOrNull { it.key == key } ?: AUTO
    }
}

/**
 * 反推表排序方式
 */
enum class SortMode(val label: String) {
    DIFF_ASC("误差↑"),
    DIFF_DESC("误差↓"),
    PRICE_ASC("总价↑"),
    PRICE_DESC("总价↓"),
    COUNT_ASC("件数↑"),
    COUNT_DESC("件数↓");

    companion object {
        fun fromKey(key: String?): SortMode = values().firstOrNull { it.name == key } ?: DIFF_ASC
    }
}

/**
 * 估值档位
 */
enum class BidTier(val tierIndex: Int) {
    CONSERVATIVE(0),  // 保守
    STEADY(1),        // 稳妥
    ESTIMATE(2),      // 按估价
    AGGRESSIVE(3),    // 激进
    HIGH_BID(4),      // 高竞价
    SPRINT(5),        // 冲刺
    LIMIT(6);         // 极限

    val label: String
        get() = when (this) {
            CONSERVATIVE -> "保守"
            STEADY -> "稳妥"
            ESTIMATE -> "按估价"
            AGGRESSIVE -> "激进"
            HIGH_BID -> "高竞价"
            SPRINT -> "冲刺"
            LIMIT -> "极限"
        }
}

/**
 * 应用状态（用户输入 + 结果）
 */
data class AuctionState(
    // ---- 输入 ----
    val totalInput: String = "",            // 紫金红总数
    val purpleInput: String = "",           // 紫件数
    val goldAverageInput: String = "",      // 金均读数（万）
    val unknownRedInput: String = "",       // 未知红件数

    // ---- 已选藏品 ----
    val selectedItems: Map<String, Int> = emptyMap(),   // <藏品 id, 件数>（大红/紫件）
    val seenGolds: Map<String, Int> = emptyMap(),       // <大金 id, 件数>（已见大金）

    // ---- 藏品目录 ----
    val redCatalog: List<CatalogItem> = emptyList(),
    val goldCatalog: List<CatalogItem> = emptyList(),

    // ---- 设置 ----
    val multiplier: Int = 8,                 // 倍率 ×8 / ×10
    val fluctuationRange: Int = 10,          // 出价浮动范围（万）
    val inferTolerance: Double = 5.0,        // 反推允许误差（万）
    val inferSeconds: Int = 5,               // 反推限时（秒）
    val minGoldCount: Int = 1,               // 最低金藏品件数
    val zeroLimit: Int = 50,                 // 零误差条目上限
    val errorLimit: Int = 50,                // 带误差条目上限
    val berserkMode: Boolean = false,        // 狂暴模式
    val sortMode: SortMode = SortMode.DIFF_ASC,
    val theme: ThemeType = ThemeType.AUTO,
    val isInferMode: Boolean = false,        // 是否反推模式

    // ---- 输出 ----
    val estimation: EstimationResult? = null,
    val inferResults: List<InferResult> = emptyList(),
    val inferMessage: String = "",
    val isCalculating: Boolean = false,
    val calculationElapsedMs: Long = 0L,

    // ---- UI ----
    val activeTab: ResultTab = ResultTab.PRICE_DETAIL,
    val sheetExpanded: Boolean = false,
    val showNotice: Boolean = false,
    val showSettings: Boolean = false,
    val message: String? = null
) {
    // ---- 派生值 ----
    val totalCount: Int? get() = totalInput.trim().toIntOrNull()
    val purpleCount: Int? get() = purpleInput.trim().toIntOrNull()
    val goldAverage: Double? get() = goldAverageInput.trim().toDoubleOrNull()
    val unknownRedCount: Int get() = (unknownRedInput.trim().toIntOrNull() ?: 0).coerceAtLeast(0)

    /** 金红数 = 总数 - 紫件数 */
    val goldRedCount: Int?
        get() {
            val total = totalCount ?: return null
            if (total <= 0) return null
            val purple = purpleCount ?: 0
            val result = total - purple
            return if (result >= 0) result else null
        }

    val selectedRedCount: Int get() = selectedItems.values.sum()

    val seenGoldCount: Int get() = seenGolds.values.sum()

    val canEstimate: Boolean get() = goldRedCount != null

    val canInfer: Boolean get() = goldRedCount != null && (goldAverage ?: 0.0) > 0.0

    /** 已选藏品加成（万），未知红件按 0 计算 */
    val selectedBonusTotal: Double
        get() = selectedItems.entries.sumOf { (id, count) ->
            (redCatalog.firstOrNull { it.id == id }?.bonus ?: 0.0) * count
        }

    fun countOf(itemId: String): Int = selectedItems[itemId] ?: 0

    fun seenCountOf(itemId: String): Int = seenGolds[itemId] ?: 0
}
