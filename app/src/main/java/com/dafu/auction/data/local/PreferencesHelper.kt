package com.dafu.auction.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.auctionDataStore: DataStore<Preferences> by preferencesDataStore(name = "auction_settings")

private object Keys {
    val THEME = stringPreferencesKey("theme")
    val MULTIPLIER = intPreferencesKey("multiplier")
    val FLUCTUATION = intPreferencesKey("fluctuation_range")
    val INFER_TOLERANCE = doublePreferencesKey("infer_tolerance")
    val INFER_SECONDS = intPreferencesKey("infer_seconds")
    val MIN_GOLD = intPreferencesKey("min_gold_count")
    val ZERO_LIMIT = intPreferencesKey("zero_limit")
    val ERROR_LIMIT = intPreferencesKey("error_limit")
    val BERSERK = booleanPreferencesKey("berserk_mode")
    val SORT_MODE = stringPreferencesKey("sort_mode")
    val NOTICE_SHOWN = booleanPreferencesKey("notice_shown")
}

/**
 * 设置持久化
 */
class PreferencesHelper(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.auctionDataStore

    /** 一次性读取全部设置 */
    suspend fun load(): StoredSettings {
        val prefs = dataStore.data.first()
        return StoredSettings(
            theme = prefs[Keys.THEME] ?: "auto",
            multiplier = prefs[Keys.MULTIPLIER] ?: 8,
            fluctuationRange = prefs[Keys.FLUCTUATION] ?: 10,
            inferTolerance = prefs[Keys.INFER_TOLERANCE] ?: 5.0,
            inferSeconds = prefs[Keys.INFER_SECONDS] ?: 5,
            minGoldCount = prefs[Keys.MIN_GOLD] ?: 1,
            zeroLimit = prefs[Keys.ZERO_LIMIT] ?: 50,
            errorLimit = prefs[Keys.ERROR_LIMIT] ?: 50,
            berserkMode = prefs[Keys.BERSERK] ?: false,
            sortMode = prefs[Keys.SORT_MODE] ?: "DIFF_ASC",
            noticeShown = prefs[Keys.NOTICE_SHOWN] ?: false
        )
    }

    suspend fun saveTheme(theme: String) = put { it[Keys.THEME] = theme }

    suspend fun saveMultiplier(value: Int) = put { it[Keys.MULTIPLIER] = value }

    suspend fun saveFluctuationRange(value: Int) = put { it[Keys.FLUCTUATION] = value }

    suspend fun saveInferTolerance(value: Double) = put { it[Keys.INFER_TOLERANCE] = value }

    suspend fun saveInferSeconds(value: Int) = put { it[Keys.INFER_SECONDS] = value }

    suspend fun saveMinGoldCount(value: Int) = put { it[Keys.MIN_GOLD] = value }

    suspend fun saveZeroLimit(value: Int) = put { it[Keys.ZERO_LIMIT] = value }

    suspend fun saveErrorLimit(value: Int) = put { it[Keys.ERROR_LIMIT] = value }

    suspend fun saveBerserkMode(enabled: Boolean) = put { it[Keys.BERSERK] = enabled }

    suspend fun saveSortMode(mode: String) = put { it[Keys.SORT_MODE] = mode }

    suspend fun saveNoticeShown(shown: Boolean) = put { it[Keys.NOTICE_SHOWN] = shown }

    private suspend fun put(block: (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }
}

/** 本地存储的设置快照 */
data class StoredSettings(
    val theme: String,
    val multiplier: Int,
    val fluctuationRange: Int,
    val inferTolerance: Double,
    val inferSeconds: Int,
    val minGoldCount: Int,
    val zeroLimit: Int,
    val errorLimit: Int,
    val berserkMode: Boolean,
    val sortMode: String,
    val noticeShown: Boolean
)
