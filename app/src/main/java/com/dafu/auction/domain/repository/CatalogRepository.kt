package com.dafu.auction.domain.repository

import com.dafu.auction.domain.model.CatalogItem
import kotlinx.coroutines.flow.Flow

/**
 * 藏品目录仓储 — 内置 + 自定义藏品
 */
interface CatalogRepository {

    /** 大红/紫件列表（内置 + 自定义） */
    fun getRedItems(): Flow<List<CatalogItem>>

    /** 大金列表（内置 + 自定义），按价格降序 */
    fun getGoldItems(): Flow<List<CatalogItem>>

    /** 按名称或价格搜索大金 */
    fun searchGoldItems(query: String): Flow<List<CatalogItem>>

    /** 新增自定义大红（加成单位：万） */
    suspend fun addCustomRedItem(name: String, bonus: Double)

    /** 新增大金（价格单位：元） */
    suspend fun addCustomGoldItem(name: String, price: Double)

    /** 删除自定义藏品 */
    suspend fun removeCustomItem(itemId: String)
}
