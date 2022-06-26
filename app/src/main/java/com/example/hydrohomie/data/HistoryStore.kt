package com.example.hydrohomie.data

import com.example.hydrohomie.domain.DrinkEntry
import com.example.hydrohomie.domain.DrinkHistory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stores the glasses that were drunk.
 *
 * The new format keeps every single glass. The old format only knew the sum of the day;
 * it is migrated automatically so that no data is lost when the app is updated.
 */
class HistoryStore(file: File) {

    private val jsonFile = JsonFile(file)

    /** Reads the history, returns an empty history if there is none. */
    fun load(): DrinkHistory {
        val json = jsonFile.read() ?: return DrinkHistory.EMPTY
        return fromJson(json)
    }

    /** Writes the history. */
    fun save(history: DrinkHistory) {
        jsonFile.write(toJson(history))
    }

    private fun fromJson(json: JSONObject): DrinkHistory {
        val entries = json.optJSONArray(KEY_ENTRIES)
        if (entries != null) {
            val parsed = mutableListOf<DrinkEntry>()
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONObject(index) ?: continue
                val amount = entry.optInt(KEY_AMOUNT, 0)
                if (amount <= 0) continue
                parsed += DrinkEntry(
                    timestampMillis = entry.optLong(KEY_TIMESTAMP, 0L),
                    amountMl = amount,
                )
            }
            return DrinkHistory(parsed)
        }

        // Migration of the old format: only the sum of the day was stored.
        val legacySum = json.optInt(KEY_LEGACY_SUM, 0)
        val legacyTimestamp = json.optLong(KEY_LEGACY_TIMESTAMP, 0L)
        return if (legacySum > 0) {
            DrinkHistory(listOf(DrinkEntry(legacyTimestamp, legacySum)))
        } else {
            DrinkHistory.EMPTY
        }
    }

    private fun toJson(history: DrinkHistory): JSONObject = JSONObject().apply {
        put(KEY_ENTRIES, JSONArray().apply {
            history.entries.forEach { entry ->
                put(JSONObject().apply {
                    put(KEY_TIMESTAMP, entry.timestampMillis)
                    put(KEY_AMOUNT, entry.amountMl)
                })
            }
        })
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val KEY_TIMESTAMP = "t"
        const val KEY_AMOUNT = "a"

        // The keys of the previous version of the app.
        const val KEY_LEGACY_SUM = "currentSum"
        const val KEY_LEGACY_TIMESTAMP = "lastEditedTimeStamp"
    }
}
