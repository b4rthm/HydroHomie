package com.example.hydrohomie.notifications

import com.example.hydrohomie.data.JsonFile
import com.example.hydrohomie.domain.ReminderSlot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Remembers which reminders are currently scheduled, so that they can be cancelled again
 * when the plan changes.
 */
class ReminderStore(file: File) {

    private val jsonFile = JsonFile(file)

    /** The scheduled reminders. */
    fun load(): List<ReminderSlot> {
        val json = jsonFile.read() ?: return emptyList()
        val array = json.optJSONArray(KEY_SLOTS) ?: return emptyList()
        val slots = mutableListOf<ReminderSlot>()
        for (index in 0 until array.length()) {
            val slot = array.optJSONObject(index) ?: continue
            slots += ReminderSlot(
                timestampMillis = slot.optLong(KEY_TIMESTAMP, 0L),
                amountMl = slot.optInt(KEY_AMOUNT, 0),
                id = slot.optInt(KEY_ID, -1),
            )
        }
        return slots
    }

    /** The ids of the scheduled reminders. */
    fun loadIds(): List<Int> = load().map { it.id }

    /** Stores the scheduled reminders. */
    fun save(slots: List<ReminderSlot>) {
        jsonFile.write(
            JSONObject().apply {
                put(KEY_SLOTS, JSONArray().apply {
                    slots.forEach { slot ->
                        put(JSONObject().apply {
                            put(KEY_ID, slot.id)
                            put(KEY_TIMESTAMP, slot.timestampMillis)
                            put(KEY_AMOUNT, slot.amountMl)
                        })
                    }
                })
            },
        )
    }
}

private const val KEY_SLOTS = "slots"
private const val KEY_ID = "id"
private const val KEY_TIMESTAMP = "t"
private const val KEY_AMOUNT = "a"
