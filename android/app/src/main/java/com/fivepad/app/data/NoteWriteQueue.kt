package com.fivepad.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Application-owned, ordered writes survive navigation and ViewModel disposal. */
class NoteWriteQueue(scope: CoroutineScope, private val onError: (Throwable) -> Unit) {
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (write in writes) {
                try {
                    write()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    onError(error)
                }
            }
        }
    }

    fun submit(write: suspend () -> Unit) {
        check(writes.trySend(write).isSuccess)
    }

    suspend fun awaitIdle() {
        val done = CompletableDeferred<Unit>()
        submit { done.complete(Unit) }
        done.await()
    }
}
