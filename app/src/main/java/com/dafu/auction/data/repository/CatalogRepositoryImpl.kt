package com.dafu.auction.data.repository

import com.dafu.auction.data.local.CustomItemDao
import com.dafu.auction.data.local.CustomItemEntity
import com.dafu.auction.data.local.GoldPriceEntryDao
import com.dafu.auction.data.local.GoldPriceEntryEntity
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.CatalogSeed
import com.dafu.auction.domain.model.ItemCategory
import com.dafu.auction.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CatalogRepositoryImpl(
    private val customItemDao: CustomItemDao,
    private val goldPriceEntryDao: GoldPriceEntryDao
) : CatalogRepository {

    override fun getRedItems(): Flow<List<CatalogItem>> = customItemDao.getAll().map { entities ->
        val custom = entities
            .filter { it.category == CustomItemEntity.CATEGORY_RED }
            .map { it.toCatalogItem() }
        (CatalogSeed.redItems() + custom)
            .sortedWith(compareByDescending<CatalogItem> { it.bonus }.thenBy { it.name })
    }

    override fun getGoldItems(): Flow<List<CatalogItem>> = goldPriceEntryDao.getAll().map { entities ->
        val custom = entities.map { it.toCatalogItem() }
        (CatalogSeed.goldItems() + custom)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<CatalogItem> { it.price }.thenBy { it.name })
    }

    override fun searchGoldItems(query: String): Flow<List<CatalogItem>> {
        val keyword = query.trim()
        return if (keyword.isEmpty()) {
            getGoldItems()
        } else {
            goldPriceEntryDao.search("%$keyword%").map { entities ->
                val custom = entities.map { it.toCatalogItem() }
                (CatalogSeed.goldItems() + custom)
                    .distinctBy { it.id }
                    .filter { it.name.contains(keyword, ignoreCase = true) || it.price.toLong().toString().contains(keyword) }
                    .sortedByDescending { it.price }
                    .take(MAX_SEARCH_RESULT)
            }
        }
    }

    override suspend fun addCustomRedItem(name: String, bonus: Double) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        customItemDao.insert(
            CustomItemEntity(
                id = "custom_red_" + UUID.randomUUID().toString(),
                name = trimmed,
                category = CustomItemEntity.CATEGORY_RED,
                bonusOrPrice = bonus.coerceAtLeast(0.0)
            )
        )
    }

    override suspend fun addCustomGoldItem(name: String, price: Double) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        goldPriceEntryDao.insert(
            GoldPriceEntryEntity(
                id = "custom_gold_" + UUID.randomUUID().toString(),
                name = trimmed,
                price = price.toLong().coerceAtLeast(0L)
            )
        )
    }

    override suspend fun removeCustomItem(itemId: String) {
        customItemDao.deleteById(itemId)
        goldPriceEntryDao.deleteById(itemId)
    }

    private fun CustomItemEntity.toCatalogItem(): CatalogItem = CatalogItem(
        id = id,
        name = name,
        category = ItemCategory.RED,
        bonus = bonusOrPrice,
        builtIn = false
    )

    private fun GoldPriceEntryEntity.toCatalogItem(): CatalogItem = CatalogItem(
        id = id,
        name = name,
        category = ItemCategory.GOLD,
        price = price.toDouble(),
        builtIn = false
    )

    private companion object {
        const val MAX_SEARCH_RESULT = 20
    }
}
