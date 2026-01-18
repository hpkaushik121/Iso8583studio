package `in`.aicortex.iso8583studio.hsm.payshield10k.data

enum class SmartcardType {
    LMK_COMPONENT,      // Stores LMK component
    AUTHORIZING_OFFICER, // Authorization card
    SETTINGS,           // HSM settings backup
    RACC               // Remote Authorization Card
}