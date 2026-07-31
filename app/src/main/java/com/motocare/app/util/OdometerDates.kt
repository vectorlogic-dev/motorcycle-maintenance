package com.motocare.app.util

import com.motocare.app.data.local.entity.OdometerEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

fun OdometerEntryEntity.recordedDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    recordedEpochDay?.let(LocalDate::ofEpochDay)
        ?: Instant.ofEpochMilli(recordedAtEpochMillis).atZone(zoneId).toLocalDate()

fun LocalDate.asStoredDateMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
