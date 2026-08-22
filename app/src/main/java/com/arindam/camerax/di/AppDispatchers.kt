package com.arindam.camerax.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * DI: injectable coroutine dispatchers. Tests pass [kotlinx.coroutines.test.TestDispatcher] instances.
 *
 * Use [io] for disk and MediaStore, [default] for CPU-bound work (bitmap decode, device probes),
 * and [main] for UI state updates only. Repositories and ViewModels must not hardcode
 * [Dispatchers.IO] — take these from [AppContainer].
 */
data class AppDispatchers(
    /** Main thread (immediate in tests). */
    val main: CoroutineDispatcher = Dispatchers.Main.immediate,
    /** CPU-bound work off the UI thread. */
    val default: CoroutineDispatcher = Dispatchers.Default,
    /** Blocking I/O (files, MediaStore, mux). */
    val io: CoroutineDispatcher = Dispatchers.IO
)
