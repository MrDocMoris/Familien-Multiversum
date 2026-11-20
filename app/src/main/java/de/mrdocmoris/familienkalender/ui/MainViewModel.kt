package de.mrdocmoris.familienkalender.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.FirebaseApp
import androidx.lifecycle.viewModelScope
import de.mrdocmoris.familienkalender.data.CalendarEvent
import de.mrdocmoris.familienkalender.data.Family
import de.mrdocmoris.familienkalender.data.LocalEventStore
import de.mrdocmoris.familienkalender.data.Repository
import de.mrdocmoris.familienkalender.data.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

sealed interface AppState {
    data object Loading : AppState
    data object LoggedOut : AppState
    data class NeedsFamily(val uid: String) : AppState
    data class Ready(val uid: String, val profile: UserProfile, val familyId: String) : AppState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    /** Offline-Modus, wenn keine google-services.json eingebunden ist (Firebase nicht initialisiert). */
    val offline: Boolean = FirebaseApp.getApps(app).isEmpty()

    // lazy: im Offline-Modus darf Firebase nie angefasst werden
    private val repo by lazy { Repository() }
    private val local by lazy { LocalEventStore(app) }

    // Zuerst deklarieren: die Flows unten greifen beim Start darauf zu
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    val state: StateFlow<AppState> = (
        if (offline) flowOf(AppState.Ready("local", UserProfile(displayName = "Ich"), "local"))
        else repo.authState().flatMapLatest { user ->
            if (user == null) flowOf(AppState.LoggedOut)
            else repo.userProfile(user.uid).map { p ->
                val fid = p.familyId
                if (fid.isNullOrBlank()) AppState.NeedsFamily(user.uid)
                else AppState.Ready(user.uid, p, fid)
            }.catch { e -> error.value = e.message; emit(AppState.NeedsFamily(user.uid)) }
        }
        ).stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    private val familyId = state.map { (it as? AppState.Ready)?.familyId }.distinctUntilChanged()

    val family: StateFlow<Family?> = familyId
        .flatMapLatest { fid -> if (fid == null || offline) flowOf(null) else repo.family(fid) }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val month = MutableStateFlow(YearMonth.now())
    val selectedDate = MutableStateFlow(LocalDate.now())

    val events: StateFlow<List<CalendarEvent>> = combine(familyId, month) { fid, m -> fid to m }
        .flatMapLatest { (fid, m) ->
            if (fid == null) flowOf(emptyList())
            else if (offline) local.events
            else repo.eventsForMonth(fid, m).catch { e -> error.value = e.message; emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- Aktionen ----------

    fun signIn(email: String, pw: String) = launchAction { repo.signIn(email.trim(), pw) }
    fun register(email: String, pw: String, name: String) = launchAction { repo.register(email.trim(), pw, name.trim()) }
    fun signOut() = repo.signOut()

    fun createFamily(uid: String, name: String) = launchAction { repo.createFamily(uid, name.trim()) }
    fun joinFamily(uid: String, code: String) = launchAction { repo.joinFamily(uid, code) }

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        month.value = YearMonth.from(date)
    }

    fun saveEvent(event: CalendarEvent) {
        val s = state.value as? AppState.Ready ?: return
        if (offline) {
            if (event.id.isBlank()) local.add(event.copy(createdByName = s.profile.displayName))
            else local.update(event)
            return
        }
        launchAction {
            if (event.id.isBlank()) {
                repo.addEvent(
                    s.familyId,
                    event.copy(createdBy = s.uid, createdByName = s.profile.displayName)
                )
            } else {
                repo.updateEvent(s.familyId, event)
            }
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        val s = state.value as? AppState.Ready ?: return
        if (offline) { local.delete(event.id); return }
        launchAction { repo.deleteEvent(s.familyId, event.id) }
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try {
                block()
            } catch (e: Exception) {
                error.value = e.localizedMessage ?: "Unbekannter Fehler"
            } finally {
                busy.value = false
            }
        }
    }
}
