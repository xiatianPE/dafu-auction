package com.dafu.auction.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dafu.auction.domain.model.AuctionState
import com.dafu.auction.domain.model.BidTier
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.InferResult
import com.dafu.auction.domain.model.ResultTab
import com.dafu.auction.domain.model.SortMode
import com.dafu.auction.presentation.components.SectionDivider
import com.dafu.auction.presentation.components.SelectionRow
import com.dafu.auction.presentation.components.StatLine
import com.dafu.auction.presentation.theme.SuccessGreen
import com.dafu.auction.presentation.theme.TierAggressive
import com.dafu.auction.presentation.theme.TierConservative
import com.dafu.auction.presentation.theme.TierEstimate
import com.dafu.auction.presentation.theme.TierHighBid
import com.dafu.auction.presentation.theme.TierLimit
import com.dafu.auction.presentation.theme.TierSprint
import com.dafu.auction.presentation.theme.TierSteady
import com.dafu.auction.presentation.theme.WarnOrange
import com.dafu.auction.presentation.viewmodel.AuctionViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    state: AuctionState,
    viewModel: AuctionViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { viewModel.setSheetExpanded(false) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            SheetHeader(state)
            TabRow(selectedTabIndex = state.activeTab.ordinal) {
                ResultTab.values().forEach { tab ->
                    Tab(
                        selected = state.activeTab == tab,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                when (state.activeTab) {
                    ResultTab.PRICE_DETAIL -> PriceDetailTab(state)
                    ResultTab.GOLD_SEEN -> GoldSeenTab(state, viewModel)
                    ResultTab.SUGGESTED_BID -> SuggestedBidTab(state)
                    ResultTab.INFER_TABLE -> InferTab(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(state: AuctionState) {
    val estimation = state.estimation
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "估价",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = estimation?.let { CatalogItem.formatWan(it.estimateValue) } ?: "—",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = " 万",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "金红数 ${state.goldRedCount ?: "—"} · 倍率 ×${state.multiplier}" +
                " · 已见大金 ${state.seenGoldCount} 件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// 价格明细
// ---------------------------------------------------------------------------

@Composable
private fun PriceDetailTab(state: AuctionState) {
    val estimation = state.estimation
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            StatLine("紫金红总数", state.totalInput.ifBlank { "—" } + " 件")
            StatLine("紫件数", state.purpleInput.ifBlank { "0" } + " 件")
            StatLine("金红数", (state.goldRedCount?.toString() ?: "—") + " 件", bold = true)
            StatLine("未知红件数", "${state.unknownRedCount} 件")
        }

        item { SectionDivider() }

        item {
            StatLine(
                label = "金红数 × 倍率",
                value = if (estimation != null) {
                    "${estimation.goldRedCount} × ${estimation.multiplier} = " +
                        "${CatalogItem.formatWan(estimation.baseValue)} 万"
                } else "—"
            )
            StatLine(
                label = "已选加成合计",
                value = "+" + CatalogItem.formatWan(estimation?.bonusTotal ?: 0.0) + " 万"
            )
        }

        if (state.selectedItems.isNotEmpty()) {
            item { SectionDivider() }
            val names = state.redCatalog.filter { state.countOf(it.id) > 0 }
            items(names, key = { it.id }) { item ->
                val count = state.countOf(item.id)
                StatLine(
                    label = "${item.name} ×$count",
                    value = "+" + CatalogItem.formatWan(item.bonus * count) + " 万"
                )
            }
        }

        item { SectionDivider() }

        item {
            StatLine(
                label = "估价合计",
                value = CatalogItem.formatWan(estimation?.estimateValue ?: 0.0) + " 万",
                bold = true,
                valueColor = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Text(
                text = "说明：未知红件按 0 加成计入金红数；大金不参与普通估价，只在反推中使用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 所见大金
// ---------------------------------------------------------------------------

@Composable
private fun GoldSeenTab(state: AuctionState, viewModel: AuctionViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "把已经确认看到的大金勾选进来（每种最多 3 件），反推时它们会被固定，搜索结果更准确。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "已见 ${state.seenGoldCount} 件",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (state.seenGolds.isNotEmpty()) {
                    androidx.compose.material3.TextButton(onClick = viewModel::clearSeenGolds) {
                        Text("清空")
                    }
                }
            }
        }
        items(state.goldCatalog, key = { it.id }) { item ->
            val count = state.seenCountOf(item.id)
            SelectionRow(
                title = item.name,
                valueText = item.displayText() + " · " + CatalogItem.formatWan(item.priceWan()) + "万",
                count = count,
                onDecrease = { viewModel.changeSeenGoldCount(item.id, -1) },
                onIncrease = { viewModel.changeSeenGoldCount(item.id, 1) },
                modifier = Modifier.fillMaxWidth(),
                maxReached = count >= 3,
                onDelete = if (item.builtIn) null else {
                    { viewModel.removeCustomItem(item.id) }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 建议出价
// ---------------------------------------------------------------------------

@Composable
private fun SuggestedBidTab(state: AuctionState) {
    val estimation = state.estimation ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "按估价 ${CatalogItem.formatWan(estimation.estimateValue)} 万，" +
                    "浮动 ±${estimation.fluctuationRange} 万生成的各档建议出价：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(estimation.bidTiers, key = { it.tier.name }) { suggestion ->
            val color = tierColor(suggestion.tier)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.10f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = suggestion.tier.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when {
                                suggestion.tier == BidTier.ESTIMATE -> "按估价"
                                suggestion.tier.tierIndex < BidTier.ESTIMATE.tierIndex ->
                                    "低于估价 ${CatalogItem.formatWan(estimation.estimateValue - suggestion.value)} 万"
                                else -> "高于估价 ${CatalogItem.formatWan(suggestion.value - estimation.estimateValue)} 万"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CatalogItem.formatWan(suggestion.value) + " 万",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
        }
    }
}

private fun tierColor(tier: BidTier): Color = when (tier) {
    BidTier.CONSERVATIVE -> TierConservative
    BidTier.STEADY -> TierSteady
    BidTier.ESTIMATE -> TierEstimate
    BidTier.AGGRESSIVE -> TierAggressive
    BidTier.HIGH_BID -> TierHighBid
    BidTier.SPRINT -> TierSprint
    BidTier.LIMIT -> TierLimit
}

// ---------------------------------------------------------------------------
// 反推表
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InferTab(state: AuctionState, viewModel: AuctionViewModel) {
    if (state.isCalculating) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "正在搜索金品组合…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = state.inferMessage.ifBlank {
                    "填写金均读数后点击「反推金均」，会列出均价符合读数的组合。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.inferResults.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SortMode.values().toList(), key = { it.name }) { mode ->
                        FilterChip(
                            selected = state.sortMode == mode,
                            onClick = { viewModel.setSortMode(mode) },
                            label = {
                                Text(mode.label, style = MaterialTheme.typography.bodySmall)
                            }
                        )
                    }
                }
            }
            items(state.inferResults, key = { it.index }) { result ->
                InferResultRow(result)
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "暂无结果",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "可以试试：放宽误差范围、提高最低金件数、开启狂暴模式，或减少已见大金限制。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InferResultRow(result: InferResult) {
    val exact = result.absoluteError < 1e-4
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (exact) {
                SuccessGreen.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "#${result.index}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${result.itemCount} 件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "均价 ${CatalogItem.formatWan(result.averagePrice)} 万",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (exact) "零误差" else formatError(result.error),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (exact) SuccessGreen else WarnOrange
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = result.compositionText(),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "总价 " + CatalogItem.formatWan(result.totalWan()) + " 万",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatError(error: Double): String {
    val sign = if (error > 0) "+" else if (error < 0) "-" else ""
    return "误差 $sign${CatalogItem.formatWan(abs(error))} 万"
}
