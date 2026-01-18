package `in`.aicortex.iso8583studio.hsm.payshield10k

/**
 * ========================================================================================================
 * PAYSHIELD 10K HSM - SLOT MANAGEMENT SYSTEM
 * Complete Implementation of HSM Storage Slots
 * ========================================================================================================
 *
 * HSM Slots are secure storage locations within the HSM used for:
 *
 * 1. **LMK SLOTS** (Multiple LMKs)
 *    - Each LMK occupies a "slot" in the LMK table
 *    - Slot ID: 00-99 (configurable by license)
 *    - Each slot stores: LMK value, scheme, algorithm, status, check value
 *    - Purpose: Multi-tenant support, key separation, migration support
 *
 * 2. **USER STORAGE SLOTS**
 *    - Secure memory for storing encrypted keys, tables, and data
 *    - Index range: 000-FFF (4096 slots)
 *    - Variable or fixed block sizes
 *    - Purpose: Store working keys, decimalization tables, CVV keys, etc.
 *
 * 3. **KEY CHANGE STORAGE SLOTS**
 *    - Temporary storage during LMK migration
 *    - "Old" LMK slot and "New" LMK slot for each LMK ID
 *    - Purpose: Zero-downtime key rotation
 *
 * Benefits:
 * - Multi-tenancy: Different applications use different LMK slots
 * - Performance: Pre-load frequently used keys in user storage
 * - Separation: Cryptographic isolation between applications
 * - Migration: Seamless key rotation without downtime
 *
 * ========================================================================================================
 */


import `in`.aicortex.iso8583studio.hsm.payshield10k.*
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmCommandResult
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.HsmErrorCodes
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.KeyType
import `in`.aicortex.iso8583studio.hsm.payshield10k.data.LmkSet
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

// ====================================================================================================
// SLOT TYPES & MODELS
// ====================================================================================================

/**
 * Slot types in HSM
 */
enum class SlotType {
    LMK_LIVE,           // Live LMK storage
    LMK_KEY_CHANGE,     // LMK key change storage (old/new)
    USER_STORAGE,       // User data/key storage
    SMARTCARD           // Smartcard data storage
}

/**
 * User storage block size modes
 */
enum class StorageMode {
    FIXED_SINGLE,       // Fixed 16-byte blocks
    FIXED_DOUBLE,       // Fixed 32-byte blocks
    FIXED_TRIPLE,       // Fixed 48-byte blocks
    VARIABLE            // Variable size (up to 1024 bytes)
}

/**
 * Slot status
 */
enum class SlotStatus {
    EMPTY,              // Slot is available
    OCCUPIED,           // Slot contains data
    LOCKED,             // Slot is locked (cannot be modified)
    CORRUPTED           // Slot data is corrupted
}

/**
 * LMK Slot - represents a single LMK storage location
 */
@Serializable
data class LmkSlot(
    val slotId: String,                     // 00-99
    val slotType: SlotType,                 // LIVE or KEY_CHANGE
    var lmkSet: LmkSet? = null,             // The actual LMK
    var isOldLmk: Boolean = false,          // For key change: is this "old" LMK?
    var isNewLmk: Boolean = false,          // For key change: is this "new" LMK?
    var isDefault: Boolean = false,         // Is this the default LMK?
    var isManagement: Boolean = false,      // Is this the management LMK?
    var status: SlotStatus = SlotStatus.EMPTY,
    val createdAt: Long = System.currentTimeMillis(),
    var lastAccessedAt: Long = System.currentTimeMillis()
) {
    fun isOccupied(): Boolean = status == SlotStatus.OCCUPIED && lmkSet != null
}

/**
 * User Storage Slot - stores encrypted data/keys
 */
@Serializable
data class UserStorageSlot(
    val index: String,                      // 000-FFF (hex)
    var data: ByteArray = ByteArray(0),     // Encrypted data
    var dataType: UserDataType = UserDataType.GENERIC,
    var blockSize: Int = 16,                // Size in bytes
    var isEncrypted: Boolean = true,        // Data is encrypted under LMK?
    var lmkId: String = "00",               // Which LMK encrypted this?
    var description: String = "",           // What is stored here?
    var status: SlotStatus = SlotStatus.EMPTY,
    val createdAt: Long = System.currentTimeMillis(),
    var lastAccessedAt: Long = System.currentTimeMillis()
)

/**
 * Types of data stored in user storage
 */
enum class UserDataType {
    GENERIC,                    // Generic data
    WORKING_KEY,                // ZPK, TPK, CVK, etc.
    DECIMALIZATION_TABLE,       // PIN generation table
    PVK_KEY,                    // PIN Verification Key
    CVK_KEY,                    // Card Verification Key
    TAK_KEY,                    // Terminal Authentication Key
    BDK_KEY,                    // Base Derivation Key (DUKPT)
    RSA_PRIVATE_KEY,            // RSA private key
    RSA_PUBLIC_KEY,             // RSA public key
    PIN_OFFSET_TABLE,           // PIN offset data
    CUSTOM                      // Application-specific data
}

// ====================================================================================================
// SLOT MANAGER - Central Management System
// ====================================================================================================

/**
 * HSM Slot Manager
 * Manages all slot types and provides unified interface
 */
class HsmSlotManager {

    // LMK Slots (00-99 based on license)
    private val lmkLiveSlots = ConcurrentHashMap<String, LmkSlot>()
    private val lmkKeyChangeSlots = ConcurrentHashMap<String, LmkSlot>()

    // User Storage Slots (000-FFF)
    private val userStorageSlots = ConcurrentHashMap<String, UserStorageSlot>()

    // Configuration
    private var maxLmkSlots = 100           // Based on license
    private var maxUserStorageSlots = 4096  // 000-FFF in hex
    private var storageMode = StorageMode.VARIABLE

    // Statistics
    private var totalReads = 0L
    private var totalWrites = 0L
    private var totalDeletes = 0L

    // ====================================================================================================
    // LMK SLOT OPERATIONS
    // ====================================================================================================

    /**
     * Allocate LMK slot
     */
    fun allocateLmkSlot(
        slotId: String,
        lmkSet: LmkSet,
        isDefault: Boolean = false,
        isManagement: Boolean = false
    ): HsmCommandResult {
        // Validate slot ID
        val id = slotId.toIntOrNull()
        if (id == null || id < 0 || id >= maxLmkSlots) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_LMK_IDENTIFIER,
                "Invalid LMK slot ID: $slotId (must be 00-${maxLmkSlots-1})"
            )
        }

        // Check if slot is already occupied
        if (lmkLiveSlots.containsKey(slotId)) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "LMK slot $slotId is already occupied"
            )
        }

        // Create and store slot
        val slot = LmkSlot(
            slotId = slotId,
            slotType = SlotType.LMK_LIVE,
            lmkSet = lmkSet,
            isDefault = isDefault,
            isManagement = isManagement,
            status = SlotStatus.OCCUPIED
        )

        lmkLiveSlots[slotId] = slot
        totalWrites++

        return HsmCommandResult.Success(
            response = "LMK slot $slotId allocated",
            data = mapOf(
                "slotId" to slotId,
                "checkValue" to lmkSet.checkValue,
                "isDefault" to isDefault.toString(),
                "isManagement" to isManagement.toString()
            )
        )
    }

    /**
     * Get LMK from slot
     */
    fun getLmkFromSlot(slotId: String): LmkSet? {
        val slot = lmkLiveSlots[slotId]
        if (slot != null) {
            slot.lastAccessedAt = System.currentTimeMillis()
            totalReads++
        }
        return slot?.lmkSet
    }

    /**
     * Load "Old" LMK into key change storage
     */
    fun loadOldLmkToKeyChangeStorage(
        slotId: String,
        oldLmkSet: LmkSet
    ): HsmCommandResult {
        val slot = LmkSlot(
            slotId = "${slotId}_OLD",
            slotType = SlotType.LMK_KEY_CHANGE,
            lmkSet = oldLmkSet,
            isOldLmk = true,
            status = SlotStatus.OCCUPIED
        )

        lmkKeyChangeSlots["${slotId}_OLD"] = slot
        totalWrites++

        return HsmCommandResult.Success(
            response = "Old LMK loaded to key change storage for slot $slotId",
            data = mapOf(
                "slotId" to slotId,
                "keyChangeSlot" to "${slotId}_OLD",
                "checkValue" to oldLmkSet.checkValue
            )
        )
    }

    /**
     * Load "New" LMK into key change storage
     */
    fun loadNewLmkToKeyChangeStorage(
        slotId: String,
        newLmkSet: LmkSet
    ): HsmCommandResult {
        val slot = LmkSlot(
            slotId = "${slotId}_NEW",
            slotType = SlotType.LMK_KEY_CHANGE,
            lmkSet = newLmkSet,
            isNewLmk = true,
            status = SlotStatus.OCCUPIED
        )

        lmkKeyChangeSlots["${slotId}_NEW"] = slot
        totalWrites++

        return HsmCommandResult.Success(
            response = "New LMK loaded to key change storage for slot $slotId",
            data = mapOf(
                "slotId" to slotId,
                "keyChangeSlot" to "${slotId}_NEW",
                "checkValue" to newLmkSet.checkValue
            )
        )
    }

    /**
     * Delete LMK from slot
     */
    fun deleteLmkSlot(slotId: String): HsmCommandResult {
        val removed = lmkLiveSlots.remove(slotId)

        if (removed == null) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_LMK_IDENTIFIER,
                "LMK slot $slotId not found"
            )
        }

        totalDeletes++

        return HsmCommandResult.Success(
            response = "LMK slot $slotId deleted",
            data = mapOf("slotId" to slotId)
        )
    }

    /**
     * View LMK Table (VT command)
     */
    fun viewLmkTable(): String {
        val sb = StringBuilder()
        sb.appendLine("LMK Table:")
        sb.appendLine("ID | Authorized | Scheme      | Algorithm    | Status | Check  | Comments")
        sb.appendLine("---|------------|-------------|--------------|--------|--------|----------")

        lmkLiveSlots.entries.sortedBy { it.key }.forEach { (id, slot) ->
            val lmk = slot.lmkSet ?: return@forEach
            sb.appendLine(
                "${id.padEnd(3)}| No         | ${lmk.scheme.padEnd(11)} | " +
                        "${getAlgorithmDisplay(lmk).padEnd(12)} | " +
                        "${if (id.startsWith("9")) "Test" else "Live".padEnd(4)} | " +
                        "${lmk.checkValue.take(6)} | " +
                        "${if (slot.isDefault) "[Default]" else ""}${if (slot.isManagement) "[Mgmt]" else ""}"
            )
        }

        sb.appendLine("\nKey Change Storage Table:")
        if (lmkKeyChangeSlots.isEmpty()) {
            sb.appendLine("No keys loaded in key change storage")
        } else {
            sb.appendLine("ID | Old/New | Check")
            lmkKeyChangeSlots.forEach { (id, slot) ->
                val lmk = slot.lmkSet ?: return@forEach
                val oldNew = if (slot.isOldLmk) "Old" else "New"
                sb.appendLine("${id.take(2)} | $oldNew     | ${lmk.checkValue.take(6)}")
            }
        }

        return sb.toString()
    }

    private fun getAlgorithmDisplay(lmk: LmkSet): String {
        return when {
            lmk.scheme == "VARIANT" -> "3DES(2key)"
            lmk.scheme == "KEY_BLOCK" -> "AES-256"
            else -> "3DES(3key)"
        }
    }

    // ====================================================================================================
    // USER STORAGE SLOT OPERATIONS
    // ====================================================================================================

    /**
     * Load data to user storage (LA command)
     */
    fun loadDataToUserStorage(
        index: String,
        data: ByteArray,
        dataType: UserDataType = UserDataType.GENERIC,
        lmkId: String = "00",
        description: String = ""
    ): HsmCommandResult {
        // Validate index
        val indexInt = try {
            index.toInt(16)
        } catch (e: Exception) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Invalid index: $index (must be hex 000-FFF)"
            )
        }

        if (indexInt < 0 || indexInt >= maxUserStorageSlots) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Index out of range: $index (must be 000-FFF)"
            )
        }

        // Validate block size based on storage mode
        val maxBlockSize = when (storageMode) {
            StorageMode.FIXED_SINGLE -> 16
            StorageMode.FIXED_DOUBLE -> 32
            StorageMode.FIXED_TRIPLE -> 48
            StorageMode.VARIABLE -> 1024
        }

        if (data.size > maxBlockSize) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "Data size ${data.size} exceeds maximum block size $maxBlockSize"
            )
        }

        // Create slot
        val slot = UserStorageSlot(
            index = index.uppercase().padStart(3, '0'),
            data = data,
            dataType = dataType,
            blockSize = data.size,
            lmkId = lmkId,
            description = description,
            status = SlotStatus.OCCUPIED
        )

        userStorageSlots[slot.index] = slot
        totalWrites++

        return HsmCommandResult.Success(
            response = "Data loaded to user storage at index ${slot.index}",
            data = mapOf(
                "index" to slot.index,
                "dataType" to dataType.name,
                "blockSize" to data.size.toString(),
                "lmkId" to lmkId
            )
        )
    }

    /**
     * Read data from user storage (LE command)
     */
    fun readDataFromUserStorage(index: String): HsmCommandResult {
        val normalizedIndex = index.uppercase().padStart(3, '0')
        val slot = userStorageSlots[normalizedIndex]

        if (slot == null || slot.status != SlotStatus.OCCUPIED) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "No data at index $normalizedIndex"
            )
        }

        slot.lastAccessedAt = System.currentTimeMillis()
        totalReads++

        return HsmCommandResult.Success(
            response = "Data retrieved from user storage",
            data = mapOf(
                "index" to slot.index,
                "data" to slot.data,
                "dataType" to slot.dataType.name,
                "blockSize" to slot.blockSize.toString(),
                "lmkId" to slot.lmkId,
                "description" to slot.description
            )
        )
    }

    /**
     * Delete data from user storage (LD command)
     */
    fun deleteDataFromUserStorage(index: String): HsmCommandResult {
        val normalizedIndex = index.uppercase().padStart(3, '0')
        val removed = userStorageSlots.remove(normalizedIndex)

        if (removed == null) {
            return HsmCommandResult.Error(
                HsmErrorCodes.INVALID_INPUT_DATA,
                "No data at index $normalizedIndex"
            )
        }

        totalDeletes++

        return HsmCommandResult.Success(
            response = "Data deleted from user storage at index $normalizedIndex",
            data = mapOf("index" to normalizedIndex)
        )
    }

    /**
     * View user storage contents
     */
    fun viewUserStorage(startIndex: String = "000", endIndex: String = "FFF"): String {
        val start = startIndex.toInt(16)
        val end = endIndex.toInt(16)

        val sb = StringBuilder()
        sb.appendLine("User Storage Contents:")
        sb.appendLine("Index | Type              | Size | LMK | Description")
        sb.appendLine("------|-------------------|------|-----|------------------")

        userStorageSlots.entries
            .filter {
                val idx = it.key.toInt(16)
                idx in start..end
            }
            .sortedBy { it.key }
            .forEach { (idx, slot) ->
                sb.appendLine(
                    "${idx.padEnd(6)}| ${slot.dataType.name.padEnd(17)} | " +
                            "${slot.blockSize.toString().padStart(4)} | " +
                            "${slot.lmkId.padEnd(3)} | " +
                            slot.description.take(20)
                )
            }

        sb.appendLine("\nTotal slots occupied: ${userStorageSlots.size} / $maxUserStorageSlots")

        return sb.toString()
    }

    // ====================================================================================================
    // SLOT UTILITIES & HELPERS
    // ====================================================================================================

    /**
     * Store working key in user storage
     * Convenience method for common use case
     */
    fun storeWorkingKey(
        keyName: String,
        encryptedKey: ByteArray,
        keyType: KeyType,
        lmkId: String = "00"
    ): HsmCommandResult {
        // Find next available slot
        val index = findNextAvailableSlot()

        return loadDataToUserStorage(
            index = index,
            data = encryptedKey,
            dataType = when (keyType) {
                KeyType.TYPE_001 -> UserDataType.WORKING_KEY
                KeyType.TYPE_002 -> UserDataType.PVK_KEY
                KeyType.TYPE_003 -> UserDataType.CVK_KEY
                KeyType.TYPE_008 -> UserDataType.TAK_KEY
                KeyType.TYPE_209 -> UserDataType.BDK_KEY
                else -> UserDataType.WORKING_KEY
            },
            lmkId = lmkId,
            description = keyName
        )
    }

    /**
     * Retrieve working key by name
     */
    fun retrieveWorkingKey(keyName: String): ByteArray? {
        val slot = userStorageSlots.values.find { it.description == keyName }
        return slot?.data
    }

    /**
     * Find next available user storage slot
     */
    private fun findNextAvailableSlot(): String {
        for (i in 0 until maxUserStorageSlots) {
            val index = i.toString(16).uppercase().padStart(3, '0')
            if (!userStorageSlots.containsKey(index)) {
                return index
            }
        }
        throw IllegalStateException("No available user storage slots")
    }

    /**
     * Set storage mode
     */
    fun setStorageMode(mode: StorageMode) {
        storageMode = mode
    }

    /**
     * Get storage statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "lmkLiveSlots" to lmkLiveSlots.size,
            "lmkKeyChangeSlots" to lmkKeyChangeSlots.size,
            "userStorageSlots" to userStorageSlots.size,
            "maxLmkSlots" to maxLmkSlots,
            "maxUserStorageSlots" to maxUserStorageSlots,
            "storageMode" to storageMode.name,
            "totalReads" to totalReads,
            "totalWrites" to totalWrites,
            "totalDeletes" to totalDeletes,
            "lmkUtilization" to "${(lmkLiveSlots.size * 100) / maxLmkSlots}%",
            "userStorageUtilization" to "${(userStorageSlots.size * 100) / maxUserStorageSlots}%"
        )
    }

    /**
     * Clear all slots (for testing/reset)
     */
    fun clearAllSlots() {
        lmkLiveSlots.clear()
        lmkKeyChangeSlots.clear()
        userStorageSlots.clear()
        totalReads = 0
        totalWrites = 0
        totalDeletes = 0
    }

    /**
     * Export slot configuration (for backup)
     */
    fun exportConfiguration(): String {
        val config = StringBuilder()
        config.appendLine("=== HSM SLOT CONFIGURATION ===")
        config.appendLine("Timestamp: ${System.currentTimeMillis()}")
        config.appendLine()
        config.appendLine(viewLmkTable())
        config.appendLine()
        config.appendLine(viewUserStorage())
        config.appendLine()
        config.appendLine("=== STATISTICS ===")
        getStatistics().forEach { (k, v) ->
            config.appendLine("$k: $v")
        }
        return config.toString()
    }
}

// ====================================================================================================
// INTEGRATION WITH PAYSHIELD10K SIMULATOR
// ====================================================================================================

/**
 * Extension to PayShield10KSimulator to add slot management
 */
fun PayShield10K.enableSlotManagement(): HsmSlotManager {
    return HsmSlotManager()
}

/**
 * Slot-aware LMK storage
 * Replaces basic LmkStorage with slot-based system
 */
class SlotAwareLmkStorage(private val slotManager: HsmSlotManager) {

    fun loadLmk(lmkId: String, lmkSet: LmkSet, isDefault: Boolean = false): HsmCommandResult {
        return slotManager.allocateLmkSlot(lmkId, lmkSet, isDefault = isDefault)
    }

    fun getLmk(lmkId: String): LmkSet? {
        return slotManager.getLmkFromSlot(lmkId)
    }

    fun deleteLmk(lmkId: String): HsmCommandResult {
        return slotManager.deleteLmkSlot(lmkId)
    }

    fun viewTable(): String {
        return slotManager.viewLmkTable()
    }
}

// ====================================================================================================
// SLOT-AWARE COMMANDS
// ====================================================================================================

/**
 * LA - Load data to User Storage
 */
data class LoadUserStorageCommand(
    val index: String,
    val data: ByteArray,
    val dataType: UserDataType = UserDataType.GENERIC,
    val lmkId: String = "00",
    val description: String = ""
)

/**
 * LE - Read data from User Storage
 */
data class ReadUserStorageCommand(
    val index: String
)

/**
 * LD - Delete data from User Storage
 */
data class DeleteUserStorageCommand(
    val index: String
)