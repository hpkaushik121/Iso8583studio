package io.cryptocalc.crypto.engines

import io.cryptocalc.crypto.engines.encryption.EMVEngines

interface Engine {
    val emvEngines: EMVEngines
}