package com.mateoviscarra.doitall.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

const val CALENDAR_REDIRECT_URI = "com.mateoviscarra.doitall:/oauth2callback"
const val CALENDAR_SCOPES = "https://www.googleapis.com/auth/calendar.events"

data class CalendarCredentials(
    val clientId: String,
    val clientSecret: String
)

data class CalendarAuthState(
    val isConnected: Boolean,
    val email: String? = null,
    val expiresAt: Long? = null
)

class CalendarManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "calendar_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "calendar_prefs",
        Context.MODE_PRIVATE
    )

    private var calendarService: Calendar? = null

    companion object {
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_EMAIL = "user_email"
    }

    fun saveCredentials(credentials: CalendarCredentials) {
        securePrefs.edit()
            .putString(KEY_CLIENT_ID, credentials.clientId)
            .putString(KEY_CLIENT_SECRET, credentials.clientSecret)
            .apply()
    }

    fun hasCredentials(): Boolean {
        return securePrefs.getString(KEY_CLIENT_ID, null) != null &&
                securePrefs.getString(KEY_CLIENT_SECRET, null) != null
    }

    fun getAuthUrl(): String {
        val clientId = securePrefs.getString(KEY_CLIENT_ID, null)
            ?: throw IllegalStateException("Client ID not configured")

        val flow = createFlow(clientId, "")
        return flow.newAuthorizationUrl()
            .setRedirectUri(CALENDAR_REDIRECT_URI)
            .build()
    }

    fun exchangeCodeForTokens(authCode: String) {
        val clientId = securePrefs.getString(KEY_CLIENT_ID, null)
            ?: throw IllegalStateException("Client ID not configured")
        val clientSecret = securePrefs.getString(KEY_CLIENT_SECRET, null)
            ?: throw IllegalStateException("Client secret not configured")

        val flow = createFlow(clientId, clientSecret)
        val response = flow.newTokenRequest(authCode)
            .setRedirectUri(CALENDAR_REDIRECT_URI)
            .execute()

        val expiryTime = System.currentTimeMillis() + (response.expiresInSeconds ?: 3600) * 1000

        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime)
            .apply()

        calendarService = createCalendarService()
    }

    fun getAuthState(): CalendarAuthState {
        val accessToken = securePrefs.getString(KEY_ACCESS_TOKEN, null)
        val hasCreds = hasCredentials()

        return CalendarAuthState(
            isConnected = hasCreds && accessToken != null,
            email = securePrefs.getString(KEY_EMAIL, null),
            expiresAt = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0).takeIf { it > 0 }
        )
    }

    fun isConnected(): Boolean {
        return hasCredentials() && securePrefs.getString(KEY_ACCESS_TOKEN, null) != null
    }

    fun disconnect() {
        securePrefs.edit().clear().apply()
        prefs.edit().clear().apply()
        calendarService = null
    }

    private fun createFlow(clientId: String, clientSecret: String): GoogleAuthorizationCodeFlow {
        val clientSecrets = GoogleClientSecrets.load(
            GsonFactory.getDefaultInstance(),
            StringReader("""
                {
                    "web": {
                        "client_id": "$clientId",
                        "client_secret": "$clientSecret"
                    }
                }
            """.trimIndent())
        )

        return GoogleAuthorizationCodeFlow.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            clientSecrets,
            listOf(CALENDAR_SCOPES)
        ).setAccessType("offline").setApprovalPrompt("force").build()
    }

    private fun createCalendarService(): Calendar {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Calendar.Builder(transport, jsonFactory) { request ->
            val accessToken = securePrefs.getString(KEY_ACCESS_TOKEN, null)
            if (accessToken != null) {
                request.headers.set("Authorization", "Bearer $accessToken")
            }
        }.setApplicationName("DoItAll").build()
    }

    suspend fun createWorkoutEvent(
        title: String,
        date: LocalDate,
        startHour: Int = 9,
        startMinute: Int = 0,
        durationMinutes: Int = 60,
        description: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: createCalendarService().also { calendarService = it }

            val startDateTime = LocalDateTime.of(date.year, date.month, date.dayOfMonth, startHour, startMinute)
            val endDateTime = startDateTime.plusMinutes(durationMinutes.toLong())

            val zoneId = ZoneId.systemDefault()

            val event = Event()
                .setSummary(title)
                .setDescription(description ?: "Workout scheduled from DoItAll")

            val start = EventDateTime()
                .setDateTime(com.google.api.client.util.DateTime(startDateTime.atZone(zoneId).toInstant().toString()))
                .setTimeZone(zoneId.id)
            event.start = start

            val end = EventDateTime()
                .setDateTime(com.google.api.client.util.DateTime(endDateTime.atZone(zoneId).toInstant().toString()))
                .setTimeZone(zoneId.id)
            event.end = end

            val reminders = EventReminder()
                .setMethod("popup")
                .setMinutes(30)
            event.reminders = com.google.api.services.calendar.model.Event.Reminders()
                .setUseDefault(false)
                .setOverrides(listOf(reminders))

            val calendarId = "primary"
            val createdEvent = service.events().insert(calendarId, event).execute()

            Result.success(createdEvent.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpcomingWorkouts(maxResults: Int = 10): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: createCalendarService().also { calendarService = it }

            val now = com.google.api.client.util.DateTime(System.currentTimeMillis())
            val events = service.events().list("primary")
                .setMaxResults(maxResults)
                .setTimeMin(now)
                .setQ("Workout")
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            Result.success(events.items ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
