package com.dafu.auction.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 价格表条目实体
 */
@Entity(tableName = "gold_price_entries")
data class GoldPriceEntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Long // 金价（完整数字）
)
