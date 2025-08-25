import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import java.util.logging.Logger
import java.util.logging.Level
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Serializable
data class AnalyticsEvent(
    val event_name: String,
    val event_params: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ErrorLog(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object SupabaseManager {
    private val logger = Logger.getLogger("SupabaseManager")
    private const val SUPABASE_URL = "https://lxsmrzoytrsaggfzmfno.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx4c21yem95dHJzYWdnZnptZm5vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTEwMzA3NjAsImV4cCI6MjA2NjYwNjc2MH0.I-vMEDPP_9Y0PAgCuB_SONQIGVngNc1O7wCESTBmG1I"
    private var supabaseClient: SupabaseClient? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize() {
        if (SUPABASE_URL.isBlank() || SUPABASE_ANON_KEY.isBlank()) {
            logger.log(Level.SEVERE, "Supabase credentials are not set. Logging will be disabled.")
            isInitialized = false
            return
        }

        supabaseClient = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }

        isInitialized = true
        logger.log(Level.INFO, "Supabase Manager initialized. Ready for logging.")
    }

    fun logEvent(name: String, params: Map<String, Any>? = null) {
        if (!isInitialized || supabaseClient == null) {
            logger.log(Level.WARNING, "Supabase Manager not initialized. Cannot log event: $name")
            return
        }

        scope.launch {
            try {
                val stringParams = params?.mapValues { it.value.toString() }
                val event = AnalyticsEvent(event_name = name, event_params = stringParams)

                supabaseClient!!.postgrest["analytics_events"].insert(event)
                logger.log(Level.INFO, "Logged Supabase Analytics event: $name with params: $params")
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to log event to Supabase: ${e.message}")
            }
        }
    }

}