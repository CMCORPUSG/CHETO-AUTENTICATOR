package com.cmcorpusg.chetoauthenticator.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LimaClock {
    private val zone = ZoneId.of("America/Lima")
    private val formatter = DateTimeFormatter
        .ofPattern("HH:mm · dd MMM", Locale("es", "PE"))
        .withZone(zone)

    fun nowLabel(epochMillis: Long = System.currentTimeMillis()): String =
        formatter.format(Instant.ofEpochMilli(epochMillis))
}
