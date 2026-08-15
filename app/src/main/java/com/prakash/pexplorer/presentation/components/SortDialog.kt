package com.prakash.pexplorer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R
import com.prakash.pexplorer.domain.model.SortOrder

@Composable
fun SortDialog(
    sortOrder: SortOrder,
    foldersFirst: Boolean,
    onSortOrderChanged: (SortOrder) -> Unit,
    onFoldersFirstChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_by)) },
        text = {
            Column {
                SortOrder.entries.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderChanged(order) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == order,
                            onClick = { onSortOrderChanged(order) }
                        )
                        Text(sortLabel(order))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.folders_first),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = foldersFirst,
                        onCheckedChange = onFoldersFirstChanged
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        }
    )
}

@Composable
private fun sortLabel(order: SortOrder): String = stringResource(
    when (order) {
        SortOrder.NAME_ASC -> R.string.name_a_z
        SortOrder.NAME_DESC -> R.string.name_z_a
        SortOrder.DATE_NEWEST -> R.string.date_newest
        SortOrder.DATE_OLDEST -> R.string.date_oldest
        SortOrder.SIZE_LARGEST -> R.string.size_largest
        SortOrder.SIZE_SMALLEST -> R.string.size_smallest
        SortOrder.TYPE -> R.string.file_type
    }
)
