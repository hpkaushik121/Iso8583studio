package `in`.aicortex.iso8583studio.hsm.payshield10k.data

/**
 * Authorization activity types
 */
enum class AuthActivity {
    ADMIN_CONSOLE,      // Administrative console commands
    AUDIT_CONSOLE,      // Audit log operations
    COMPONENT_KEY_CONSOLE,  // Key component generation
    MISC_CONSOLE,       // Miscellaneous authorized commands
    CLEAR_PIN_CONSOLE   // Clear PIN operations
}
