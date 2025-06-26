package io.cryptocalc.crypto.engines.encryption

import io.cryptocalc.crypto.engines.keys.KeysEngine
import io.cryptocalc.crypto.engines.keys.KeysEngineImpl
import io.cryptocalc.crypto.engines.encryption.EncryptionEngine

class EMVEngines() {
    val encryptionEngine: EncryptionEngine = EncryptionEngineImpl(this)
    val keysEngine: KeysEngine = KeysEngineImpl(this)
}