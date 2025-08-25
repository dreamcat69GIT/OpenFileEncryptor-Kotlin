import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import java.awt.Desktop
import java.net.URI
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.Key
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.crypto.BadPaddingException
import javax.crypto.AEADBadTagException
import java.awt.datatransfer.DataFlavor
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

private val DarkBlueColors = darkColors(
    primary = Color(0xFF2196F3),
    primaryVariant = Color(0xFF1976D2),
    secondary = Color(0xFF03A9F4),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
@Preview
fun OpenFileEncryptorApp() {
    val initialSettings = remember { SettingsManager.loadSettings() }
    var tab by remember { mutableStateOf("Encryption") }
    var encryptionPath by remember { mutableStateOf("") }
    var encryptionKey by remember { mutableStateOf("") }
    var decryptionPath by remember { mutableStateOf("") }
    var decryptionKey by remember { mutableStateOf("") }
    var generateKey by remember { mutableStateOf(false) }
    var logsEnabled by remember { mutableStateOf(initialSettings.saveLogs) }
    var autoUpdate by remember { mutableStateOf(initialSettings.autoUpdate) }
    var analyticsEnabled by remember { mutableStateOf(initialSettings.crashAnalytics) }
    var isFileMode by remember { mutableStateOf(initialSettings.isFileMode) }
    val availableLanguages = remember { mutableStateListOf<String>() }
    var currentLanguage by remember { mutableStateOf(LanguageLoader.getDefaultLanguage()) }
    val consoleOutput = remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val encryptionProgress = remember { mutableStateOf(0) }
    val decryptionProgress = remember { mutableStateOf(0) }
    val isProcessing = remember { mutableStateOf(0) }
    val currentProcessingFile = remember { mutableStateOf("") }
    var generatedKey by remember { mutableStateOf("") }
    val isElevated = remember { ElevationChecker.isElevated() }
    val errorMessage = remember { mutableStateOf("") }
    val Message = remember { mutableStateOf("") }


    fun saveCurrentSettings() {
        val settingsToSave = AppSettings(
            saveLogs = logsEnabled,
            autoUpdate = autoUpdate,
            crashAnalytics = analyticsEnabled,
            isFileMode = isFileMode,
            selectedLanguage = currentLanguage.language
        )
        SettingsManager.saveSettings(settingsToSave)
    }

    fun showError(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            errorMessage.value = message
            delay(3000)
            if (errorMessage.value == message) {
                errorMessage.value = ""
            }
        }
    }
    fun showMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Message.value = message
            delay(5000)
            if (Message.value == message) {
                Message.value = ""
            }
        }
    }

    LaunchedEffect(Unit) {
        LanguageLoader.loadLanguages()
        availableLanguages.addAll(LanguageLoader.getAvailableLanguages())
        currentLanguage = LanguageLoader.getLanguage(initialSettings.selectedLanguage) ?: LanguageLoader.getDefaultLanguage()
        tab = currentLanguage.encryption_tab
        SupabaseManager.initialize()

    }
    LaunchedEffect(logsEnabled, autoUpdate, analyticsEnabled, isFileMode, currentLanguage) {
        saveCurrentSettings()
    }

    fun writeLog(message: String) {
        if (logsEnabled) {
            CoroutineScope(Dispatchers.Main).launch {
                File("Log.txt").appendText("[${Timestamp(System.currentTimeMillis())}] $message\n")
            }
        }
    }

    fun saveKeyToFile(key: String) {
        val fileChooser = JFileChooser().apply {
            dialogTitle = currentLanguage.save_key_to_file
            fileSelectionMode = JFileChooser.FILES_ONLY
            selectedFile = File("encryption_key.txt")
        }
        val result = fileChooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                fileChooser.selectedFile.writeText(key)
                writeLog(currentLanguage.log_key_saved_success.replace("%s", fileChooser.selectedFile.absolutePath))
                showMessage(currentLanguage.log_key_saved_success + " $Key")
            } catch (e: Exception) {
                showError(currentLanguage.log_save_key_failed + "$e")
                if (analyticsEnabled) {
                    SupabaseManager.logEvent("save_key_failed", mapOf("error_message" to (e.message ?: "Unknown error"), "file_path" to (fileChooser.selectedFile.absolutePath)))
                }
            }
        }
    }

    val textFieldTextStyle = TextStyle(
        color = Color.White,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )

    val buttonTextStyle = TextStyle(fontSize = 14.sp)

    val customTextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = Color.White,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        backgroundColor = Color(0xFF2D2D2D),
        focusedLabelColor = Color(0xFF64B5F6),
        unfocusedLabelColor = Color(0xFF9E9E9E),
        cursorColor = Color.White
    )

    Column(Modifier.padding(horizontal = 4.dp, vertical = 16.dp).fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tabs = remember(currentLanguage) {
                listOf(
                    currentLanguage.encryption_tab,
                    currentLanguage.decryption_tab,
                    currentLanguage.settings_tab
                )
            }
            tabs.forEach { tabName ->
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val isPressed by interactionSource.collectIsPressedAsState()

                val backgroundColor by animateColorAsState(
                    targetValue = (when {
                        tab == tabName -> MaterialTheme.colors.primary
                        isPressed -> MaterialTheme.colors.surface.darker(0.2f)
                        isHovered -> MaterialTheme.colors.surface.darker(0.1f)
                        else -> MaterialTheme.colors.surface
                    })
                )

                Button(
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        tab = tabName
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = backgroundColor,
                        contentColor = MaterialTheme.colors.onPrimary
                    ),
                    interactionSource = interactionSource,
                    enabled = isProcessing.value == 0
                ) {
                    Text(
                        tabName,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(fontSize = 14.sp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (tab) {
            currentLanguage.encryption_tab -> {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = generateKey,
                            onCheckedChange = { generateKey = it },
                            modifier = Modifier.padding(end = 8.dp),
                            enabled = isProcessing.value == 0
                        )
                        Text(currentLanguage.generate_key)
                        if (generateKey) {
                            Spacer(Modifier.width(8.dp))
                            ButtonWithInteraction(
                                onClick = {
                                    if (generatedKey.isNotBlank()) {
                                        saveKeyToFile(generatedKey)
                                    } else {
                                        showError(currentLanguage.log_error_no_key_generated)
                                    }
                                },
                                text = currentLanguage.save_key_to_file,
                                buttonTextStyle = buttonTextStyle,
                                baseColor = MaterialTheme.colors.primary,
                                modifier = Modifier.width(120.dp).height(50.dp),
                                enabled = isProcessing.value == 0
                            )
                        }
                    }

                    Spacer(Modifier.height(15.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ButtonWithInteraction(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    fileSelectionMode = if (isFileMode) JFileChooser.FILES_ONLY else JFileChooser.DIRECTORIES_ONLY
                                    dialogTitle = currentLanguage.browse
                                }
                                val result = fileChooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    encryptionPath = fileChooser.selectedFile.absolutePath
                                }
                            },
                            text = currentLanguage.browse,
                            buttonTextStyle = buttonTextStyle,
                            baseColor = MaterialTheme.colors.primary,
                            modifier = Modifier.width(120.dp).height(50.dp),
                            enabled = isProcessing.value == 0
                        )
                        TextField(
                            value = encryptionPath,
                            onValueChange = { encryptionPath = it },
                            label = { Text(currentLanguage.path_to_directory) },
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = textFieldTextStyle,
                            singleLine = true,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f),
                            enabled = isProcessing.value == 0
                        )
                    }

                    Spacer(Modifier.height(15.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ButtonWithInteraction(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    dialogTitle = "Select Encryption Key File"
                                    fileSelectionMode = JFileChooser.FILES_ONLY
                                    fileFilter = FileNameExtensionFilter("Key Files (*.key, *.txt)", "key", "txt")
                                }
                                val result = fileChooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    try {
                                        encryptionKey = fileChooser.selectedFile.readText()
                                        writeLog("Encryption key loaded from file: ${fileChooser.selectedFile.absolutePath}")
                                        showMessage(currentLanguage.log_key_saved_success + " ${fileChooser.selectedFile.absolutePath}")
                                    } catch (e: Exception) {
                                        writeLog("Failed to read encryption key file: ${e.message}")
                                        if (analyticsEnabled) {
                                            SupabaseManager.logEvent("read_key_file_failed", mapOf("error_message" to (e.message ?: "Unknown error")))
                                        }
                                    }
                                }
                            },
                            text = currentLanguage.browse,
                            buttonTextStyle = buttonTextStyle,
                            baseColor = MaterialTheme.colors.primary,
                            enabled = !generateKey && isProcessing.value == 0,
                            modifier = Modifier.width(120.dp).height(50.dp)
                        )
                        TextField(
                            value = encryptionKey,
                            onValueChange = { encryptionKey = it },
                            label = { Text(currentLanguage.enter_encryption_key) },
                            enabled = !generateKey && isProcessing.value == 0,
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = textFieldTextStyle,
                            singleLine = true,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    ButtonWithInteraction(
                        onClick = {
                            if (isProcessing.value > 0) {
                                writeLog(currentLanguage.log_operation_in_progress)
                                return@ButtonWithInteraction
                            }

                            if (encryptionPath.isBlank()) {
                                showError(currentLanguage.log_error_no_encryption_path)
                                if (analyticsEnabled) {
                                    SupabaseManager.logEvent("encryption_path_empty", mapOf("reason" to "Encryption path is not selected."))
                                }
                                return@ButtonWithInteraction
                            }
                            if (!generateKey && encryptionKey.isBlank()) {
                                showError(currentLanguage.log_error_no_encryption_key)
                                if (analyticsEnabled) {
                                    SupabaseManager.logEvent("encryption_key_empty", mapOf("reason" to "Encryption key is not provided."))
                                }
                                return@ButtonWithInteraction
                            }

                            isProcessing.value = 1
                            writeLog(currentLanguage.log_encryption_started)
                            encryptionProgress.value = 0
                            currentProcessingFile.value = ""
                            errorMessage.value = ""

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val fileOrDir = File(encryptionPath)
                                    val keyToUse = if (generateKey) {
                                        val generated = Cryptography.generateKey()
                                        generatedKey = generated
                                        writeLog("${currentLanguage.log_key_generated}: $generated")
                                        writeLog(currentLanguage.log_save_key_warning)
                                        showMessage(currentLanguage.log_save_key_warning)
                                        val clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                        clipboard.setContents(StringSelection(generated), null)
                                        writeLog(currentLanguage.log_key_copied)
                                        generated
                                    } else {
                                        encryptionKey
                                    }

                                    if (fileOrDir.exists()) {
                                        if (isFileMode) {
                                            if (fileOrDir.isFile) {
                                                currentProcessingFile.value = fileOrDir.name
                                                writeLog("${currentLanguage.log_encrypting_file}: ${fileOrDir.name}")
                                                Cryptography.encryptFile(fileOrDir, keyToUse)
                                                writeLog(currentLanguage.log_file_encrypted_success.replace("%s", fileOrDir.name))
                                            } else {
                                                showError(currentLanguage.log_error_invalid_file_selected)
                                                return@launch
                                            }
                                        } else {
                                            if (fileOrDir.isDirectory) {
                                                writeLog("${currentLanguage.log_encrypting_directory}: ${fileOrDir.name}")
                                                processDirectory(fileOrDir, keyToUse, true,
                                                    updateFileNameCallback = { fileName -> currentProcessingFile.value = fileName },
                                                    progressCallback = { processed, total ->
                                                        val progress = if (total > 0) ((processed.toDouble() / total) * 100).toInt() else 0
                                                        encryptionProgress.value = progress
                                                    },
                                                    logErrorCallback = { reason, ex ->
                                                        if (analyticsEnabled) {
                                                            SupabaseManager.logEvent(reason, mapOf("exception_message" to (ex?.message ?: "Unknown error"), "exception_class" to ex?.javaClass?.simpleName.orEmpty()))
                                                        }
                                                    }
                                                )
                                                writeLog(currentLanguage.log_directory_encrypted_success.replace("%s", fileOrDir.name))
                                            } else {
                                                if (analyticsEnabled) {
                                                    SupabaseManager.logEvent("invalid_directory_selected_for_encryption", mapOf("path" to encryptionPath))
                                                }
                                                showError(currentLanguage.log_error_invalid_directory_selected)
                                            }
                                        }
                                    } else {
                                        if (analyticsEnabled) {
                                            SupabaseManager.logEvent("path_not_found_for_encryption", mapOf("path" to encryptionPath))
                                        }
                                        showError(currentLanguage.log_error_path_not_found.replace("%s", encryptionPath))
                                    }
                                } catch (e: Exception) {
                                    val errorText = when (e) {
                                        is IllegalArgumentException -> currentLanguage.log_error_encryption_failed.replace("%s", currentLanguage.log_error_decryption_argument.replace("%s", e.message ?: "Unknown"))
                                        else -> currentLanguage.log_error_encryption_failed.replace("%s", e.message ?: "Unknown error")
                                    }
                                    showError(errorText)
                                    if (analyticsEnabled) {
                                        SupabaseManager.logEvent("encryption_failed", mapOf("error_message" to (e.message ?: "Unknown error"), "exception_class" to e.javaClass.simpleName))
                                    }
                                } finally {
                                    isProcessing.value = 0
                                    encryptionProgress.value = 0
                                    currentProcessingFile.value = ""
                                    writeLog(currentLanguage.log_encryption_finished)
                                }
                            }
                        },
                        text = currentLanguage.encrypt,
                        buttonTextStyle = buttonTextStyle,
                        baseColor = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = isProcessing.value == 0
                    )
                }
            }

            currentLanguage.decryption_tab -> {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ButtonWithInteraction(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    fileSelectionMode = if (isFileMode) JFileChooser.FILES_ONLY else JFileChooser.DIRECTORIES_ONLY
                                    dialogTitle = currentLanguage.browse
                                    if (isFileMode) {
                                        fileFilter = FileNameExtensionFilter(currentLanguage.encrypted_files_filter, "encrypted")
                                    } else {
                                        fileFilter = null
                                    }
                                }
                                val result = fileChooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    decryptionPath = fileChooser.selectedFile.absolutePath
                                }
                            },
                            text = currentLanguage.browse,
                            buttonTextStyle = buttonTextStyle,
                            baseColor = MaterialTheme.colors.primary,
                            modifier = Modifier.width(120.dp).height(50.dp),
                            enabled = isProcessing.value == 0
                        )
                        TextField(
                            value = decryptionPath,
                            onValueChange = { decryptionPath = it },
                            label = { Text(currentLanguage.path_to_directory) },
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = textFieldTextStyle,
                            singleLine = true,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f),
                            enabled = isProcessing.value == 0
                        )
                    }

                    Spacer(Modifier.height(15.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ButtonWithInteraction(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    dialogTitle = "Select Decryption Key File"
                                    fileSelectionMode = JFileChooser.FILES_ONLY
                                    fileFilter = FileNameExtensionFilter("Key Files (*.key, *.txt)", "key", "txt")
                                }
                                val result = fileChooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    try {
                                        decryptionKey = fileChooser.selectedFile.readText()
                                        writeLog("Decryption key loaded from file: ${fileChooser.selectedFile.absolutePath}")
                                    } catch (e: Exception) {
                                        writeLog("Failed to read decryption key file: ${e.message}")
                                        if (analyticsEnabled) {
                                            SupabaseManager.logEvent("read_key_file_failed", mapOf("error_message" to (e.message ?: "Unknown error")))
                                        }
                                    }
                                }
                            },
                            text = currentLanguage.browse,
                            buttonTextStyle = buttonTextStyle,
                            baseColor = MaterialTheme.colors.primary,
                            modifier = Modifier.width(120.dp).height(50.dp),
                            enabled = isProcessing.value == 0
                        )
                        TextField(
                            value = decryptionKey,
                            onValueChange = { decryptionKey = it },
                            label = { Text(currentLanguage.enter_decryption_key) },
                            colors = customTextFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = textFieldTextStyle,
                            singleLine = true,
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f),
                            enabled = isProcessing.value == 0
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    ButtonWithInteraction(
                        onClick = {
                            if (isProcessing.value > 0) {
                                writeLog(currentLanguage.log_operation_in_progress)
                                return@ButtonWithInteraction
                            }

                            if (decryptionPath.isBlank()) {
                                showError(currentLanguage.log_error_no_decryption_path)
                                if (analyticsEnabled) {
                                    SupabaseManager.logEvent("decryption_path_empty", mapOf("reason" to "Decryption path is not selected."))
                                }
                                return@ButtonWithInteraction
                            }
                            if (decryptionKey.isBlank()) {
                                showError(currentLanguage.log_error_no_decryption_key)
                                if (analyticsEnabled) {
                                    SupabaseManager.logEvent("decryption_key_empty", mapOf("reason" to "Decryption key is not provided."))
                                }
                                return@ButtonWithInteraction
                            }

                            isProcessing.value = 2
                            writeLog(currentLanguage.log_decryption_started)
                            decryptionProgress.value = 0
                            currentProcessingFile.value = ""
                            errorMessage.value = ""

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val fileOrDir = File(decryptionPath)

                                    if (fileOrDir.exists()) {
                                        if (isFileMode) {
                                            if (fileOrDir.isFile) {
                                                if (!fileOrDir.name.endsWith(".encrypted")) {
                                                    showError(currentLanguage.log_error_not_encrypted_file)
                                                    if (analyticsEnabled) {
                                                        SupabaseManager.logEvent("invalid_file_type", mapOf("file_name" to fileOrDir.name))
                                                    }
                                                    isProcessing.value = 0
                                                    return@launch
                                                }
                                                currentProcessingFile.value = fileOrDir.name
                                                writeLog("${currentLanguage.log_decrypting_file}: ${fileOrDir.name}")
                                                Cryptography.decryptFile(fileOrDir, decryptionKey)
                                                writeLog(currentLanguage.log_file_decrypted_success.replace("%s", fileOrDir.name))
                                            } else {
                                                showError(currentLanguage.log_error_invalid_file_selected)
                                                if (analyticsEnabled) {
                                                    SupabaseManager.logEvent("invalid_file_selected_for_decryption", mapOf("path" to decryptionPath))
                                                }
                                            }
                                        } else {
                                            if (fileOrDir.isDirectory) {
                                                writeLog("${currentLanguage.log_decryption_directory}: ${fileOrDir.name}")
                                                processDirectory(fileOrDir, decryptionKey, false,
                                                    updateFileNameCallback = { fileName -> currentProcessingFile.value = fileName },
                                                    progressCallback = { processed, total ->
                                                        val progress = if (total > 0) ((processed.toDouble() / total) * 100).toInt() else 0
                                                        decryptionProgress.value = progress
                                                    },
                                                    logErrorCallback = { reason, ex ->
                                                        if (analyticsEnabled) {
                                                            SupabaseManager.logEvent(reason, mapOf("exception_message" to (ex?.message ?: "Unknown error"), "exception_class" to ex?.javaClass?.simpleName.orEmpty()))
                                                        }
                                                    }
                                                )
                                                writeLog(currentLanguage.log_directory_decrypted_success.replace("%s", fileOrDir.name))
                                            } else {
                                                showError(currentLanguage.log_error_invalid_directory_selected)
                                                if (analyticsEnabled) {
                                                    SupabaseManager.logEvent("invalid_directory_selected_for_decryption", mapOf("path" to decryptionPath))
                                                }
                                            }
                                        }
                                    } else {
                                        showError(currentLanguage.log_error_path_not_found.replace("%s", decryptionPath))
                                        if (analyticsEnabled) {
                                            SupabaseManager.logEvent("path_not_found_for_decryption", mapOf("path" to decryptionPath))
                                        }
                                    }
                                } catch (e: Exception) {
                                    val errorText = when (e) {
                                        is BadPaddingException -> currentLanguage.log_error_decryption_invalid_key
                                        is AEADBadTagException -> currentLanguage.log_error_decryption_invalid_key_tag
                                        is IllegalArgumentException -> currentLanguage.log_error_decryption_argument.replace("%s", e.message ?: "Unknown")
                                        else -> currentLanguage.log_error_decryption_failed.replace("%s", e.message ?: "Unknown error")
                                    }
                                    showError(errorText)
                                    val eventName = when (e) {
                                        is BadPaddingException -> "decryption_padding_error"
                                        is AEADBadTagException -> "decryption_tag_error"
                                        is IllegalArgumentException -> "decryption_argument_error"
                                        else -> "decryption_failed"
                                    }
                                    if (analyticsEnabled) {
                                        SupabaseManager.logEvent(eventName, mapOf("error_message" to (e.message ?: "Unknown error"), "exception_class" to e.javaClass.simpleName))
                                    }
                                } finally {
                                    isProcessing.value = 0
                                    decryptionProgress.value = 0
                                    currentProcessingFile.value = ""
                                    writeLog(currentLanguage.log_decryption_finished)
                                }
                            }
                        },
                        text = currentLanguage.decrypt,
                        buttonTextStyle = buttonTextStyle,
                        baseColor = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = isProcessing.value == 0
                    )
                }
            }

            currentLanguage.settings_tab -> {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxSize()) {
                    Divider(color = Color(0xFF3A3A3A), thickness = 1.dp)
                    DropdownMenuBox(
                        label = currentLanguage.lang,
                        options = availableLanguages,
                        selected = currentLanguage.language,
                        onSelected = { langName ->
                            LanguageLoader.getLanguage(langName)?.let {
                                currentLanguage = it
                                tab = when (tab) {
                                    currentLanguage.encryption_tab -> it.encryption_tab
                                    currentLanguage.decryption_tab -> it.decryption_tab
                                    currentLanguage.settings_tab -> it.settings_tab
                                    else -> it.encryption_tab
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = isProcessing.value == 0
                    )
                    Divider(color = Color(0xFF3A3A3A), thickness = 1.dp)
                    DropdownMenuBox(
                        label = currentLanguage.work_mode,
                        options = listOf(currentLanguage.mode_file, currentLanguage.mode_directory),
                        selected = if (isFileMode) currentLanguage.mode_file else currentLanguage.mode_directory,
                        onSelected = { mode ->
                            isFileMode = (mode == currentLanguage.mode_file)
                            writeLog("${currentLanguage.log_mode_changed}: $mode")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = isProcessing.value == 0
                    )
                    Divider(color = Color(0xFF3A3A3A), thickness = 1.dp)
                    SettingsSwitchItem(currentLanguage.save_logs, logsEnabled, { logsEnabled = it }, isProcessing.value == 0)
                    Divider(color = Color(0xFF3A3A3A), thickness = 1.dp)
                    SettingsSwitchItem(currentLanguage.auto_update, autoUpdate, { autoUpdate = it }, isProcessing.value == 0)
                    Divider(color = Color(0xFF3A3A3A), thickness = 1.dp)
                    SettingsSwitchItem(currentLanguage.crash_analytics, analyticsEnabled, {
                        analyticsEnabled = it
                        if (analyticsEnabled) {
                            CoroutineScope(Dispatchers.IO).launch {
                                SupabaseManager.logEvent(
                                    "analytics_preference",
                                    mapOf("enabled" to it.toString())
                                )
                            }
                        }
                    }, isProcessing.value == 0)
                    Divider(color = Color(0xFF3A3A3A), thickness = 1.dp)
                    SettingsItem(
                        title = currentLanguage.github_link,
                        onClick = {
                            try {
                                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                    Desktop.getDesktop().browse(URI("https://github.com/dreamcat69GIT/OpenFileEncryptor-Kotlin"))
                                } else {
                                    if (analyticsEnabled) {
                                        SupabaseManager.logEvent("desktop_api_not_supported", mapOf("reason" to "Desktop API is not supported or cannot open browser."))
                                    }
                                }
                            } catch (e: Exception) {
                                if (analyticsEnabled) {
                                    SupabaseManager.logEvent("github_link_open_failed", mapOf("error_message" to (e.message ?: "Unknown error"), "exception_class" to e.javaClass.simpleName))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = isProcessing.value == 0
                    )
                    Divider(color = Color(0xFF3A3A3A), thickness = 1.dp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (isProcessing.value > 0) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = (if (isProcessing.value == 1) encryptionProgress.value else decryptionProgress.value) / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colors.secondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${currentLanguage.log_processing_file}: ${currentProcessingFile.value} (${if (isProcessing.value == 1) encryptionProgress.value else decryptionProgress.value}%)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (errorMessage.value.isNotBlank()) {
            Text(
                text = errorMessage.value,
                color = Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
        if (Message.value.isNotBlank()) {
            Text(
                text = Message.value,
                color = DarkBlueColors.primary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun UpdateNotificationScreen(
    onContinue: () -> Unit
) {
    val currentLanguage = LanguageLoader.getDefaultLanguage()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentLanguage.update_available,
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        ButtonWithInteraction(
            onClick = onContinue,
            text = currentLanguage.continue_button,
            buttonTextStyle = TextStyle(fontSize = 14.sp),
            baseColor = DarkBlueColors.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonWithInteraction(
            onClick = { UpdateChecker.openRepositoryUrl() },
            text = currentLanguage.open_repository,
            buttonTextStyle = TextStyle(fontSize = 14.sp),
            baseColor = DarkBlueColors.primaryVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
fun PrivilegeWarningScreen(
    os: String,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    val currentLanguage = LanguageLoader.getDefaultLanguage()
    val osName = if (os.lowercase().contains("win")) currentLanguage.admin_privileges else currentLanguage.root_privileges
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentLanguage.privilege_warning_text.replace("%s", osName),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        ButtonWithInteraction(
            onClick = onContinue,
            text = currentLanguage.continue_button,
            buttonTextStyle = TextStyle(fontSize = 14.sp),
            baseColor = DarkBlueColors.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonWithInteraction(
            onClick = onExit,
            text = currentLanguage.exit_button,
            buttonTextStyle = TextStyle(fontSize = 14.sp),
            baseColor = Color.Red,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

private fun processDirectory(
    directory: File,
    key: String,
    isEncrypt: Boolean,
    progressCallback: (processed: Int, total: Int) -> Unit,
    updateFileNameCallback: ((String) -> Unit)? = null,
    logErrorCallback: (reason: String, exception: Throwable?) -> Unit
) {
    val files = directory.walkTopDown()
        .filter { it.isFile && (if (isEncrypt) true else it.extension == "encrypted") }
        .toList()
    val totalFiles = files.size
    var processedFiles = 0

    files.forEach { file ->
        updateFileNameCallback?.invoke(file.name)
        try {
            if (isEncrypt) {
                Cryptography.encryptFile(file, key)
            } else {
                Cryptography.decryptFile(file, key)
            }
            processedFiles++
            progressCallback(processedFiles, totalFiles)
        } catch (e: Exception) {
            logErrorCallback("file_processing_failed", e)
        }
    }
}

fun Color.darker(factor: Float): Color = Color(
    red = (this.red * (1f - factor)).coerceIn(0f, 1f),
    green = (this.green * (1f - factor)).coerceIn(0f, 1f),
    blue = (this.blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = this.alpha
)

@Composable
fun MutableInteractionSource.collectIsHoveredAsState(): State<Boolean> {
    val interactions = remember { mutableStateListOf<HoverInteraction>() }
    LaunchedEffect(this) {
        this@collectIsHoveredAsState.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> interactions.add(interaction)
                is HoverInteraction.Exit -> interactions.removeAll { it == interaction.enter }
            }
        }
    }
    return remember { derivedStateOf { interactions.isNotEmpty() } }
}

@Composable
fun MutableInteractionSource.collectIsPressedAsState(): State<Boolean> {
    val interactions = remember { mutableStateListOf<PressInteraction.Press>() }
    LaunchedEffect(this) {
        this@collectIsPressedAsState.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release, is PressInteraction.Cancel -> interactions.clear()
            }
        }
    }
    return remember { derivedStateOf { interactions.isNotEmpty() } }
}

@Composable
fun ButtonWithInteraction(
    onClick: () -> Unit,
    text: String,
    buttonTextStyle: TextStyle,
    baseColor: Color,
    modifier: Modifier = Modifier.height(50.dp),
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> baseColor.darker(0.4f)
            isPressed -> baseColor.darker(0.5f)
            isHovered -> baseColor.darker(0.2f)
            else -> baseColor
        }
    )

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = MaterialTheme.colors.onPrimary,
            disabledBackgroundColor = backgroundColor
        ),
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Text(text, style = buttonTextStyle)
    }
}
@Composable
fun DropdownMenuBox(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.height(50.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = !expanded }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.body1.copy(color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selected,
                    style = MaterialTheme.typography.body1.copy(color = if (enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    painter = rememberVectorPainter(Icons.Default.ArrowDropDown),
                    contentDescription = "Dropdown arrow",
                    tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach {
                DropdownMenuItem(onClick = {
                    onSelected(it)
                    expanded = false
                }) {
                    Text(it, color = MaterialTheme.colors.onSurface)
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .height(50.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1.copy(color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium),
                uncheckedTrackColor = Color.Gray.copy(alpha = ContentAlpha.medium)
            )
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .height(50.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1.copy(color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f)
        )
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OpenFileEncryptor",
        resizable = false,
        state = WindowState(width = 400.dp, height = 500.dp)
    ) {
        var screenState by remember { mutableStateOf("loading") }

        LaunchedEffect(Unit) {
            val isElevated = ElevationChecker.isElevated()
            if (!isElevated) {
                screenState = "privilege"
                return@LaunchedEffect
            }

            val settings = SettingsManager.loadSettings()
            if (settings.autoUpdate) {
                val hasUpdate = withContext(Dispatchers.IO) {
                    UpdateChecker.checkForUpdates()
                }
                screenState = if (hasUpdate) "update" else "app"
            } else {
                screenState = "app"
            }
        }

        MaterialTheme(colors = DarkBlueColors) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                when (screenState) {
                    "loading" -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator()
                        }
                    }
                    "privilege" -> {
                        PrivilegeWarningScreen(
                            os = System.getProperty("os.name"),
                            onContinue = { screenState = "app" },
                            onExit = ::exitApplication
                        )
                    }
                    "update" -> {
                        UpdateNotificationScreen(
                            onContinue = { screenState = "app" }
                        )
                    }
                    "app" -> {
                        OpenFileEncryptorApp()
                    }
                }
            }
        }
    }
}
