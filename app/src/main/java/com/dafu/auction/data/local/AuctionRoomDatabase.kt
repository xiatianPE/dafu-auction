package com.dafu.auction.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 本地 Room 数据库
 */
@Database(
    entities = [
        CustomItemEntity::class,      // 自定义藏品
        GoldPriceEntryEntity::class   // 大金价格条目
    ],
    version = 2,
    exportSchema = false
)
abstract class AuctionRoomDatabase : RoomDatabase() {
    
    abstract fun customItemDao(): CustomItemDao
    abstract fun goldPriceEntryDao(): GoldPriceEntryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AuctionRoomDatabase? = null
        
        fun getInstance(context: Context): AuctionRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuctionRoomDatabase::class.java,
                    "auction_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
