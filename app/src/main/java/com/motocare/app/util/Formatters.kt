package com.motocare.app.util

import java.text.NumberFormat
import java.util.Currency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DisplayFormats {
    @Volatile var currencyCode: String = "PHP"
        private set
    @Volatile var datePattern: String = "dd/MM/yyyy"
        private set

    fun configure(currency: String, dateFormat: String) {
        currencyCode = currency.takeIf { runCatching { Currency.getInstance(it) }.isSuccess } ?: "PHP"
        datePattern = dateFormat.takeIf { runCatching { DateTimeFormatter.ofPattern(it) }.isSuccess } ?: "dd/MM/yyyy"
    }
}

fun Long.asPeso(): String = NumberFormat.getCurrencyInstance(
    if (DisplayFormats.currencyCode == "PHP") Locale.forLanguageTag("en-PH") else Locale.US,
).apply { currency = Currency.getInstance(DisplayFormats.currencyCode) }.format(this / 100.0)

fun LocalDate.asDisplayDate(): String = format(DateTimeFormatter.ofPattern(DisplayFormats.datePattern))
fun String.toCentavosOrNull(): Long? = replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong()
