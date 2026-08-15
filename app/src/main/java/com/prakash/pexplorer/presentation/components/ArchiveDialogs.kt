package com.prakash.pexplorer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prakash.pexplorer.R

@Composable
fun ZipNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    NameDialog(
        title = stringResource(R.string.compress_selected_title),
        initialName = "archive.zip",
        confirmLabel = stringResource(R.string.compress),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun ExtractDialog(
    archiveName: String,
    onDismiss: () -> Unit,
    onExtractHere: () -> Unit,
    onExtractToNewFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Archive, contentDescription = null) },
        title = { Text(stringResource(R.string.extract_selected_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = archiveName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onExtractHere,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.extract_here))
                }
                OutlinedButton(
                    onClick = onExtractToNewFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.extract_new_folder))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
