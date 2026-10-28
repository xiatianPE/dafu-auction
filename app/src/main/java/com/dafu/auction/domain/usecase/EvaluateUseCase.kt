package com.dafu.auction.domain.usecase

import com.dafu.auction.domain.model.BidSuggestion
import com.dafu.auction.domain.model.BidTier
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.EstimationResult
import com.dafu.auction.domain.model.ItemCategory
import kotlin.math.roundToLong

/**
 * 普通估价器
 *
 * 估价 = 金红数 × 倍率 + 已选大红/紫件加成合计
 * 金红数 = 紫金红总数 - 紫件数
 */
class EvaluateUseCase {

    /**
     * @param selectedItems <藏品 id, 件数>
     * @param catalog 大红/紫件目录（用于查加成）
     */
    fun execute(
        goldRedCount: Int,
        multiplier: Int,
        fluctuationRange: Int,
        selectedItems: Map<String, Int>,
        catalog: List<CatalogItem>
    ): EstimationResult {
        val safeCount = goldRedCount.coerceAtLeast(0)
        val safeMultiplier = multiplier.coerceAtLeast(0)

        var bonus = 0.0
        for ((itemId, count) in selectedItems) {
            if (count <= 0) continue
            val item = catalog.firstOrNull { it.id == itemId } ?: continue
            if (item.category == ItemCategory.GOLD) continue
            bonus += item.bonus * count
        }

        val base = safeCount.toDouble() * safeMultiplier
        val estimate = base + bonus
        val bids = calculateBids(estimate, fluctuationRange.coerceAtLeast(0))

        return EstimationResult(
            goldRedCount = safeCount,
            multiplier = safeMultiplier,
            baseValue = base,
            bonusTotal = bonus,
            estimateValue = estimate,
            fluctuationRange = fluctuationRange.coerceAtLeast(0),
            bidTiers = bids
        )
    }

    /** 各档位建议出价（万） */
    private fun calculateBids(estimate: Double, range: Int): List<BidSuggestion> {
        val f = range.toDouble()
        val half = f / 2.0
        return listOf(
            BidSuggestion(BidTier.CONSERVATIVE, clamp(estimate - f)),
            BidSuggestion(BidTier.STEADY, clamp(estimate - half)),
            BidSuggestion(BidTier.ESTIMATE, clamp(estimate)),
            BidSuggestion(BidTier.AGGRESSIVE, clamp(estimate + half)),
            BidSuggestion(BidTier.HIGH_BID, clamp(estimate + f)),
            BidSuggestion(BidTier.SPRINT, clamp(estimate + f * 1.5)),
            BidSuggestion(BidTier.LIMIT, clamp(estimate + f * 2.0))
        )
    }

    private fun clamp(value: Double): Double {
        if (value <= 0.0) return 0.0
        // 保留一位小数，便于阅读
        return (value * 10.0).roundToLong() / 10.0
    }
}
