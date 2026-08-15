package com.prakash.pexplorer.data.transfer

import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.model.TransferTask
import com.prakash.pexplorer.domain.model.TransferTaskStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TransferQueueManager(
    private val scope: CoroutineScope
) {
    private val _tasks = MutableStateFlow<List<TransferTask>>(emptyList())
    val tasks: StateFlow<List<TransferTask>> = _tasks.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()

    fun enqueue(
        operation: FileOperation,
        sourcePaths: List<String>,
        destinationPath: String?,
        worker: suspend (onProgress: (TransferProgress) -> Unit) -> Result<Unit>
    ): String {
        val id = UUID.randomUUID().toString()
        val task = TransferTask(
            id = id,
            operation = operation,
            sourcePaths = sourcePaths,
            destinationPath = destinationPath
        )
        _tasks.update { it + task }
        jobs[id] = scope.launch {
            updateTask(id) { it.copy(status = TransferTaskStatus.RUNNING) }
            val result = worker { progress ->
                updateTask(id) {
                    it.copy(
                        completedItems = progress.completedItems,
                        totalItems = progress.totalItems,
                        currentName = progress.currentName
                    )
                }
            }
            result.fold(
                onSuccess = {
                    updateTask(id) { it.copy(status = TransferTaskStatus.COMPLETED) }
                },
                onFailure = { error ->
                    if (error is CancellationException) {
                        updateTask(id) { it.copy(status = TransferTaskStatus.CANCELLED) }
                    } else {
                        updateTask(id) {
                            it.copy(
                                status = TransferTaskStatus.FAILED,
                                errorMessage = error.message
                            )
                        }
                    }
                }
            )
            jobs.remove(id)
        }
        return id
    }

    fun cancel(id: String) {
        jobs[id]?.cancel()
        updateTask(id) { it.copy(status = TransferTaskStatus.CANCELLED) }
    }

    fun removeFinished() {
        _tasks.update { tasks ->
            tasks.filterNot {
                it.status == TransferTaskStatus.COMPLETED ||
                    it.status == TransferTaskStatus.CANCELLED
            }
        }
    }

    private fun updateTask(id: String, transform: (TransferTask) -> TransferTask) {
        _tasks.update { tasks -> tasks.map { task -> if (task.id == id) transform(task) else task } }
    }
}
