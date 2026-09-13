package com.fivepad.app.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NoteWriteQueueTest {
    @Test fun switchingSlotsDoesNotCancelEditsAndClearRunsAfterTheLatestEdit() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val failures = mutableListOf<Throwable>()
            val queue = NoteWriteQueue(scope, failures::add)
            val gate = CompletableDeferred<Unit>()
            val stored = mutableMapOf<Int, String>()
            var cleared = ""
            queue.submit { gate.await(); stored[1] = "first" }
            queue.submit { stored[2] = "second" }
            queue.submit { stored[1] = "latest keystroke" }
            queue.submit { cleared = stored[1].orEmpty(); stored[1] = "" }
            gate.complete(Unit)
            queue.awaitIdle()
            assertEquals("latest keystroke", cleared)
            assertEquals("second", stored[2])
            assertEquals("", stored[1])
            assertTrue(failures.isEmpty())
        } finally { scope.cancel() }
    }

    @Test fun aFailedWriteIsReportedWithoutDiscardingLaterWrites() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val failures = mutableListOf<Throwable>()
            val queue = NoteWriteQueue(scope, failures::add)
            var label = ""
            queue.submit { throw IllegalStateException("disk unavailable") }
            queue.submit { label = "saved label" }
            queue.awaitIdle()
            assertEquals(1, failures.size)
            assertEquals("saved label", label)
        } finally { scope.cancel() }
    }
}
