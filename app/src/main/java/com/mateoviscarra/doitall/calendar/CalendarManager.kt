package com.mateoviscarra.doitall.calendar

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val TAG = "CalendarManager"

const val CALENDAR_SCOPES = CalendarScopes.CALENDAR_EVENTS

data class CalendarAuthState(
    val isConnected: Boolean,
    val email: String? = null,
    val displayName: String? = null
)

data class CalendarInfo(
    val id: String,
    val summary: String,
    val description: String?,
    val backgroundColor: String?,
    val foregroundColor: String?
)

class CalendarManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "calendar_prefs",
        Context.MODE_PRIVATE
    )

    private var googleSignInClient: GoogleSignInClient? = null
    private var cachedAccount: GoogleSignInAccount? = null
    private var cachedService: Calendar? = null

    companion object {
        private const val KEY_SELECTED_CALENDAR = "selected_calendar_id"
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        googleSignInClient?.let { return it }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CALENDAR_SCOPES))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
        return googleSignInClient!!
    }

    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CALENDAR_SCOPES))
            .build()

        return GoogleSignIn.getClient(activity, gso)
    }

    fun getAuthState(): CalendarAuthState {
        val account = getLastSignedInAccount()
        Log.d(TAG, "getAuthState: account = $account")
        return if (account != null && account.account != null) {
            CalendarAuthState(
                isConnected = true,
                email = account.email,
                displayName = account.displayName
            )
        } else {
            CalendarAuthState(isConnected = false)
        }
    }

    private fun getLastSignedInAccount(): GoogleSignInAccount? {
        cachedAccount?.let { return it }
        cachedAccount = GoogleSignIn.getLastSignedInAccount(context)
        return cachedAccount
    }

    fun isConnected(): Boolean {
        return getAuthState().isConnected
    }

    suspend fun disconnect() = withContext(Dispatchers.Main) {
        val client = getGoogleSignInClient()
        client.signOut()
        cachedAccount = null
        cachedService = null
        prefs.edit().clear().apply()
    }

    fun getSelectedCalendarId(): String {
        return prefs.getString(KEY_SELECTED_CALENDAR, "primary") ?: "primary"
    }

    fun setSelectedCalendarId(calendarId: String) {
        prefs.edit().putString(KEY_SELECTED_CALENDAR, calendarId).apply()
    }

    @Suppress("DEPRECATION")
    private fun requestAuthToken(): String? {
        val account = getLastSignedInAccount() ?: return null
        val email = account.email ?: return null

        val accountManager = AccountManager.get(context)
        val googleAccount = Account(email, "com.google")

        return try {
            val bundle = accountManager.getAuthToken(
                googleAccount,
                CALENDAR_SCOPES,
                false,
                null,
                null
            ).result
            bundle?.getString(AccountManager.KEY_AUTHTOKEN)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getCalendarService(): Calendar? = withContext(Dispatchers.IO) {
        cachedService?.let { return@withContext it }

        val account = getLastSignedInAccount() ?: return@withContext null
        val email = account.email ?: return@withContext null
        val authToken = requestAuthToken() ?: return@withContext null

        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = object : com.google.api.client.http.HttpRequestInitializer {
                override fun initialize(request: com.google.api.client.http.HttpRequest) {
                    request.headers.setAuthorization("Bearer $authToken")
                }
            }

            val service = Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("DoItAll")
                .build()

            cachedService = service
            service
        } catch (e: Exception) {
            null
        }
    }

    fun invalidateServiceCache() {
        cachedService = null
    }

    suspend fun getAvailableCalendars(): Result<List<CalendarInfo>> = withContext(Dispatchers.IO) {
        try {
            invalidateServiceCache()
            val service = getCalendarService() ?: return@withContext Result.failure(Exception("Not signed in"))

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
            invalidateServiceCache()
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
            invalidateServiceCache()
            val service = getCalendarService() ?: return@withContext Result.failure(Exception("Not signed in"))

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
            Result.success(createdEvent.id ?: "")
        } catch (e: Exception) {
            invalidateServiceCache()
            Result.failure(e)
        }
    }

    suspend fun getUpcomingWorkouts(maxResults: Int = 10): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val service = getCalendarService() ?: return@withContext Result.failure(Exception("Not signed in"))

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
            invalidateServiceCache()
            Result.failure(e)
        }
    }
}