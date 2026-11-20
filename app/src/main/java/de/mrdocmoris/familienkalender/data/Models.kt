package de.mrdocmoris.familienkalender.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/** users/{uid} */
data class UserProfile(
    val displayName: String = "",
    val familyId: String? = null,
)

/** families/{familyId} */
data class Family(
    @DocumentId val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),
    val inviteCode: String = "",
)

/** families/{familyId}/events/{eventId} */
data class CalendarEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val note: String = "",
    val start: Timestamp = Timestamp.now(),
    val end: Timestamp = Timestamp.now(),
    val allDay: Boolean = false,
    val createdBy: String = "",
    val createdByName: String = "",
) {
    // @get:Exclude: sonst würde Firestore diese Hilfsfelder mitspeichern
    @get:Exclude val startLocal: LocalDateTime get() = start.toLocal()
    @get:Exclude val endLocal: LocalDateTime get() = end.toLocal()

    /** true, wenn der Termin (ganz oder teilweise) an diesem Tag liegt */
    fun isOn(date: LocalDate): Boolean =
        !startLocal.toLocalDate().isAfter(date) && !endLocal.toLocalDate().isBefore(date)
}

fun Timestamp.toLocal(): LocalDateTime =
    LocalDateTime.ofInstant(toDate().toInstant(), ZoneId.systemDefault())

fun LocalDateTime.toTimestamp(): Timestamp =
    Timestamp(Date.from(atZone(ZoneId.systemDefault()).toInstant()))
