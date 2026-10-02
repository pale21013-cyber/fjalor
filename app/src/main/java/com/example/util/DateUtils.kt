package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

object DateUtils {

    /**
     * UTC date in YYYY-MM-DD format as required by the WOTD algorithm
     */
    fun todayUtc(): String {
        return LocalDate.now(ZoneOffset.UTC).toString()
    }

    /**
     * Exact 32-bit integer hash port of JavaScript's String.hashCode from word-of-the-day.ts
     */
    fun jsHash(s: String): Int {
        var h = 0
        for (c in s) {
            h = h * 31 + c.code
            h = h or 0 // 32-bit truncation
        }
        return if (h == Int.MIN_VALUE) Int.MAX_VALUE else kotlin.math.abs(h)
    }

    /**
     * Local formatted date in Albanian, e.g. "e premte, 2 tetor 2026"
     */
    fun formatAlbanianLocalDate(date: LocalDate = LocalDate.now(ZoneId.systemDefault())): String {
        val dayName = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "e hënë"
            DayOfWeek.TUESDAY -> "e martë"
            DayOfWeek.WEDNESDAY -> "e mërkurë"
            DayOfWeek.THURSDAY -> "e enjte"
            DayOfWeek.FRIDAY -> "e premte"
            DayOfWeek.SATURDAY -> "e shtunë"
            DayOfWeek.SUNDAY -> "e diel"
            else -> ""
        }

        val monthName = when (date.monthValue) {
            1 -> "janar"
            2 -> "shkurt"
            3 -> "mars"
            4 -> "prill"
            5 -> "maj"
            6 -> "qershor"
            7 -> "korrik"
            8 -> "gusht"
            9 -> "shtator"
            10 -> "tetor"
            11 -> "nëntor"
            12 -> "dhjetor"
            else -> ""
        }

        return "$dayName, ${date.dayOfMonth} $monthName ${date.year}"
    }
}
