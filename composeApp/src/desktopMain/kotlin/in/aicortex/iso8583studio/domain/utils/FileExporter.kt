package `in`.aicortex.iso8583studio.domain.utils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Files
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * A platform-aware file exporter that works on both macOS and Windows/PC
 * with automatic platform detection
 */
class FileExporter {
    // Detect the OS once when the class is loaded
    private val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    private val currentOS = when {
        isMacOS -> "macOS"
        isWindows -> "Windows"
        else -> "Linux/Other"
    }

    /**
     * Export a file to a location chosen by the user
     * @param window The ComposeWindow to use as parent for the file dialog
     * @param fileName Suggested file name
     * @param fileExtension The extension of the file without the dot (e.g., "pdf")
     * @param fileContent The content to write to the file
     * @param fileDescription Description for the file type in the dialog
     * @return The path to the exported file or null if export was canceled
     */
    suspend fun exportFile(
        window: ComposeWindow,
        fileName: String,
        fileExtension: String,
        fileContent: ByteArray,
        fileDescription: String = "File"
    ): ExportResult = withContext(Dispatchers.IO) {
        println("Exporting file on $currentOS...")

        val selectedFile: File? = if (isMacOS) {
            // Use FileDialog for macOS for native look and feel
            showMacOSFileDialog(window, fileName, fileExtension)
        } else {
            // Use JFileChooser for Windows and other platforms
            showFileChooserDialog(window, fileName, fileExtension, fileDescription)
        }

        // Handle the case when user cancels the dialog
        if (selectedFile == null) {
            return@withContext ExportResult.Cancelled
        }

        // Make sure the file has the correct extension
        val targetFile = ensureFileExtension(selectedFile, fileExtension)

        // Write the content to the file
        return@withContext try {
            Files.write(targetFile.toPath(), fileContent)
            ExportResult.Success(
                filePath = targetFile.absolutePath,
                platform = currentOS
            )
        } catch (e: Exception) {
            ExportResult.Error("Error saving file: ${e.message}", e)
        }
    }

    /**
     * Shows a native FileDialog for macOS
     */
    private fun showMacOSFileDialog(
        window: ComposeWindow,
        fileName: String,
        fileExtension: String
    ): File? {
        val fileDialog = FileDialog(window, "Save File", FileDialog.SAVE)
        fileDialog.file = ensureFileNameHasExtension(fileName, fileExtension)

        // Set a filter for the file extension (macOS style)
        fileDialog.filenameFilter = FilenameFilter { dir, name -> name?.lowercase()?.endsWith(".$fileExtension") == true }

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
        fileName: String,
        fileExtension: String,
        fileDescription: String
    ): File? {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Save File"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false

            // Set file filter to show only the specific extension
            val filter = FileNameExtensionFilter("$fileDescription (.$fileExtension)", fileExtension)
            fileFilter = filter

            // Set suggested file name
            selectedFile = File(ensureFileNameHasExtension(fileName, fileExtension))
        }

        val result = fileChooser.showSaveDialog(window)

        // Return null if user cancels the dialog
        if (result != JFileChooser.APPROVE_OPTION) {
            return null
        }

        return fileChooser.selectedFile
    }

    /**
     * Ensures the file name has the correct extension
     */
    private fun ensureFileNameHasExtension(fileName: String, extension: String): String {
        return if (fileName.lowercase().endsWith(".$extension")) {
            fileName
        } else {
            "$fileName.$extension"
        }
    }

    /**
     * Ensures the file has the correct extension
     */
    private fun ensureFileExtension(file: File, extension: String): File {
        if (file.name.lowercase().endsWith(".$extension")) {
            return file
        }

        return File("${file.absolutePath}.$extension")
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
}

/**
 * Sealed class representing the result of an export operation
 */
sealed class ExportResult {
    data class Success(val filePath: String, val platform: String) : ExportResult()
    data object Cancelled : ExportResult()
    data class Error(val message: String, val exception: Exception? = null) : ExportResult()
}

/**
 * Data class containing platform information
 */
data class PlatformInfo(
    val osName: String,
    val osVersion: String,
    val osArch: String,
    val isMacOS: Boolean,
    val isWindows: Boolean,
    val isLinux: Boolean
)

/**
 * Remember a FileExporter instance in a composable
 */
@Composable
fun rememberFileExporter(): FileExporter {
    return remember { FileExporter() }
}