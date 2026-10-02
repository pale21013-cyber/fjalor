package com.example.net

import org.json.JSONArray
import org.json.JSONObject

data class Entry(
    val slug: String,
    val term: String,
    val attributes: List<String> = emptyList(),
    val definitions: List<String>? = null
) {
    val displayDefinitions: List<String>
        get() = definitions?.filter { it.isNotBlank() } ?: emptyList()

    val firstDefinition: String?
        get() = displayDefinitions.firstOrNull()
}

sealed interface Result<out T> {
    data class Ok<T>(val data: T) : Result<T>
    data object Empty : Result<Nothing>
    data class Http(val code: Int) : Result<Nothing>
    data object Offline : Result<Nothing>
    data class Err(val msg: String) : Result<Nothing>
}

object JsonParsers {
    fun parseEntry(json: JSONObject): Entry {
        val slug = json.optString("slug", "")
        val term = json.optString("term", "")

        val attrs = mutableListOf<String>()
        val attrsArr = json.optJSONArray("attributes")
        if (attrsArr != null) {
            for (i in 0 until attrsArr.length()) {
                val item = attrsArr.optString(i)
                if (!item.isNullOrEmpty()) {
                    attrs.add(item)
                }
            }
        }

        val defs = if (json.has("definitions") && !json.isNull("definitions")) {
            val defsArr = json.optJSONArray("definitions")
            if (defsArr != null) {
                val list = mutableListOf<String>()
                for (i in 0 until defsArr.length()) {
                    val d = defsArr.optString(i)
                    if (!d.isNullOrEmpty()) {
                        list.add(d)
                    }
                }
                list
            } else null
        } else {
            null
        }

        return Entry(
            slug = slug,
            term = term,
            attributes = attrs,
            definitions = defs
        )
    }

    fun parseEntryList(jsonArrayStr: String): List<Entry> {
        val arr = JSONArray(jsonArrayStr)
        val list = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(parseEntry(obj))
        }
        return list
    }

    fun parseStringList(jsonArrayStr: String): List<String> {
        val arr = JSONArray(jsonArrayStr)
        val list = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }
}
