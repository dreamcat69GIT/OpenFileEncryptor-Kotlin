import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.swing.JOptionPane
import java.awt.Desktop
import java.io.File
import java.io.IOException

object ElevationChecker {
    fun isElevated(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> isWindowsAdmin()
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> isLinuxRoot()
            else -> false
        }
    }

    private fun isWindowsAdmin(): Boolean {
        return try {
            val process = ProcessBuilder("cmd", "/c", "net session").start()
            process.waitFor(5, TimeUnit.SECONDS)
            process.exitValue() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isLinuxRoot(): Boolean {
        return try {
            val process = ProcessBuilder("id", "-u").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            process.waitFor(5, TimeUnit.SECONDS)
            output == "0"
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}