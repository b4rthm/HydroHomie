package com.example.hydrohomie.data

import com.example.hydrohomie.domain.DrinkHistory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryStoreTest {

    private lateinit var file: File
    private lateinit var store: HistoryStore

    @Before
    fun setUp() {
        file = File.createTempFile("history", ".json")
        file.delete()
        store = HistoryStore(file)
    }

    @Test
    fun `without a file the history is empty`() {
        assertTrue(store.load().entries.isEmpty())
    }

    @Test
    fun `the glasses survive a round trip`() {
        val history = DrinkHistory.EMPTY
            .add(250, 1_000L)
            .add(500, 2_000L)

        store.save(history)

        val loaded = store.load()
        assertEquals(2, loaded.entries.size)
        assertEquals(750, loaded.totalMl)
        assertEquals(250, loaded.entries[0].amountMl)
        assertEquals(1_000L, loaded.entries[0].timestampMillis)
    }

    @Test
    fun `the history of the previous version is migrated`() {
        file.writeText(
            """
            {
              "currentSum": 750,
              "lastEditedTimeStamp": 1715587200000
            }
            """.trimIndent(),
        )

        val history = store.load()

        assertEquals(1, history.entries.size)
        assertEquals(750, history.totalMl)
        assertEquals(1715587200000L, history.entries[0].timestampMillis)
    }

    @Test
    fun `an empty sum of the previous version results in an empty history`() {
        file.writeText("""{"currentSum": 0, "lastEditedTimeStamp": 1715587200000}""")

        assertTrue(store.load().entries.isEmpty())
    }

    @Test
    fun `a broken file results in an empty history`() {
        file.writeText("{not json")

        assertTrue(store.load().entries.isEmpty())
    }

    @Test
    fun `entries without an amount are ignored`() {
        file.writeText("""{"entries":[{"t":1000},{"t":2000,"a":250}]}""")

        val history = store.load()

        assertEquals(1, history.entries.size)
        assertEquals(250, history.totalMl)
    }
}
