package com.prakash.pexplorer.data.transfer

import com.prakash.pexplorer.domain.model.FileOperation
import com.prakash.pexplorer.domain.model.TransferProgress
import com.prakash.pexplorer.domain.model.TransferTaskStatus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferQueueManagerTest {
    @Test
    fun completedWorkerMovesTaskToCompleted() = runTest {
        val manager = TransferQueueManager(this)
        val id = manager.enqueue(FileOperation.COPY, listOf("source.txt"), "/target") { onProgress ->
            onProgress(TransferProgress(FileOperation.COPY, "source.txt", 1, 1))
            Result.success(Unit)
        }

        advanceUntilIdle()

        val task = manager.tasks.value.single { it.id == id }
        assertEquals(TransferTaskStatus.COMPLETED, task.status)
        assertEquals(1, task.completedItems)
    }

    @Test
    fun failedWorkerPreservesFailureStatus() = runTest {
        val manager = TransferQueueManager(this)
        val id = manager.enqueue(FileOperation.DELETE, listOf("missing.txt"), null) {
            Result.failure(IllegalStateException("missing"))
        }

        advanceUntilIdle()

        val task = manager.tasks.value.single { it.id == id }
        assertEquals(TransferTaskStatus.FAILED, task.status)
        assertEquals("missing", task.errorMessage)
    }
}
