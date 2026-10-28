package com.dafu.auction.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dafu.auction.data.local.PreferencesHelper
import com.dafu.auction.domain.model.AuctionState
import com.dafu.auction.domain.model.InferResult
import com.dafu.auction.domain.model.ResultTab
import com.dafu.auction.domain.model.SortMode
import com.dafu.auction.domain.model.ThemeType
import com.dafu.auction.domain.repository.CatalogRepository
import com.dafu.auction.domain.usecase.CalcConfig
import com.dafu.auction.domain.usecase.EvaluateUseCase
import com.dafu.auction.domain.usecase.InferEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主界面 ViewModel：输入、估价、反推、设置
 */
class AuctionViewModel(
    private val catalogRepository: CatalogRepository,
    private val preferences: PreferencesHelper,
    private val evaluateUseCase: EvaluateUseCase,
    private val inferEngine: InferEngine
) : ViewModel() {

    private val _state = MutableStateFlow(AuctionState())
    val state: StateFlow<AuctionState> = _state.asStateFlow()

    private var inferJob: Job? = null

    init {
        loadSettings()
        observeCatalog()
    }

    // ------------------------------------------------------------------
    // 初始化
    // ------------------------------------------------------------------

    private fun loadSettings() {
        viewModelScope.launch {
            val saved = preferences.load()
            _state.update {
                it.copy(
                    theme = ThemeType.fromKey(saved.theme),
                    multiplier = saved.multiplier,
                    fluctuationRange = saved.fluctuationRange,
                    inferTolerance = saved.inferTolerance,
                    inferSeconds = saved.inferSeconds,
                    minGoldCount = saved.minGoldCount,
                    zeroLimit = saved.zeroLimit,
                    errorLimit = saved.errorLimit,
                    berserkMode = saved.berserkMode,
                    sortMode = SortMode.fromKey(saved.sortMode),
                    showNotice = !saved.noticeShown
                )
            }
            refreshEstimation()
        }
    }

    private fun observeCatalog() {
        viewModelScope.launch {
            catalogRepository.getRedItems().collect { items ->
                _state.update { it.copy(redCatalog = items) }
                refreshEstimation()
            }
        }
        viewModelScope.launch {
            catalogRepository.getGoldItems().collect { items ->
                _state.update { it.copy(goldCatalog = items) }
            }
        }
    }

    // ------------------------------------------------------------------
    // 输入
    // ------------------------------------------------------------------

    fun setTotalInput(value: String) {
        _state.update { it.copy(totalInput = digitsOnly(value, 4)) }
        refreshEstimation()
    }

    fun setPurpleInput(value: String) {
        _state.update { it.copy(purpleInput = digitsOnly(value, 4)) }
        refreshEstimation()
    }

    fun setUnknownRedInput(value: String) {
        _state.update { it.copy(unknownRedInput = digitsOnly(value, 3)) }
        refreshEstimation()
    }

    /** 金均读数，允许两位小数 */
    fun setGoldAverageInput(value: String) {
        var dotUsed = false
        val filtered = buildString {
            for (ch in value) {
                when {
                    ch.isDigit() -> append(ch)
                    (ch == '.' || ch == '。') && !dotUsed -> {
                        dotUsed = true
                        append('.')
                    }
                }
            }
        }.take(8)
        val normalized = filtered.trimStart('.')
        _state.update { it.copy(goldAverageInput = normalized) }
    }

    fun setMultiplier(value: Int) {
        _state.update { it.copy(multiplier = value) }
        refreshEstimation()
        viewModelScope.launch { preferences.saveMultiplier(value) }
    }

    fun toggleInferMode() {
        _state.update { it.copy(isInferMode = !it.isInferMode) }
    }

    // ------------------------------------------------------------------
    // 已选藏品
    // ------------------------------------------------------------------

    fun changeItemCount(itemId: String, delta: Int) {
        _state.update { current ->
            val next = ((current.selectedItems[itemId] ?: 0) + delta).coerceIn(0, MAX_ITEM_COUNT)
            val map = current.selectedItems.toMutableMap()
            if (next == 0) map.remove(itemId) else map[itemId] = next
            current.copy(selectedItems = map)
        }
        refreshEstimation()
    }

    fun setItemCount(itemId: String, count: Int) {
        _state.update { current ->
            val next = count.coerceIn(0, MAX_ITEM_COUNT)
            val map = current.selectedItems.toMutableMap()
            if (next == 0) map.remove(itemId) else map[itemId] = next
            current.copy(selectedItems = map)
        }
        refreshEstimation()
    }

    fun changeSeenGoldCount(itemId: String, delta: Int) {
        _state.update { current ->
            val next = ((current.seenGolds[itemId] ?: 0) + delta).coerceIn(0, MAX_GOLD_PER_ITEM)
            val map = current.seenGolds.toMutableMap()
            if (next == 0) map.remove(itemId) else map[itemId] = next
            current.copy(seenGolds = map)
        }
    }

    fun clearSelectedItems() {
        _state.update { it.copy(selectedItems = emptyMap()) }
        refreshEstimation()
    }

    fun clearSeenGolds() {
        _state.update { it.copy(seenGolds = emptyMap()) }
    }

    // ------------------------------------------------------------------
    // 计算
    // ------------------------------------------------------------------

    private fun refreshEstimation() {
        val current = _state.value
        val count = current.goldRedCount
        val result = if (count == null) {
            null
        } else {
            evaluateUseCase.execute(
                goldRedCount = count,
                multiplier = current.multiplier,
                fluctuationRange = current.fluctuationRange,
                selectedItems = current.selectedItems,
                catalog = current.redCatalog
            )
        }
        _state.update { it.copy(estimation = result) }
    }

    /** 点击“估价” */
    fun estimate() {
        refreshEstimation()
        val current = _state.value
        if (!current.canEstimate) {
            showMessage("请先填写紫金红总数与紫件数")
            return
        }
        _state.update {
            it.copy(
                activeTab = ResultTab.PRICE_DETAIL,
                sheetExpanded = true,
                isInferMode = false
            )
        }
    }

    /** 点击“反推” */
    fun runInference() {
        val current = _state.value
        val goldRedCount = current.goldRedCount
        val avg = current.goldAverage

        if (goldRedCount == null) {
            showMessage("请先填写紫金红总数与紫件数")
            return
        }
        if (avg == null || avg <= 0.0) {
            showMessage("请先填写金均读数")
            return
        }
        if (current.goldCatalog.isEmpty()) {
            showMessage("暂无可选大金，请先添加大金价格")
            return
        }

        inferJob?.cancel()
        _state.update {
            it.copy(
                isCalculating = true,
                inferResults = emptyList(),
                inferMessage = "正在反推…",
                activeTab = ResultTab.INFER_TABLE,
                sheetExpanded = true,
                isInferMode = true
            )
        }

        val config = CalcConfig.fromSettings(
            minGoldCount = current.minGoldCount,
            zeroLimit = current.zeroLimit,
            errorLimit = current.errorLimit,
            berserkMode = current.berserkMode,
            toleranceWan = current.inferTolerance,
            seconds = current.inferSeconds
        )

        inferJob = viewModelScope.launch {
            val search = withContext(Dispatchers.Default) {
                inferEngine.search(
                    goldRedCount = goldRedCount,
                    avgWan = avg,
                    goldItems = current.goldCatalog,
                    seenCounts = current.seenGolds,
                    cfg = config
                )
            }
            _state.update {
                it.copy(
                    isCalculating = false,
                    inferResults = sortResults(search.solutions, it.sortMode),
                    inferMessage = search.message,
                    calculationElapsedMs = search.elapsedMs,
                    activeTab = ResultTab.INFER_TABLE
                )
            }
        }
    }

    fun setSortMode(mode: SortMode) {
        _state.update { it.copy(sortMode = mode, inferResults = sortResults(it.inferResults, mode)) }
        viewModelScope.launch { preferences.saveSortMode(mode.name) }
    }

    private fun sortResults(results: List<InferResult>, mode: SortMode): List<InferResult> {
        val sorted = when (mode) {
            SortMode.DIFF_ASC -> results.sortedWith(compareBy({ it.absoluteError }, { it.itemCount }))
            SortMode.DIFF_DESC -> results.sortedWith(
                compareByDescending<InferResult> { it.absoluteError }.thenBy { it.itemCount }
            )
            SortMode.PRICE_ASC -> results.sortedBy { it.totalPrice }
            SortMode.PRICE_DESC -> results.sortedByDescending { it.totalPrice }
            SortMode.COUNT_ASC -> results.sortedWith(compareBy({ it.itemCount }, { it.absoluteError }))
            SortMode.COUNT_DESC -> results.sortedWith(
                compareByDescending<InferResult> { it.itemCount }.thenBy { it.absoluteError }
            )
        }
        return sorted.mapIndexed { index, result -> result.copy(index = index + 1) }
    }

    // ------------------------------------------------------------------
    // 设置
    // ------------------------------------------------------------------

    fun setTheme(theme: ThemeType) {
        _state.update { it.copy(theme = theme) }
        viewModelScope.launch { preferences.saveTheme(theme.key) }
    }

    fun setFluctuationRange(value: Int) {
        val safe = value.coerceIn(0, 200)
        _state.update { it.copy(fluctuationRange = safe) }
        refreshEstimation()
        viewModelScope.launch { preferences.saveFluctuationRange(safe) }
    }

    fun setInferTolerance(value: Double) {
        val safe = value.coerceIn(0.0, 100.0)
        _state.update { it.copy(inferTolerance = safe) }
        viewModelScope.launch { preferences.saveInferTolerance(safe) }
    }

    fun setInferSeconds(value: Int) {
        val safe = value.coerceIn(1, 60)
        _state.update { it.copy(inferSeconds = safe) }
        viewModelScope.launch { preferences.saveInferSeconds(safe) }
    }

    fun setMinGoldCount(value: Int) {
        val safe = value.coerceIn(1, 30)
        _state.update { it.copy(minGoldCount = safe) }
        viewModelScope.launch { preferences.saveMinGoldCount(safe) }
    }

    fun setZeroLimit(value: Int) {
        val safe = value.coerceIn(0, 500)
        _state.update { it.copy(zeroLimit = safe) }
        viewModelScope.launch { preferences.saveZeroLimit(safe) }
    }

    fun setErrorLimit(value: Int) {
        val safe = value.coerceIn(0, 500)
        _state.update { it.copy(errorLimit = safe) }
        viewModelScope.launch { preferences.saveErrorLimit(safe) }
    }

    fun setBerserkMode(enabled: Boolean) {
        _state.update { it.copy(berserkMode = enabled) }
        viewModelScope.launch { preferences.saveBerserkMode(enabled) }
    }

    fun openSettings() = _state.update { it.copy(showSettings = true) }

    fun closeSettings() = _state.update { it.copy(showSettings = false) }

    fun dismissNotice() {
        _state.update { it.copy(showNotice = false) }
        viewModelScope.launch { preferences.saveNoticeShown(true) }
    }

    fun showNotice() = _state.update { it.copy(showNotice = true) }

    // ------------------------------------------------------------------
    // 藏品维护
    // ------------------------------------------------------------------

    fun addCustomRedItem(name: String, bonus: Double) {
        if (name.isBlank()) {
            showMessage("请输入藏品名称")
            return
        }
        viewModelScope.launch {
            catalogRepository.addCustomRedItem(name, bonus)
            showMessage("已添加：$name")
        }
    }

    fun addCustomGoldItem(name: String, priceYuan: Double) {
        if (name.isBlank()) {
            showMessage("请输入大金名称")
            return
        }
        if (priceYuan <= 0.0) {
            showMessage("请输入有效的价格")
            return
        }
        viewModelScope.launch {
            catalogRepository.addCustomGoldItem(name, priceYuan)
            showMessage("已添加：$name")
        }
    }

    fun removeCustomItem(itemId: String) {
        viewModelScope.launch {
            catalogRepository.removeCustomItem(itemId)
            changeItemCount(itemId, 0)
            changeSeenGoldCount(itemId, 0)
        }
    }

    // ------------------------------------------------------------------
    // UI 状态
    // ------------------------------------------------------------------

    fun setActiveTab(tab: ResultTab) = _state.update { it.copy(activeTab = tab) }

    fun setSheetExpanded(expanded: Boolean) = _state.update { it.copy(sheetExpanded = expanded) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun showMessage(text: String) = _state.update { it.copy(message = text) }

    override fun onCleared() {
        inferJob?.cancel()
        super.onCleared()
    }

    private fun digitsOnly(value: String, maxLen: Int): String =
        value.filter { it.isDigit() }.take(maxLen)

    private companion object {
        const val MAX_ITEM_COUNT = 99
        const val MAX_GOLD_PER_ITEM = 3
    }
}
