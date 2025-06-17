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
import `in`.aicortex.iso8583studio.data.model.StudioTool
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.landing.ToolSuite

class HomeScreenViewModel : ScreenModel {
    var searchQuery = mutableStateOf("")
    var selectedCategory = mutableStateOf<ToolSuite?>(null)
    var isLoaded = mutableStateOf(false)
    val tools = StudioTool.values()
    var overviewVisibility = mutableStateOf(false)
    var displayQuoteText =  mutableStateOf("")
    val popularTools = tools.filter { it.isPopular }
    val newTools =
        tools.filter { it.status == DevelopmentStatus.COMING_SOON || it.status == DevelopmentStatus.UNDER_DEVELOPMENT || it.status == DevelopmentStatus.BETA || it.status == DevelopmentStatus.EXPERIMENTAL }

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
