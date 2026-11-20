package de.mrdocmoris.familienkalender.data

import android.content.Context
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

/**
 * Offline-Modus: speichert Termine nur lokal auf dem Gerät (SharedPreferences).
 * Wird benutzt, wenn keine google-services.json eingebunden ist.
 */
class LocalEventStore(context: Context) {
    private val prefs = context.getSharedPreferences("local_events", Context.MODE_PRIVATE)
    private val _events = MutableStateFlow(load())
    val events: StateFlow<List<CalendarEvent>> = _events

    fun add(event: CalendarEvent) = save(_events.value + event.copy(id = UUID.randomUUID().toString()))
    fun update(event: CalendarEvent) = save(_events.value.map { if (it.id == event.id) event else it })
    fun delete(id: String) = save(_events.value.filterNot { it.id == id })

    private fun save(list: List<CalendarEvent>) {
        val sorted = list.sortedBy { it.start.toDate().time }
        _events.value = sorted
        val arr = JSONArray()
        sorted.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("note", e.note)
                    .put("start", e.start.toDate().time)
                    .put("end", e.end.toDate().time)
                    .put("allDay", e.allDay)
                    .put("createdByName", e.createdByName)
            )
        }
        prefs.edit().putString("events", arr.toString()).apply()
    }

    private fun load(): List<CalendarEvent> {
        val raw = prefs.getString("events", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CalendarEvent(
                    id = o.getString("id"),
                    title = o.optString("title"),
                    note = o.optString("note"),
                    start = Timestamp(Date(o.getLong("start"))),
                    end = Timestamp(Date(o.getLong("end"))),
                    allDay = o.optBoolean("allDay"),
                    createdByName = o.optString("createdByName"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
