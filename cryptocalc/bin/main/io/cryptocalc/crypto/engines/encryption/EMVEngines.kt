package io.cryptocalc.crypto.engines.encryption

import io.cryptocalc.crypto.engines.applicationCryptogram.AcEngine
import io.cryptocalc.crypto.engines.applicationCryptogram.AcEngineImpl
import io.cryptocalc.crypto.engines.keys.KeysEngine
import io.cryptocalc.crypto.engines.keys.KeysEngineImpl
import io.cryptocalc.crypto.engines.encryption.EncryptionEngine

class EMVEngines() {
    val encryptionEngine: EncryptionEngine = EncryptionEngineImpl(this)
    val keysEngine: KeysEngine = KeysEngineImpl(this)
    val acEngine: AcEngine = AcEngineImpl(this)
}