package com.dafu.auction.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自定义藏品条目实体
 */
@Entity(tableName = "custom_items")
data class CustomItemEntity(
    @PrimaryKey val id: String,
    val name: String,           // 藏品名称
    val category: Int,          // 0=大红, 1=大金
    val bonusOrPrice: Double,   // 加成值(万)或价格(完整数字)
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_RED = 0
        const val CATEGORY_GOLD = 1
    }
}
