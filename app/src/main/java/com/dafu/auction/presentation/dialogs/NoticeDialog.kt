package com.dafu.auction.presentation.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NoticeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("使用说明") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoticeItem("1", "填入紫金红总数与紫件数，自动得到金红数。")
                NoticeItem("2", "勾选已经看清的大红 / 紫件，加成会自动累加。")
                NoticeItem("3", "点击「估价」，查看合计与七档建议出价。")
                NoticeItem("4", "填入金均读数后点击「反推金均」，列出均价匹配的金品组合。")
                NoticeItem("5", "「所见大金」里勾选确认过的大金，反推会把它作为固定项。")
                NoticeItem("6", "所有数据只保存在本机，可随时在设置里调整参数。")
                Text(
                    text = "提示：反推结果仅供参考，实际成交价仍受竞价影响。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun NoticeItem(index: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = "$index. $text",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
