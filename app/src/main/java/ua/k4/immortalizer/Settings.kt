package ua.k4.immortalizer

import org.json.JSONObject
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class Settings(private val file: Path) {
    private val values = hashMapOf(
        "disableBatteryOptimization" to true, "startOnBoot" to true
    )

    init {
        load()
    }

    var disableBatteryOptimization: Boolean
        get() = values["disableBatteryOptimization"]!!
        set(value) {
            values["disableBatteryOptimization"] = value
            save()
        }
    var startOnBoot: Boolean
        get() = values["startOnBoot"]!!
        set(value) {
            values["startOnBoot"] = value
            save()
        }

    private fun load() {
        if (file.notExists()) return
        val json = JSONObject(file.readText())

        values.keys.forEach {
            if (json.has(it)) values[it] = json.getBoolean(it)
        }
    }

    private fun save() {
        file.writeText(JSONObject(values as Map<String, Boolean>).toString())
    }
}
