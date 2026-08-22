package com.arindam.camerax.util.coroutines

import kotlinx.coroutines.flow.SharingStarted

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Keep upstream camera flows warm across short configuration changes, then stop in the background.
 */
val ShareWhileSubscribed: SharingStarted = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS)
