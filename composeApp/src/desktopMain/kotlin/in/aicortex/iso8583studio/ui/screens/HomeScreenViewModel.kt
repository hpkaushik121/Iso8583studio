package `in`.aicortex.iso8583studio.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import `in`.aicortex.iso8583studio.ui.screens.landing.ToolSuite

class HomeScreenViewModel : ScreenModel {
    var searchQuery = mutableStateOf("")
    var selectedCategory = mutableStateOf<ToolSuite?>(null)
    var isLoaded = mutableStateOf(false)
    val tools = getAllEnhancedTools()
    var overviewVisibility = mutableStateOf(false)
    var displayQuoteText =  mutableStateOf("")
    val popularTools = tools.filter { it.isPopular }
    val newTools =
        tools.filter { it.status == ToolStatus.NEW || it.status == ToolStatus.BETA || it.status == ToolStatus.EXPERIMENTAL }

    // Data sources and models from the original code (unchanged)
    val paymentQuotes = listOf(
        "💡 \"The future of payments is not about replacing cash, but enabling choice.\" - Unknown",
        "🔐 \"In payments, security and convenience must dance together.\" - Payment Industry Expert",
        "🤝 \"Every transaction tells a story of trust between strangers.\" - Modern Banking",
        "🌐 \"ISO8583 is the silent language that speaks across all payment networks.\" - FinTech Pioneer",
        "🧪 \"Testing today prevents failures tomorrow in the payment ecosystem.\" - Quality Assurance",
        "🛡️ \"A secure payment is a promise kept to every customer.\" - Trust & Safety",
        "✨ \"Innovation in payments means making the complex invisible to users.\" - UX Design",
        "⚡ \"Behind every seamless payment lies rigorous testing and validation.\" - Payment Engineering",
        "🔒 \"Cryptography in payments: Where mathematics meets trust.\" - Security Expert",
        "🎯 \"The best payment experience is the one you don't notice.\" - User Experience",
        "💳 \"EMV chips transformed payments from magnetic strips to smart security.\" - Card Technology",
        "🏦 \"HSMs are the guardians of cryptographic keys in payment systems.\" - Cyber Security",
        "⚡ \"Real-time payments demand real-time testing capabilities.\" - System Architecture",
        "⏱️ \"Every millisecond matters in high-frequency payment processing.\" - Performance Engineering",
        "🔐 \"Payment tokenization: Hiding sensitive data in plain sight.\" - Data Protection"
    )
}


data class EnhancedStudioTool(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isPopular: Boolean = false,
    val category: String = "General",
    val usageCount: Int = 0,
    val status: ToolStatus = ToolStatus.STABLE
)

enum class ToolStatus(val color: Color, val label: String) {
    STABLE(Color(0xFF4CAF50), "Stable"),
    BETA(Color(0xFFFF9800), "Beta"),
    NEW(Color(0xFF2196F3), "New"),
    EXPERIMENTAL(Color(0xFF9C27B0), "Experimental")
}


private fun getAllEnhancedTools(): List<EnhancedStudioTool> {
    return listOf(
        EnhancedStudioTool(
            "Host Simulator", "Payment host response simulation", Icons.Default.Router,
            isPopular = true, category = "Simulation", usageCount = 1247, status = ToolStatus.STABLE
        ),
        EnhancedStudioTool(
            "AES Calculator",
            "AES encryption/decryption",
            Icons.Default.Lock,
            isPopular = true,
            category = "Cryptography",
            usageCount = 2156,
            status = ToolStatus.STABLE
        ),
        EnhancedStudioTool(
            "CVV Calculator",
            "Card Verification Value",
            Icons.Default.CreditCard,
            isPopular = true,
            category = "Card Processing",
            usageCount = 987,
            status = ToolStatus.STABLE
        ),
        EnhancedStudioTool(
            "Base64 Encoder", "Base64 encoding/decoding", Icons.Default.Code,
            isPopular = false, category = "Encoding", usageCount = 1654, status = ToolStatus.STABLE
        ),
        EnhancedStudioTool(
            "Hash Calculator",
            "MD5, SHA-1, SHA-256",
            Icons.Default.Tag,
            isPopular = false,
            category = "Cryptography",
            usageCount = 1432,
            status = ToolStatus.STABLE
        ),
        EnhancedStudioTool(
            "APDU Simulator", "Smart card APDU commands", Icons.Default.Nfc,
            isPopular = true, category = "Smart Cards", usageCount = 765, status = ToolStatus.NEW
        ),
        EnhancedStudioTool(
            "HSM Commander", "Interact with Hardware Security Modules", Icons.Default.Security,
            isPopular = false, category = "HSM", usageCount = 453, status = ToolStatus.BETA
        ),
        EnhancedStudioTool(
            "PIN Block Tool",
            "Calculate and verify PIN blocks",
            Icons.Default.Password,
            isPopular = true,
            category = "Cryptography",
            usageCount = 1890,
            status = ToolStatus.EXPERIMENTAL
        )
    )
}
