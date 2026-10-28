package com.dafu.auction.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 大金价格条目 DAO
 */
@Dao
interface GoldPriceEntryDao {

    @Query("SELECT * FROM gold_price_entries ORDER BY price DESC")
    fun getAll(): Flow<List<GoldPriceEntryEntity>>

    @Query("SELECT * FROM gold_price_entries ORDER BY price DESC")
    suspend fun getAllOnce(): List<GoldPriceEntryEntity>

    /** query 需自行带上 % 通配符 */
    @Query("SELECT * FROM gold_price_entries WHERE name LIKE :query OR CAST(price AS TEXT) LIKE :query ORDER BY price DESC LIMIT 20")
    fun search(query: String): Flow<List<GoldPriceEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GoldPriceEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GoldPriceEntryEntity>)

    @Query("DELETE FROM gold_price_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM gold_price_entries")
    suspend fun deleteAll()
}
