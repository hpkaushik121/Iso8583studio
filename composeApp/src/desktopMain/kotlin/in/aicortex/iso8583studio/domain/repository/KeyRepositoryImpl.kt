package `in`.aicortex.iso8583studio.domain.repository

import `in`.aicortex.iso8583studio.data.model.SecurityKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Implementation of KeyRepository
 */
class KeyRepositoryImpl(
) : KeyRepository {

    private val keysFile = File("keys.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private suspend fun loadKeys(): List<SecurityKey> {
        return if (keysFile.exists()) {
            try {
                json.decodeFromString(keysFile.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private suspend fun saveKeys(keys: List<SecurityKey>) {
        keysFile.writeText(json.encodeToString(keys))
    }

    override fun getKeys(): Flow<List<SecurityKey>> = flow {
        emit(loadKeys())
    }

    override suspend fun getKey(id: String): SecurityKey? {
        return loadKeys().find { it.id == id }
    }

    override suspend fun addKey(key: SecurityKey): Result<SecurityKey> {
        return try {
            val keys = loadKeys().toMutableList()

            if (keys.any { it.id == key.id }) {
                Result.failure(IllegalArgumentException("Key already exists with ID: ${key.id}"))
            } else {
                keys.add(key)
                saveKeys(keys)
                Result.success(key)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateKey(key: SecurityKey): Result<SecurityKey> {
        return try {
            val keys = loadKeys().toMutableList()
            val index = keys.indexOfFirst { it.id == key.id }

            if (index != -1) {
                keys[index] = key
                saveKeys(keys)
                Result.success(key)
            } else {
                Result.failure(IllegalArgumentException("Key not found with ID: ${key.id}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteKey(id: String): Result<Boolean> {
        return try {
            val keys = loadKeys().toMutableList()
            val removed = keys.removeIf { it.id == id }

            if (removed) {
                saveKeys(keys)
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Key not found with ID: $id"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}