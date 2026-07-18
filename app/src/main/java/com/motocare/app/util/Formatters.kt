package com.motocare.app.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"))
private val displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun Long.asPeso(): String = pesoFormat.format(this / 100.0)
fun LocalDate.asDisplayDate(): String = format(displayDate)
fun String.toCentavosOrNull(): Long? = replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong()
