package com.example.util

object Abbrev {
    private val abbrevMap: Map<String, String> = mapOf(
        "em." to "emër",
        "f." to "femërore",
        "m." to "mashkullore",
        "mb." to "mbiemër",
        "kal." to "kalimtar",
        "jokal." to "jokalimtar",
        "sh." to "shumës",
        "njëj." to "njëjës",
        "bised." to "bisedore",
        "fig." to "figurativisht",
        "hist." to "histori",
        "let." to "letërsi",
        "usht." to "ushtarak",
        "nd." to "ndajfolje",
        "fol." to "folje",
        "lidh." to "lidhëz",
        "përk." to "përkatëse",
        "vetv." to "vetvetore",
        "përem." to "përemër",
        "pasth." to "pasthirrmë",
        "parafj." to "parafjalë",
        "bot." to "botanikë",
        "zool." to "zoologji",
        "mjek." to "mjekësi",
        "gjeogr." to "gjeografi",
        "gjuh." to "gjuhësi",
        "fiz." to "fizikë",
        "kim." to "kimi",
        "mat." to "matematikë",
        "muz." to "muzikë",
        "teatr." to "teatër",
        "krahin." to "krahinore",
        "lib." to "librore",
        "vjet." to "e vjetëruar",
        "iron." to "ironike",
        "keq." to "keqësuese",
        "zvëgl." to "zvogëluese",
        "përbm." to "përbuzëse",
        "kryes." to "kryesisht",
        "kund." to "kundërshtuese"
    )

    fun expand(raw: String): String {
        val trimmed = raw.trim().lowercase()
        // Try exact match or with trailing dot
        val withDot = if (trimmed.endsWith(".")) trimmed else "$trimmed."
        return abbrevMap[withDot] ?: abbrevMap[trimmed] ?: raw
    }

    fun getCategoryOrMeaning(attr: String): String {
        val meaning = expand(attr)
        return if (meaning != attr) "$attr ($meaning)" else attr
    }
}
