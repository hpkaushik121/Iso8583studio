package `in`.aicortex.iso8583studio.domain.repository

import `in`.aicortex.iso8583studio.data.model.SecurityKey
import kotlinx.coroutines.flow.Flow


/**
 * Repository interface for security keys
 */
interface KeyRepository {
    /**
     * Get all security keys
     */
    fun getKeys(): Flow<List<SecurityKey>>

    /**
     * Get a specific security key
     */
    suspend fun getKey(id: String): SecurityKey?

    /**
     * Add a new security key
     */
    suspend fun addKey(key: SecurityKey): Result<SecurityKey>

    /**
     * Update an existing security key
     */
    suspend fun updateKey(key: SecurityKey): Result<SecurityKey>

    /**
     * Delete a security key
     */
    suspend fun deleteKey(id: String): Result<Boolean>
}