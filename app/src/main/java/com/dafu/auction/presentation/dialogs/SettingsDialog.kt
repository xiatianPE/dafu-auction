package com.dafu.auction.presentation.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dafu.auction.domain.model.AuctionState
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.ThemeType
import com.dafu.auction.presentation.components.SectionDivider
import com.dafu.auction.presentation.components.StepperSetting
import com.dafu.auction.presentation.viewmodel.AuctionViewModel

import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dafu.auction.domain.model.AuctionState
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.domain.model.ThemeType
import com.dafu.auction.presentation.components.SectionDivider
import com.dafu.auction.presentation.components.StepperSetting
import com.dafu.auction.presentation.viewmodel.AuctionViewModel

@Composable
fun SettingsDialog(
    state: AuctionState,
    viewModel: AuctionViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("主题", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeType.values().forEach { theme ->
                        FilterChip(
                            selected = state.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                            label = {
                                Text(theme.label, style = MaterialTheme.typography.bodySmall)
                            }
                        )
                    }
                }

                SectionDivider()

                StepperSetting(
                    title = "反推误差范围",
                    valueText = "±${CatalogItem.formatWan(state.inferTolerance)}万",
                    subtitle = "越小越严格，0 表示只找零误差",
                    onDecrease = { viewModel.setInferTolerance(state.inferTolerance - 0.5) },
                    onIncrease = { viewModel.setInferTolerance(state.inferTolerance + 0.5) }
                )

                StepperSetting(
                    title = "反推限时",
                    valueText = "${state.inferSeconds} 秒",
                    subtitle = "超时后返回已找到的结果",
                    onDecrease = { viewModel.setInferSeconds(state.inferSeconds - 1) },
                    onIncrease = { viewModel.setInferSeconds(state.inferSeconds + 1) }
                )

                StepperSetting(
                    title = "最低金件数",
                    valueText = "${state.minGoldCount} 件",
                    subtitle = "组合里至少包含的大金数量",
                    onDecrease = { viewModel.setMinGoldCount(state.minGoldCount - 1) },
                    onIncrease = { viewModel.setMinGoldCount(state.minGoldCount + 1) }
                )

                SectionDivider()

                StepperSetting(
                    title = "零误差条目上限",
                    valueText = "${state.zeroLimit} 条",
                    onDecrease = { viewModel.setZeroLimit(state.zeroLimit - 10) },
                    onIncrease = { viewModel.setZeroLimit(state.zeroLimit + 10) }
                )

                StepperSetting(
                    title = "带误差条目上限",
                    valueText = "${state.errorLimit} 条",
                    onDecrease = { viewModel.setErrorLimit(state.errorLimit - 10) },
                    onIncrease = { viewModel.setErrorLimit(state.errorLimit + 10) }
                )

                SectionDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("狂暴模式", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "放宽搜索时间与节点上限，结果更多但更耗电",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = state.berserkMode,
                        onCheckedChange = viewModel::setBerserkMode
                    )
                }

                Text(
                    text = "参数会自动保存到本机。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}
