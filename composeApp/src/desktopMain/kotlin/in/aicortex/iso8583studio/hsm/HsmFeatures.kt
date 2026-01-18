package `in`.aicortex.iso8583studio.hsm

import `in`.aicortex.iso8583studio.hsm.payshield10k.HsmSlotManager

interface HsmFeatures {
    fun getSlotManager(): HsmSlotManager
}