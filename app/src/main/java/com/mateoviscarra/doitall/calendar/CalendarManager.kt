package com.mateoviscarra.doitall.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.CalendarList
import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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

data class CalendarInfo(
    val id: String,
    val summary: String,
    val description: String?,
    val backgroundColor: String?,
    val foregroundColor: String?
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

    companion object {
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_SELECTED_CALENDAR = "selected_calendar_id"
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

    fun getAuthorizationUrl(): String {
        val clientId = securePrefs.getString(KEY_CLIENT_ID, null)
            ?: throw IllegalStateException("Client ID not configured")

        return "https://accounts.google.com/o/oauth2/v2/auth?" +
               "client_id=${java.net.URLEncoder.encode(clientId, "UTF-8")}" +
               "&response_type=code" +
               "&scope=${java.net.URLEncoder.encode(CALENDAR_SCOPES, "UTF-8")}" +
               "&redirect_uri=http://localhost:8080" +
               "&access_type=offline" +
               "&prompt=consent"
    }

    suspend fun exchangeCodeForTokens(authCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val clientId = securePrefs.getString(KEY_CLIENT_ID, null)
                ?: return@withContext Result.failure(Exception("Client ID not configured"))
            val clientSecret = securePrefs.getString(KEY_CLIENT_SECRET, null)
                ?: return@withContext Result.failure(Exception("Client secret not configured"))

            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val request = GoogleAuthorizationCodeTokenRequest(
                transport,
                jsonFactory,
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                authCode,
                "http://localhost:8080"
            )

            val response = request.execute()

            val expiryTime = System.currentTimeMillis() + (response.expiresInSeconds ?: 3600) * 1000

            securePrefs.edit()
                .putString(KEY_ACCESS_TOKEN, response.accessToken)
                .putString(KEY_REFRESH_TOKEN, response.refreshToken)
                .putLong(KEY_TOKEN_EXPIRY, expiryTime)
                .apply()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createRequestInitializer(): HttpRequestInitializer {
        return HttpRequestInitializer { request ->
            val accessToken = securePrefs.getString(KEY_ACCESS_TOKEN, null) ?: ""
            if (accessToken.isNotEmpty()) {
                request.headers.set("Authorization", "Bearer $accessToken")
            }
        }
    }

    private fun getFreshCalendarService(): Calendar {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Calendar.Builder(transport, jsonFactory, createRequestInitializer())
            .setApplicationName("DoItAll")
            .build()
    }

    private suspend fun ensureValidToken(): Result<Unit> {
        val expiryTime = securePrefs.getLong(KEY_TOKEN_EXPIRY, 0)
        val refreshToken = securePrefs.getString(KEY_REFRESH_TOKEN, null) ?: ""

        if (refreshToken.isEmpty()) {
            return Result.failure(Exception("No refresh token available"))
        }

        if (System.currentTimeMillis() < expiryTime - 300000) {
            return Result.success(Unit)
        }

        return try {
            val clientId = securePrefs.getString(KEY_CLIENT_ID, null)
                ?: return Result.failure(Exception("Client ID not configured"))
            val clientSecret = securePrefs.getString(KEY_CLIENT_SECRET, null)
                ?: return Result.failure(Exception("Client secret not configured"))

            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val request = com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest(
                transport,
                jsonFactory,
                clientId,
                clientSecret,
                refreshToken
            )

            val response = request.execute()

            val newExpiryTime = System.currentTimeMillis() + (response.expiresInSeconds ?: 3600) * 1000

            securePrefs.edit()
                .putString(KEY_ACCESS_TOKEN, response.accessToken)
                .putLong(KEY_TOKEN_EXPIRY, newExpiryTime)
                .apply()

            Result.success(Unit)
        } catch (e: Exception) {
            if (e.message?.contains("invalid_grant", ignoreCase = true) == true ||
                e.message?.contains("token expired", ignoreCase = true) == true) {
                securePrefs.edit()
                    .remove(KEY_ACCESS_TOKEN)
                    .remove(KEY_REFRESH_TOKEN)
                    .remove(KEY_TOKEN_EXPIRY)
                    .apply()
            }
            Result.failure(e)
        }
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
    }

    fun getSelectedCalendarId(): String {
        return prefs.getString(KEY_SELECTED_CALENDAR, "primary") ?: "primary"
    }

    fun setSelectedCalendarId(calendarId: String) {
        prefs.edit().putString(KEY_SELECTED_CALENDAR, calendarId).apply()
    }

    suspend fun getAvailableCalendars(): Result<List<CalendarInfo>> = withContext(Dispatchers.IO) {
        try {
            ensureValidToken().getOrThrow()

            val service = getFreshCalendarService()

            val calendarList = service.calendarList().list()
                .setMaxResults(100)
                .setShowHidden(false)
                .execute()

            val calendars = calendarList.items?.map { entry ->
                CalendarInfo(
                    id = entry.id ?: "",
                    summary = entry.summary ?: "Unknown",
                    description = entry.description,
                    backgroundColor = entry.backgroundColor,
                    foregroundColor = entry.foregroundColor
                )
            } ?: emptyList()

            Result.success(calendars)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            ensureValidToken().getOrThrow()

            val service = getFreshCalendarService()

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

            val calendarId = getSelectedCalendarId()
            val createdEvent = service.events().insert(calendarId, event).execute()

            Result.success(createdEvent.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpcomingWorkouts(maxResults: Int = 10): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            ensureValidToken().getOrThrow()

            val service = getFreshCalendarService()

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
