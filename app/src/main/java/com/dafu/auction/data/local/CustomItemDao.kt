package com.dafu.auction.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 自定义藏品 DAO
 */
@Dao
interface CustomItemDao {
    
    @Query("SELECT * FROM custom_items ORDER BY name")
    fun getAll(): Flow<List<CustomItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CustomItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CustomItemEntity>)
    
    @Query("DELETE FROM custom_items WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM custom_items")
    suspend fun deleteAll()
}
