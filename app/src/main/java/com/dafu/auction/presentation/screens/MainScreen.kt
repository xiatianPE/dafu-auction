package com.dafu.auction.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dafu.auction.domain.model.AuctionState
import com.dafu.auction.domain.model.BidTier
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.CatalogSeed
import com.dafu.auction.domain.model.ResultTab
import com.dafu.auction.presentation.components.NumberField
import com.dafu.auction.presentation.components.SectionCard
import com.dafu.auction.presentation.components.SelectionRow
import com.dafu.auction.presentation.components.StepperSetting
import com.dafu.auction.presentation.dialogs.ManageItemsDialog
import com.dafu.auction.presentation.dialogs.NoticeDialog
import com.dafu.auction.presentation.dialogs.SettingsDialog
import com.dafu.auction.presentation.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: AuctionState,
    viewModel: AuctionViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showManageItems by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "达芙拍卖估价器",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "紫金红估价 · 大金反推",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showNotice() }) {
                        Icon(Icons.Default.Info, contentDescription = "公告")
                    }
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InputSection(state, viewModel) }

            if (state.estimation != null) {
                item {
                    EstimationPreview(
                        state = state,
                        onOpenDetail = {
                            viewModel.setActiveTab(ResultTab.PRICE_DETAIL)
                            viewModel.setSheetExpanded(true)
                        }
                    )
                }
            }

            item { MultiplierSection(state, viewModel) }

            item {
                RedItemsSection(
                    state = state,
                    viewModel = viewModel,
                    onManageItems = { showManageItems = true }
                )
            }

            item { ActionSection(state, viewModel) }
        }
    }

    if (state.sheetExpanded) {
        ResultSheet(state = state, viewModel = viewModel)
    }

    if (state.showNotice) {
        NoticeDialog(onDismiss = viewModel::dismissNotice)
    }

    if (state.showSettings) {
        SettingsDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = viewModel::closeSettings
        )
    }

    if (showManageItems) {
        ManageItemsDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showManageItems = false }
        )
    }
}

// ---------------------------------------------------------------------------
// 输入区
// ---------------------------------------------------------------------------

@Composable
private fun InputSection(state: AuctionState, viewModel: AuctionViewModel) {
    SectionCard(
        title = "估价输入",
        subtitle = "紫金红总数 - 紫件数 = 金红数"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                value = state.totalInput,
                onValueChange = viewModel::setTotalInput,
                label = "紫金红总数",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = state.purpleInput,
                onValueChange = viewModel::setPurpleInput,
                label = "紫件数",
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val count = state.goldRedCount
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = if (count != null) "金红数 $count 件" else "金红数 —",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            if (state.purpleCount != null && state.totalCount != null &&
                (state.purpleCount ?: 0) > (state.totalCount ?: 0)
            ) {
                Text(
                    text = "紫件数不能大于总数",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        NumberField(
            value = state.goldAverageInput,
            onValueChange = viewModel::setGoldAverageInput,
            label = "金均读数",
            suffix = "万",
            decimal = true
        )

        Text(
            text = "金均读数用于「反推金品组合」，只做估价时可不填。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// 估价预览
// ---------------------------------------------------------------------------

@Composable
private fun EstimationPreview(state: AuctionState, onOpenDetail: () -> Unit) {
    val estimation = state.estimation ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "估价合计",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onOpenDetail) {
                    Text("查看明细", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = CatalogItem.formatWan(estimation.estimateValue),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = " 万",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "金红数 ${estimation.goldRedCount} × ${estimation.multiplier} = " +
                    "${CatalogItem.formatWan(estimation.baseValue)} 万",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "已选加成 +${CatalogItem.formatWan(estimation.bonusTotal)} 万" +
                    if (state.unknownRedCount > 0) " · 未知红 ${state.unknownRedCount} 件" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "建议出价 " +
                    CatalogItem.formatWan(estimation.bidOf(BidTier.CONSERVATIVE)) + " ~ " +
                    CatalogItem.formatWan(estimation.bidOf(BidTier.LIMIT)) + " 万",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 倍率 / 浮动
// ---------------------------------------------------------------------------

@Composable
private fun MultiplierSection(state: AuctionState, viewModel: AuctionViewModel) {
    SectionCard(
        title = "倍率与浮动",
        subtitle = "倍率参与金红数的基础估价"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogSeed.multipliers.forEach { multiplier ->
                FilterChip(
                    selected = state.multiplier == multiplier,
                    onClick = { viewModel.setMultiplier(multiplier) },
                    label = { Text("×$multiplier") }
                )
            }
        }
        StepperSetting(
            title = "浮动范围",
            valueText = "±${state.fluctuationRange}万",
            subtitle = "影响建议出价的高低档位",
            onDecrease = { viewModel.setFluctuationRange(state.fluctuationRange - 1) },
            onIncrease = { viewModel.setFluctuationRange(state.fluctuationRange + 1) }
        )
    }
}

// ---------------------------------------------------------------------------
// 大红 / 紫件选择
// ---------------------------------------------------------------------------

@Composable
private fun RedItemsSection(
    state: AuctionState,
    viewModel: AuctionViewModel,
    onManageItems: () -> Unit
) {
    SectionCard(
        title = "大红 / 紫件加成",
        subtitle = "已选 ${state.selectedRedCount} 件 · 加成 " +
            "${CatalogItem.formatWan(state.selectedBonusTotal)} 万",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onManageItems) {
                    Icon(Icons.Default.Add, contentDescription = "管理藏品")
                }
                TextButton(
                    onClick = viewModel::clearSelectedItems,
                    enabled = state.selectedItems.isNotEmpty()
                ) {
                    Text("清空")
                }
            }
        }
    ) {
        NumberField(
            value = state.unknownRedInput,
            onValueChange = viewModel::setUnknownRedInput,
            label = "未知红件数（不计加成）"
        )

        if (state.redCatalog.isEmpty()) {
            Text(
                text = "暂无藏品数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.redCatalog.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { item ->
                            val count = state.countOf(item.id)
                            SelectionRow(
                                title = item.name,
                                valueText = item.displayText(),
                                count = count,
                                onDecrease = { viewModel.changeItemCount(item.id, -1) },
                                onIncrease = { viewModel.changeItemCount(item.id, 1) },
                                modifier = Modifier.weight(1f),
                                maxReached = count >= 99,
                                onDelete = if (item.builtIn) {
                                    null
                                } else {
                                    { viewModel.removeCustomItem(item.id) }
                                }
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 操作按钮
// ---------------------------------------------------------------------------

@Composable
private fun ActionSection(state: AuctionState, viewModel: AuctionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = viewModel::estimate,
                enabled = state.canEstimate,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("估价")
            }
            FilledTonalButton(
                onClick = viewModel::runInference,
                enabled = state.canInfer && !state.isCalculating,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(if (state.isCalculating) "反推中…" else "反推金均")
            }
        }
        Text(
            text = "估价 = 金红数 × 倍率 + 已选加成；反推会列出均价符合读数的金品组合。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
