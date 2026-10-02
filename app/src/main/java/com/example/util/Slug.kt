package com.example.util

object Slug {
    /**
     * Reimplementation of slug generation from process.ts:11
     * Single-char fold: ë -> e, ç -> c
     */
    fun of(term: String): String {
        return term.lowercase()
            .replace('ë', 'e')
            .replace('ç', 'c')
            .split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-zA-Z]"), "") }
            .filter { it.isNotEmpty() }
            .joinToString("-")
    }

    /**
     * Normalize diacritics: ë -> e, ç -> c, lowercase
     */
    fun foldDiacritic(s: String): String {
        return s.lowercase()
            .replace('ë', 'e')
            .replace('ç', 'c')
    }

    /**
     * Cross-reference match from crossref.ts:13
     * Exact hit, else delete-one-char retry for inflections
     */
    fun matchSlug(slug: String, slugs: Set<String>): String? {
        if (slug in slugs) return slug
        if (slug.length < 6) return null
        for (i in slug.indices) {
            val v = slug.removeRange(i, i + 1)
            if (v in slugs) return v
        }
        return null
    }
}
