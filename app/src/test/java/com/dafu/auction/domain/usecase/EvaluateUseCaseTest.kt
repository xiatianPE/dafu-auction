package com.dafu.auction.domain.usecase

import com.dafu.auction.domain.model.BidTier
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.CatalogSeed
import com.dafu.auction.domain.model.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluateUseCaseTest {

    private val useCase = EvaluateUseCase()
    private val catalog = CatalogSeed.redItems()

    @Test
    fun `无加成时估价等于金红数乘倍率`() {
        val result = useCase.execute(
            goldRedCount = 20,
            multiplier = 8,
            fluctuationRange = 10,
            selectedItems = emptyMap(),
            catalog = catalog
        )
        assertEquals(160.0, result.estimateValue, 1e-6)
        assertEquals(0.0, result.bonusTotal, 1e-6)
    }

    @Test
    fun `加成按件数累加`() {
        val eternal = catalog.first { it.name == "永恒之心" }
        val result = useCase.execute(
            goldRedCount = 10,
            multiplier = 10,
            fluctuationRange = 10,
            selectedItems = mapOf(eternal.id to 2),
            catalog = catalog
        )
        assertEquals(240.0, result.bonusTotal, 1e-6)
        assertEquals(100.0 + 240.0, result.estimateValue, 1e-6)
    }

    @Test
    fun `大金不计入加成`() {
        val gold = CatalogItem(
            id = "g1",
            name = "大饼",
            category = ItemCategory.GOLD,
            price = 202_798.0
        )
        val result = useCase.execute(
            goldRedCount = 5,
            multiplier = 8,
            fluctuationRange = 10,
            selectedItems = mapOf(gold.id to 3),
            catalog = catalog
        )
        assertEquals(0.0, result.bonusTotal, 1e-6)
        assertEquals(40.0, result.estimateValue, 1e-6)
    }

    @Test
    fun `各档位围绕估价展开`() {
        val result = useCase.execute(
            goldRedCount = 10,
            multiplier = 8,
            fluctuationRange = 10,
            selectedItems = emptyMap(),
            catalog = catalog
        )
        assertEquals(70.0, result.bidOf(BidTier.CONSERVATIVE), 1e-6)
        assertEquals(80.0, result.bidOf(BidTier.ESTIMATE), 1e-6)
        assertEquals(100.0, result.bidOf(BidTier.LIMIT), 1e-6)
    }
}
