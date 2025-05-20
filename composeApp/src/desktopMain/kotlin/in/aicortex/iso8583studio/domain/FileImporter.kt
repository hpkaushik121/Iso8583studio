package `in`.aicortex.iso8583studio.domain

import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Files
import java.text.DecimalFormat
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * A platform-aware file importer for single file selection that works on both macOS and Windows/PC
 */
class FileImporter {
    // Detect the OS once when the class is loaded
    private val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    private val currentOS = when {
        isMacOS -> "macOS"
        isWindows -> "Windows"
        else -> "Linux/Other"
    }

    /**
     * Import a single file chosen by the user
     *
     * @param window The ComposeWindow to use as parent for the file dialog
     * @param title Dialog title
     * @param fileExtensions Accepted file extensions without the dot (e.g., ["pdf", "txt"])
     * @param fileDescription Description for the file type in the dialog
     * @return The result of the import operation
     */
    suspend fun importFile(
        window: ComposeWindow,
        title: String = "Select File",
        fileExtensions: List<String> = emptyList(),
        fileDescription: String = "All Files",
        importLogic:(File) -> ImportResult
    ): ImportResult = withContext(Dispatchers.IO) {
        println("Importing file on $currentOS...")

        val selectedFile: File? = if (isMacOS) {
            // Use FileDialog for macOS for native look and feel
            showMacOSFileDialog(window, title, fileExtensions)
        } else {
            // Use JFileChooser for Windows and other platforms
            showFileChooserDialog(window, title, fileExtensions, fileDescription)
        }

        // Handle the case when user cancels the dialog
        if (selectedFile == null) {
            return@withContext ImportResult.Cancelled
        }

        // Check if the file exists
        if (!selectedFile.exists()) {
            return@withContext ImportResult.Error("The selected file does not exist")
        }

        // Read the file content
        return@withContext try {

            importLogic(selectedFile)
        } catch (e: Exception) {
            ImportResult.Error("Error reading file: ${e.message}", e)
        }
    }

    /**
     * Shows a native FileDialog for macOS
     */
    private fun showMacOSFileDialog(
        window: ComposeWindow,
        title: String,
        fileExtensions: List<String>
    ): File? {
        val fileDialog = FileDialog(window, title, FileDialog.LOAD)

        // Set a filter for the file extensions if provided
        if (fileExtensions.isNotEmpty()) {
            fileDialog.filenameFilter = FilenameFilter { _, name ->
                fileExtensions.isEmpty() || fileExtensions.any { ext ->
                    name.lowercase().endsWith(".$ext")
                }
            }
        }

        // Show the dialog
        fileDialog.isVisible = true

        // User canceled the dialog
        if (fileDialog.file == null) {
            return null
        }

        // Get the selected file path
        val directory = fileDialog.directory
        val file = fileDialog.file
        return File(directory, file)
    }

    /**
     * Shows a JFileChooser dialog for Windows/PC
     */
    private fun showFileChooserDialog(
        window: ComposeWindow,
        title: String,
        fileExtensions: List<String>,
        fileDescription: String
    ): File? {
        val fileChooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = fileExtensions.isEmpty()

            // Set file filter to show only the specific extension(s)
            if (fileExtensions.isNotEmpty()) {
                val extDescription = if (fileExtensions.size == 1) {
                    "$fileDescription (*.${fileExtensions[0]})"
                } else {
                    "$fileDescription (${fileExtensions.joinToString(", ") { "*.$it" }})"
                }

                val filter = FileNameExtensionFilter(
                    extDescription,
                    *fileExtensions.toTypedArray()
                )
                fileFilter = filter
            }
        }

        val result = fileChooser.showOpenDialog(window)

        // Return null if user cancels the dialog
        if (result != JFileChooser.APPROVE_OPTION) {
            return null
        }

        return fileChooser.selectedFile
    }

    /**
     * Get information about the current operating system
     */
    fun getPlatformInfo(): PlatformInfo {
        return PlatformInfo(
            osName = System.getProperty("os.name"),
            osVersion = System.getProperty("os.version"),
            osArch = System.getProperty("os.arch"),
            isMacOS = isMacOS,
            isWindows = isWindows,
            isLinux = !isMacOS && !isWindows
        )
    }

    /**
     * Format file size in a human-readable way
     */
    fun formatFileSize(sizeInBytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = sizeInBytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        val df = DecimalFormat("#.##")
        return "${df.format(size)} ${units[unitIndex]}"
    }
}

/**
 * Sealed class representing the result of an import operation
 */
sealed class ImportResult {
    data class Success(
        val filePath: String = "",
        val fileName: String = "",
        val fileExtension: String = "",
        val fileContent: ByteArray,
        val fileSize: Long = 0,
        val platform: String = ""
    ) : ImportResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (filePath != other.filePath) return false
            if (fileName != other.fileName) return false
            if (fileExtension != other.fileExtension) return false
            if (!fileContent.contentEquals(other.fileContent)) return false
            if (fileSize != other.fileSize) return false

            return true
        }

        override fun hashCode(): Int {
            var result = filePath.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + fileExtension.hashCode()
            result = 31 * result + fileContent.contentHashCode()
            result = 31 * result + fileSize.hashCode()
            return result
        }
    }

    data object Cancelled : ImportResult()

    data class Error(val message: String, val exception: Exception? = null) : ImportResult()
}

/**
 * Platform information data class
 */
data class PlatformInfo(
    val osName: String,
    val osVersion: String,
    val osArch: String,
    val isMacOS: Boolean,
    val isWindows: Boolean,
    val isLinux: Boolean
)
