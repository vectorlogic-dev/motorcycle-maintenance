package com.motocare.app.ui

import com.motocare.app.data.local.entity.MotorcycleEntity
import com.motocare.app.data.local.entity.OdometerEntryEntity
import com.motocare.app.domain.model.OdometerStats
import com.motocare.app.domain.usecase.OdometerCalculator
import java.time.LocalDate

internal fun OdometerCalculator.statsFor(
    motorcycle: MotorcycleEntity?,
    entries: List<OdometerEntryEntity>,
): OdometerStats = stats(
    entries = entries,
    initialReadingKm = motorcycle?.initialOdometerKm,
    initialDate = motorcycle?.purchaseDateEpochDay?.let(LocalDate::ofEpochDay),
)
