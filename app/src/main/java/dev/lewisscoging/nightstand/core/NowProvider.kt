package dev.lewisscoging.nightstand.core

import java.time.ZonedDateTime

fun interface NowProvider {
    fun now(): ZonedDateTime
}

/** Reads the default zone on every call, so timezone and DST changes are picked up. */
val systemNowProvider = NowProvider { ZonedDateTime.now() }
