package com.example.hydrohomie.data

import org.json.JSONObject
import java.io.File

/**
 * Reads and writes small JSON files.
 *
 * The file is written to a temporary file first and then renamed, so that a crash while
 * writing cannot destroy the existing file.
 */
class JsonFile(private val file: File) {

    /** Reads the file, returns `null` if it does not exist or cannot be parsed. */
    fun read(): JSONObject? {
        if (!file.exists()) return null
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    /** Writes the object, replacing the file atomically. */
    fun write(json: JSONObject) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.toString())
        if (!temp.renameTo(file)) {
            // renameTo can fail on some file systems, fall back to a plain copy.
            file.writeText(temp.readText())
            temp.delete()
        }
    }

    /** Deletes the file. */
    fun delete() {
        file.delete()
    }

    /** Whether the file exists. */
    fun exists(): Boolean = file.exists()
}
