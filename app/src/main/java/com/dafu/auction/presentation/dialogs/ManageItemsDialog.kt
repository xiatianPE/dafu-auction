package com.dafu.auction.presentation.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dafu.auction.domain.model.AuctionState
import com.dafu.auction.domain.model.CatalogItem
import com.dafu.auction.presentation.components.NumberField
import com.dafu.auction.presentation.components.SectionDivider
import com.dafu.auction.presentation.viewmodel.AuctionViewModel

@Composable
fun ManageItemsDialog(
    state: AuctionState,
    viewModel: AuctionViewModel,
    onDismiss: () -> Unit
) {
    var redName by remember { mutableStateOf("") }
    var redBonus by remember { mutableStateOf("") }
    var goldName by remember { mutableStateOf("") }
    var goldPrice by remember { mutableStateOf("") }

    val customReds = remember(state.redCatalog) { state.redCatalog.filter { !it.builtIn } }
    val customGolds = remember(state.goldCatalog) { state.goldCatalog.filter { !it.builtIn } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("藏品管理") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("添加自定义大红", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = redName,
                    onValueChange = { redName = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumberField(
                        value = redBonus,
                        onValueChange = { redBonus = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = "加成",
                        suffix = "万",
                        decimal = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val bonus = redBonus.toDoubleOrNull() ?: 0.0
                            viewModel.addCustomRedItem(redName, bonus)
                            redName = ""
                            redBonus = ""
                        },
                        enabled = redName.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }

                SectionDivider()

                Text("添加大金价格", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = goldName,
                    onValueChange = { goldName = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumberField(
                        value = goldPrice,
                        onValueChange = { goldPrice = it.filter { ch -> ch.isDigit() } },
                        label = "价格",
                        suffix = "元",
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val price = goldPrice.toDoubleOrNull() ?: 0.0
                            viewModel.addCustomGoldItem(goldName, price)
                            goldName = ""
                            goldPrice = ""
                        },
                        enabled = goldName.isNotBlank() && goldPrice.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }

                if (customReds.isNotEmpty() || customGolds.isNotEmpty()) {
                    SectionDivider()
                    Text("自定义藏品", style = MaterialTheme.typography.labelLarge)
                    customReds.forEach { item ->
                        CustomItemRow(
                            item = item,
                            valueText = CatalogItem.formatWan(item.bonus) + " 万",
                            onDelete = { viewModel.removeCustomItem(item.id) }
                        )
                    }
                    customGolds.forEach { item ->
                        CustomItemRow(
                            item = item,
                            valueText = CatalogItem.formatWan(item.price) + " 元",
                            onDelete = { viewModel.removeCustomItem(item.id) }
                        )
                    }
                }

                Text(
                    text = "自定义条目只保存在本机；内置条目不可删除。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun CustomItemRow(
    item: CatalogItem,
    valueText: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${item.category.label} · $valueText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
