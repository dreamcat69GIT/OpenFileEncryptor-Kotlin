import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Desktop
import kotlin.math.max

@Serializable
private data class GithubRelease(
    val tag_name: String,
    val html_url: String
)

object UpdateChecker {
    private const val githubRepoApiUrl = "https://api.github.com/repos/dreamcat69GIT/OpenFileEncryptor-Kotlin/releases/latest"
    private const val browserRepoUrl = "https://github.com/dreamcat69GIT/OpenFileEncryptor-Kotlin/releases"
    private val CURRENT_VERSION: String = "1.0.0"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(): Boolean {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI(githubRepoApiUrl))
            .GET()
            .build()

        return try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val release = json.decodeFromString<GithubRelease>(response.body())
                val latestVersion = release.tag_name
                val isNewer = compareVersions(latestVersion, CURRENT_VERSION)
                isNewer
            } else {
                println("Failed to fetch update info. HTTP Status: ${response.statusCode()}")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    private fun compareVersions(latest: String, current: String): Boolean {
        val latestParts = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        System.out.println(latest)
        val maxLen = max(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        val latestIsPrerelease = latest.contains("-")
        val currentIsPrerelease = current.contains("-")
        return latestIsPrerelease != currentIsPrerelease && !latestIsPrerelease
    }

    fun openRepositoryUrl() {
        try {
            Desktop.getDesktop().browse(URI(browserRepoUrl))
        } catch (e: Exception) { }
    }
}