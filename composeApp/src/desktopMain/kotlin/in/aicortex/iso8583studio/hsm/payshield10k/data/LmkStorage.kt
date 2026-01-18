package `in`.aicortex.iso8583studio.hsm.payshield10k.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LmkStorage(
    var liveLmks: MutableMap<String, LmkSet> = mutableMapOf(),
    var oldLmk: LmkSet? = null,
    var newLmk: LmkSet? = null,
    var defaultLmkId: String = "00"
) {
    // Companion object for Factory and Constants
    companion object {
        private const val FILE_NAME = "payShield10k_lmkStorage.json"
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        fun load(): LmkStorage {
            val file = File(FILE_NAME)
            return if (file.exists() && file.length() > 0) {
                try {
                    json.decodeFromString<LmkStorage>(file.readText())
                } catch (e: Exception) {
                    LmkStorage() // Fallback on corruption
                }
            } else {
                LmkStorage()
            }
        }
    }

    private fun save() {
        val file = File(FILE_NAME)
        file.writeText(json.encodeToString(this))
    }

    fun getLmk(id: String): LmkSet? = liveLmks[id]

    fun addLmk(lmk: LmkSet) {
        liveLmks[lmk.identifier] = lmk
        save()
    }

    fun deleteLmk(id: String) {
        liveLmks.remove(id)
        save()
    }
}
