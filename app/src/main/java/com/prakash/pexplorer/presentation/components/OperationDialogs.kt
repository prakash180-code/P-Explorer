package com.prakash.pexplorer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prakash.pexplorer.R
import com.prakash.pexplorer.core.util.formatBytes
import com.prakash.pexplorer.core.util.formatModifiedDate
import com.prakash.pexplorer.domain.model.FileKind
import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.FileProperties
import com.prakash.pexplorer.domain.usecase.FileNameError
import com.prakash.pexplorer.domain.usecase.FileNameValidator
import com.prakash.pexplorer.presentation.PropertiesUiState
import com.prakash.pexplorer.presentation.TransferUiState

@Composable
fun NameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    val validationError = remember(name) { FileNameValidator.validate(name) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = { Text(stringResource(R.string.name)) },
                isError = validationError != null && name.isNotEmpty(),
                supportingText = if (validationError != null && name.isNotEmpty()) {
                    { Text(stringResource(validationMessage(validationError))) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (validationError == null) onConfirm(name.trim())
                    }
                )
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = validationError == null
            ) {
                Text(confirmLabel)
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = { Text(stringResource(R.string.delete_selected_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.delete_selected_message
                ) + " " + pluralStringResource(
                    R.plurals.selected_count,
                    selectedCount,
                    selectedCount
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        }
    )
}

@Composable
fun TransferDestinationDialog(
    operation: FileOperation,
    selectedCount: Int,
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var destination by remember(currentPath) { mutableStateOf(currentPath) }
    val isCopy = operation == FileOperation.COPY
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isCopy) Icons.Filled.ContentCopy else Icons.AutoMirrored.Filled.DriveFileMove,
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(if (isCopy) R.string.copy_selected_title else R.string.move_selected_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.selected_count,
                        selectedCount,
                        selectedCount
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.destination_folder)) },
                    placeholder = { Text(stringResource(R.string.destination_path_hint)) },
                    supportingText = { Text(stringResource(R.string.destination_path_supporting)) }
                )
                OutlinedButton(
                    onClick = { destination = currentPath },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.use_current_folder))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(destination.trim()) },
                enabled = destination.isNotBlank()
            ) {
                Text(stringResource(if (isCopy) R.string.copy else R.string.move))
            }
        }
    )
}

@Composable
fun TransferProgressDialog(
    state: TransferUiState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val operationLabel = when (state.operation) {
        FileOperation.COPY -> R.string.copying
        FileOperation.MOVE -> R.string.moving
        FileOperation.DELETE -> R.string.deleting
    }
    AlertDialog(
        onDismissRequest = { if (!state.isRunning) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        icon = {
            Icon(
                imageVector = when (state.operation) {
                    FileOperation.COPY -> Icons.Filled.ContentCopy
                    FileOperation.MOVE -> Icons.AutoMirrored.Filled.DriveFileMove
                    FileOperation.DELETE -> Icons.Filled.Delete
                },
                contentDescription = null
            )
        },
        title = { Text(stringResource(operationLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { state.fraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.operation_progress,
                            state.completedItems,
                            state.completedItems,
                            state.totalItems
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                    state.currentName?.let { currentName ->
                        Text(
                            text = currentName,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        dismissButton = if (state.isRunning) {
            {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        } else {
            null
        },
        confirmButton = if (!state.isRunning) {
            {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.confirm))
                }
            }
        } else {
            {}
        }
    )
}

@Composable
fun PropertiesDialog(
    state: PropertiesUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
        title = {
            Text(
                text = state.properties?.name?.let {
                    stringResource(R.string.properties_for, it)
                } ?: stringResource(R.string.properties)
            )
        },
        text = {
            when {
                state.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.calculating))
                    }
                }
                state.errorMessage != null -> Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
                state.properties != null -> PropertyList(state.properties)
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
private fun PropertyList(properties: FileProperties) {
    Column(
        modifier = Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        PropertyRow(stringResource(R.string.name), properties.name)
        PropertyRow(stringResource(R.string.path), properties.path)
        PropertyRow(stringResource(R.string.type), typeLabel(properties.kind))
        PropertyRow(stringResource(R.string.size), formatBytes(properties.sizeBytes))
        properties.itemCount?.let {
            PropertyRow(
                stringResource(R.string.items),
                pluralStringResource(R.plurals.folder_items_count, it, it)
            )
        }
        PropertyRow(stringResource(R.string.created), formatDate(properties.createdEpochMillis))
        PropertyRow(stringResource(R.string.modified), formatDate(properties.modifiedEpochMillis))
        PropertyRow(stringResource(R.string.accessed), formatDate(properties.accessedEpochMillis))
        PropertyRow(
            stringResource(R.string.mime_type),
            properties.mimeType ?: stringResource(R.string.not_available)
        )
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 7.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun formatDate(epochMillis: Long?): String =
    epochMillis?.let(::formatModifiedDate)?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.not_available)

@Composable
private fun typeLabel(kind: FileKind): String = stringResource(
    when (kind) {
        FileKind.FOLDER -> R.string.type_folder
        FileKind.IMAGE -> R.string.type_image
        FileKind.VIDEO -> R.string.type_video
        FileKind.AUDIO -> R.string.type_audio
        FileKind.PDF -> R.string.type_pdf
        FileKind.DOCUMENT -> R.string.type_document
        FileKind.SPREADSHEET -> R.string.type_spreadsheet
        FileKind.PRESENTATION -> R.string.type_presentation
        FileKind.TEXT -> R.string.type_text
        FileKind.ARCHIVE -> R.string.type_archive
        FileKind.APK -> R.string.type_apk
        FileKind.UNKNOWN -> R.string.type_file
    }
)

private fun validationMessage(error: FileNameError): Int = when (error) {
    FileNameError.EMPTY -> R.string.invalid_name_empty
    FileNameError.DOT_NAME -> R.string.invalid_name_dot
    FileNameError.INVALID_CHARACTER -> R.string.invalid_name_characters
}
