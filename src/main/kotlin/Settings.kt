import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException


@Serializable
data class AppSettings(
    val saveLogs: Boolean = false,
    val autoUpdate: Boolean = false,
    val crashAnalytics: Boolean = false,
    val isFileMode: Boolean = true,
    val selectedLanguage: String = "English"
)

object SettingsManager {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val settingsFile = File("settings.json")

    fun loadSettings(): AppSettings {
        return try {
            if (settingsFile.exists()) {
                val jsonString = settingsFile.readText()
                json.decodeFromString<AppSettings>(jsonString)
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            System.err.println("Error loading settings: ${e.message}")
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        try {
            val jsonString = json.encodeToString(AppSettings.serializer(), settings)
            settingsFile.writeText(jsonString)
        } catch (e: Exception) {
            System.err.println("Error saving settings: ${e.message}")
        }
    }
}
