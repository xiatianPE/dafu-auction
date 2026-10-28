package com.dafu.auction.domain.usecase

import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InferEngineTest {

    private fun gold(name: String, price: Long) = CatalogItem(
        id = "gold_$name",
        name = name,
        category = ItemCategory.GOLD,
        price = price.toDouble(),
        builtIn = true
    )

    private val items = listOf(
        gold("a", 120_000L),
        gold("b", 40_000L),
        gold("c", 20_000L),
        gold("d", 10_000L)
    )

    @Test
    fun `找到零误差组合`() {
        // 4万 × 3 = 12万，均价正好 4万；也存在单件 4万 的零误差解
        val result = InferEngine().search(
            goldRedCount = 3,
            avgWan = 4.0,
            goldItems = items,
            cfg = CalcConfig(toleranceWan = 0.1)
        )
        assertTrue(result.zeroFound)
        val threeItems = result.solutions.firstOrNull { it.itemCount == 3 }
        assertNotNull("应存在 3 件组合", threeItems)
        assertEquals(0.0, threeItems!!.error, 1e-4)
        assertEquals(120_000L, threeItems.totalPrice)
    }

    @Test
    fun `零误差结果均价等于读数`() {
        val result = InferEngine().search(
            goldRedCount = 5,
            avgWan = 2.4,
            goldItems = items,
            cfg = CalcConfig(toleranceWan = 0.2)
        )
        assertTrue(result.solutions.isNotEmpty())
        result.solutions.forEach { solution ->
            assertEquals(2.4, solution.averagePrice, 0.2 + 1e-6)
        }
    }

    @Test
    fun `已见大金作为固定项`() {
        val result = InferEngine().search(
            goldRedCount = 3,
            avgWan = 2.0,
            goldItems = items,
            seenCounts = mapOf("gold_a" to 1),
            cfg = CalcConfig(toleranceWan = 5.0)
        )
        assertTrue(result.solutions.isNotEmpty())
        result.solutions.forEach { solution ->
            assertTrue(
                solution.composition.any { it.itemId == "gold_a" }
            )
        }
    }

    @Test
    fun `金红数不足时返回空结果`() {
        val result = InferEngine().search(
            goldRedCount = 0,
            avgWan = 2.0,
            goldItems = items
        )
        assertTrue(result.solutions.isEmpty())
        assertFalse(result.zeroFound)
    }
}
