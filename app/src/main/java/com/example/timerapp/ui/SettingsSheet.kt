package com.example.timerapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.timerapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    currentOrder:  SortOrder,
    onOrderChange: (SortOrder) -> Unit,
    onDismiss:     () -> Unit
) {
    val settingsTitle = stringResource(R.string.settings_title)
    val customLabel   = stringResource(R.string.sort_custom)
    val ascLabel      = stringResource(R.string.sort_time_asc)
    val descLabel     = stringResource(R.string.sort_time_desc)

    val options = listOf(
        SortOrder.CUSTOM    to customLabel,
        SortOrder.TIME_ASC  to ascLabel,
        SortOrder.TIME_DESC to descLabel
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(settingsTitle, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            options.forEach { (order, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentOrder == order,
                            role     = Role.RadioButton,
                            onClick  = { onOrderChange(order) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentOrder == order,
                        onClick  = null
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
