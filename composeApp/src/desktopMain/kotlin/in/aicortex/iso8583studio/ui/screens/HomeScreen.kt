package `in`.aicortex.iso8583studio.ui.screens.landing

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.aicortex.iso8583studio.data.model.StudioTool
import `in`.aicortex.iso8583studio.ui.screens.HomeScreenViewModel
import `in`.aicortex.iso8583studio.ui.screens.components.DevelopmentStatus
import `in`.aicortex.iso8583studio.ui.screens.components.UnderDevelopmentChip
import kotlinx.coroutines.delay

/**
 * Comprehensive tool suite following the existing theme
 */
enum class ToolSuite(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val tools: List<StudioTool>
) {
    CORE_SIMULATORS(
        displayName = "Payment Simulators",
        description = "Core payment system simulators for transaction testing",
        icon = Icons.Default.Router,
        tools = listOf(
            StudioTool.HOST_SIMULATOR,
            StudioTool.HSM_SIMULATOR,
            StudioTool.POS_TERMINAL,
            StudioTool.ATM_SIMULATOR,
            StudioTool.PAYMENT_SWITCH,
            StudioTool.ACQUIRER_GATEWAY,
            StudioTool.APDU_SIMULATOR,
            StudioTool.ISSUER_SYSTEM,
            StudioTool.ECR_SIMULATOR,
        )
    ),

    EMV_CARD_TOOLS(
        displayName = "EMV & Card Tools",
        description = "Smart card, EMV, and contactless payment tools",
        icon = Icons.Default.CreditCard,
        tools = listOf(
            StudioTool.EMV_41_CRYPTO,
            StudioTool.EMV_42_CRYPTO,
            StudioTool.MASTERCARD_CRYPTO,
            StudioTool.VSDC_CRYPTO,
            StudioTool.SDA_VERIFICATION,
            StudioTool.DDA_VERIFICATION,
            StudioTool.CAP_TOKEN,
            StudioTool.HCE_VISA,
            StudioTool.SECURE_MESSAGING,
            StudioTool.ATR_PARSER,
            StudioTool.EMV_DATA_PARSER,
            StudioTool.EMV_TAG_DICTIONARY
        )
    ),

    CRYPTOGRAPHY(
        displayName = "Cryptographic Tools",
        description = "Encryption, decryption, and security utilities",
        icon = Icons.Default.VpnKey,
        tools = listOf(
            StudioTool.AES_CALCULATOR,
            StudioTool.DES_CALCULATOR,
            StudioTool.RSA_CALCULATOR,
            StudioTool.ECDSA_CALCULATOR,
            StudioTool.HASH_CALCULATOR,
            StudioTool.THALES_RSA,
            StudioTool.FPE_CALCULATOR
        )
    ),

    KEY_MANAGEMENT(
        displayName = "Key Management",
        description = "HSM keys, key blocks, and derivation tools",
        icon = Icons.Default.VpnKey,
        tools = listOf(
            StudioTool.DEA_KEYS,
            StudioTool.KEYSHARE_GENERATOR,
            StudioTool.THALES_KEYS,
            StudioTool.FUTUREX_KEYS,
            StudioTool.ATALLA_KEYS,
            StudioTool.SAFENET_KEYS,
            StudioTool.THALES_KEY_BLOCKS,
            StudioTool.TR31_KEY_BLOCKS,
            StudioTool.SSL_CERTIFICATE,
            StudioTool.RSA_DER_KEYS
        )
    ),

    PAYMENT_UTILITIES(
        displayName = "Payment Utilities",
        description = "Payment processing and validation utilities",
        icon = Icons.Default.Payment,
        tools = listOf(
            StudioTool.CVV_CALCULATOR,
            StudioTool.AMEX_CSC,
            StudioTool.AS2805_CALCULATOR,
            StudioTool.BITMAP_CALCULATOR,
            StudioTool.MESSAGE_PARSER
        )
    ),

    DATA_CONVERTERS(
        displayName = "Data Converters",
        description = "Format conversion and encoding utilities",
        icon = Icons.Default.Transform,
        tools = listOf(
            StudioTool.BASE64_ENCODER,
            StudioTool.BASE94_ENCODER,
            StudioTool.BCD_CONVERTER,
            StudioTool.CHARACTER_ENCODER,
            StudioTool.CHECK_DIGIT
        )
    )
}


/**
 * Main landing page following existing theme and style
 */
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel,
    onToolSelected: (StudioTool) -> Unit,
    onGetStarted: () -> Unit = {}
) {



    LaunchedEffect(Unit) {
        delay(200)
        viewModel.isLoaded.value = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left panel - Tool categories
                Card(
                    modifier = Modifier
                        .width(400.dp)
                        .fillMaxHeight()
                        .padding(12.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Text(
                            "ISO8583Studio",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Professional Payment Testing Platform",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )

                        // Tool categories list
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            elevation = 0.dp,
                            backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Tool Categories",
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )

                                ToolSuite.values().forEach { category ->
                                    CategoryButton(
                                        category = category,
                                        isSelected = viewModel.selectedCategory.value == category,
                                        onClick = {
                                            viewModel.selectedCategory.value =
                                                if (viewModel.selectedCategory.value == category) null else category
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Right panel - Tool showcase or overview
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (viewModel.searchQuery.value.isNotEmpty()) {
                        // Show search results across all tools
                        SearchResultsView(
                            query = viewModel.searchQuery.value,
                            onToolSelected = onToolSelected
                        )
                    } else if (viewModel.selectedCategory.value != null) {
                        // Show tools for selected category
                        CategoryToolsView(
                            category = viewModel.selectedCategory.value!!,
                            onToolSelected = onToolSelected
                        )
                    } else {
                        // Show overview with stats and quote
                        OverviewContent(viewModel)
                    }
                }
            }
        }

        // Floating search bar at center bottom
        FloatingSearchBar(
            query = viewModel.searchQuery.value,
            onQueryChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 32.dp)
        )
    }
}

@Composable
private fun CategoryButton(
    category: ToolSuite,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp,
            pressedElevation = 4.dp
        ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
            contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.displayName,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.displayName,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
                Text(
                    "${category.tools.size} tools",
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface.copy(
                        alpha = 0.6f
                    )
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryToolsView(
    category: ToolSuite,
    onToolSelected: (StudioTool) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    category.displayName,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    category.description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tools grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(category.tools) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = { onToolSelected(tool) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    onToolSelected: (StudioTool) -> Unit
) {
    val allTools = getAllTools()
    val results = allTools.filter { tool ->
        tool.label.contains(query, ignoreCase = true) ||
                tool.description.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Search Results for \"$query\"",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (results.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            "No tools found",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "Try searching with different keywords",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Text(
                "Found ${results.size} tool${if (results.size == 1) "" else "s"}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onToolSelected(tool) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(700.dp), // Increased width significantly
        elevation = 12.dp, // Higher elevation to make it more prominent
        shape = RoundedCornerShape(32.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search across all 47 tools and simulators...",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color.Transparent,
                textColor = MaterialTheme.colors.onSurface,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(32.dp),
            textStyle = MaterialTheme.typography.body1.copy(fontSize = 16.sp)
        )
    }
}

@Composable
private fun ToolCard(
    tool: StudioTool,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHovered = true
                        tryAwaitRelease()
                        isHovered = false
                    },
                    onTap = { onClick() }
                )
            },
        elevation = if (isHovered) 6.dp else 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (isHovered) MaterialTheme.colors.primary.copy(alpha = 0.05f) else MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tool.isPopular) {
                        StatusBadge("POPULAR", MaterialTheme.colors.secondary)
                    }
                    if (tool.isNew) {
                        StatusBadge("NEW", MaterialTheme.colors.primary)
                    }
                    if (tool.status != DevelopmentStatus.STABLE) {
                        UnderDevelopmentChip(status = tool.status)
                    }
                }
            }

            Column {
                Text(
                    text = tool.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color
) {
    Card(
        backgroundColor = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        elevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Helper functions
private fun getAllTools(): List<StudioTool> {
    return ToolSuite.values().flatMap { it.tools }
}
