package dev.lewisscoging.nightstand.core

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** How long an orientation reading must hold before the gate flips. */
const val ORIENTATION_STABLE_MS = 1000L

/**
 * Emits a value only once it has held, unchanged, for [ms] milliseconds. The trailing
 * [distinctUntilChanged] drops the repeat that a flicker away and back would otherwise emit.
 */
@OptIn(FlowPreview::class)
fun Flow<Boolean>.stableFor(ms: Long): Flow<Boolean> =
    distinctUntilChanged().debounce(ms).distinctUntilChanged()
