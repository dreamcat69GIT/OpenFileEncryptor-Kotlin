import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.JarURLConnection

@Serializable
data class Language(
    val language: String,
    val lang: String,
    val encryption_tab: String,
    val decryption_tab: String,
    val settings_tab: String,
    val generate_key: String,
    val browse: String,
    val path_to_directory: String,
    val paste: String,
    val enter_encryption_key: String,
    val enter_decryption_key: String,
    val encrypt: String,
    val decrypt: String,
    val save_logs: String,
    val auto_update: String,
    val crash_analytics: String,
    val decrypt_error: String,
    val encrypt_error: String,
    val log_operation_in_progress: String,
    val log_encryption_started: String,
    val log_key_generated: String,
    val log_save_key_warning: String,
    val log_key_copied: String,
    val log_encrypting_file: String,
    val log_file_encrypted_success: String,
    val log_encrypting_directory: String,
    val log_directory_encrypted_success: String,
    val log_encryption_finished: String,
    val log_decryption_started: String,
    val log_decrypting_file: String,
    val log_file_decrypted_success: String,
    val log_decryption_directory: String,
    val log_directory_decrypted_success: String,
    val log_decryption_finished: String,
    val log_mode_changed: String,
    val log_processing_file: String,
    val save_key_to_file: String,
    val work_mode: String,
    val mode_file: String,
    val mode_directory: String,
    val privilege_warning_text: String,
    val continue_button: String,
    val exit_button: String,
    val github_link: String,
    val log_error_no_key_generated: String,
    val log_key_saved_success: String,
    val log_error_no_encryption_path: String,
    val log_error_no_encryption_key: String,
    val log_error_encryption_failed: String,
    val log_error_invalid_file_selected: String,
    val log_error_invalid_directory_selected: String,
    val log_error_path_not_found: String,
    val log_error_no_decryption_path: String,
    val log_error_no_decryption_key: String,
    val log_error_not_encrypted_file: String,
    val log_error_decryption_invalid_key: String,
    val log_error_decryption_invalid_key_tag: String,
    val log_error_decryption_argument: String,
    val log_error_decryption_failed: String,
    val encrypted_files_filter: String,
    val admin_privileges: String,
    val log_save_key_failed: String,
    val update_available: String,
    val open_repository: String,
    val root_privileges: String
)

object LanguageLoader {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val languagesMap = mutableMapOf<String, Language>()
    private var defaultLanguage: Language? = null

    init {
        defaultLanguage = createDefaultLanguage()
    }

    fun loadLanguages(directoryPath: String = "languages") {
        try {
            val classLoader = Thread.currentThread().contextClassLoader
            val url = classLoader.getResource(directoryPath)

            if (url != null) {
                when (url.protocol) {
                    "file" -> {
                        File(url.toURI()).walk().filter { it.extension == "json" }.forEach { file ->
                            parseAndStoreLanguage(file.readText(), file.name)
                        }
                    }
                    "jar" -> {
                        val jarConnection = url.openConnection() as JarURLConnection
                        val jarFile = jarConnection.jarFile
                        jarFile.entries().asSequence().filter { it.name.startsWith("$directoryPath/") && it.name.endsWith(".json") }.forEach { entry ->
                            jarFile.getInputStream(entry).bufferedReader().use { reader ->
                                parseAndStoreLanguage(reader.readText(), entry.name)
                            }
                        }
                    }
                    else -> {
                        System.err.println("Language directory not found")
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error loading languages from directory: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun parseAndStoreLanguage(jsonString: String, fileName: String) {
        try {
            val language = json.decodeFromString<Language>(jsonString)
            languagesMap[language.language] = language
            if (language.language == "English") {
                defaultLanguage = language
            }
        } catch (e: Exception) {
            System.err.println("Error parsing language content from file: $fileName: ${e.message}")
        }
    }

    fun getLanguage(name: String): Language? {
        return languagesMap[name]
    }

    fun getAvailableLanguages(): List<String> {
        return languagesMap.keys.toList().sorted()
    }

    fun getDefaultLanguage(): Language {
        return defaultLanguage ?: createDefaultLanguage()
    }

    private fun createDefaultLanguage(): Language {
        return Language(
            language = "English",
            lang = "Language",
            encryption_tab = "Encryption",
            decryption_tab = "Decryption",
            settings_tab = "Settings",
            generate_key = "Generate a key?",
            browse = "Browse",
            path_to_directory = "Path to Directory",
            paste = "Paste",
            enter_encryption_key = "Enter key",
            enter_decryption_key = "Enter key",
            encrypt = "Encrypt",
            decrypt = "Decrypt",
            save_logs = "Save Logs?",
            auto_update = "Checking for updates",
            crash_analytics = "Crash Analytics",
            decrypt_error = "Error while decrypting",
            encrypt_error = "Error while encrypting",
            log_operation_in_progress = "Operation is already in progress. Please wait for it to finish.",
            log_encryption_started = "Encryption started...",
            log_key_generated = "Generated Key",
            log_save_key_warning = "Please copy this key and save it securely for decryption!",
            log_key_copied = "Generated key copied to clipboard.",
            log_encrypting_file = "Encrypting file",
            log_file_encrypted_success = "File %s encrypted successfully.",
            log_encrypting_directory = "Encrypting directory",
            log_directory_encrypted_success = "Directory %s encrypted successfully.",
            log_encryption_finished = "Encryption finished.",
            log_decryption_started = "Decryption started...",
            log_decrypting_file = "Decrypting file",
            log_file_decrypted_success = "File %s decrypted successfully.",
            log_decryption_directory = "Decrypting directory",
            log_directory_decrypted_success = "Directory %s decrypted successfully.",
            log_decryption_finished = "Decryption finished.",
            log_mode_changed = "Work mode changed to",
            log_processing_file = "Processing",
            save_key_to_file = "Save Key",
            work_mode = "Work Mode",
            mode_file = "File",
            mode_directory = "Directory",
            privilege_warning_text = "For the application to work correctly, it may need to be run with elevated privileges (%s).",
            continue_button = "Continue",
            exit_button = "Exit",
            github_link = "Github",
            log_error_no_key_generated = "Error: Please generate a key first by clicking 'Encrypt'.",
            log_key_saved_success = "Key successfully saved to file: %s",
            log_error_no_encryption_path = "Error: Please select a file or folder for encryption.",
            log_error_no_encryption_key = "Error: Please enter an encryption key.",
            log_error_encryption_failed = "Encryption error: %s",
            log_error_invalid_file_selected = "Error: The selected path is not a file. Please select a file or switch to directory mode.",
            log_error_invalid_directory_selected = "Error: The selected path is not a directory. Please select a directory or switch to file mode.",
            log_error_path_not_found = "Error: The specified path does not exist: %s",
            log_error_no_decryption_path = "Error: Please select a file or folder for decryption.",
            log_error_no_decryption_key = "Error: Please enter a decryption key.",
            log_error_not_encrypted_file = "Error: The selected file is not an encrypted file.",
            log_error_decryption_invalid_key = "Decryption error: Invalid key or corrupted file.",
            log_error_decryption_invalid_key_tag = "Decryption error: Invalid key or corrupted file.",
            log_error_decryption_argument = "Decryption error: Invalid key format or algorithm: %s",
            log_error_decryption_failed = "Decryption error: %s",
            encrypted_files_filter = "Encrypted Files (*.encrypted)",
            admin_privileges = "administrator",
            root_privileges = "root",
            update_available = "Update available",
            open_repository = "Open repository",
            log_save_key_failed = "Failed to save the key"
        )
    }
}