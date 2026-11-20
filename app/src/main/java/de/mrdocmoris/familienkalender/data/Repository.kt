package de.mrdocmoris.familienkalender.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.YearMonth

/**
 * Datenstruktur in Firestore:
 *   users/{uid}                         -> UserProfile
 *   families/{familyId}                 -> Family
 *   families/{familyId}/events/{id}     -> CalendarEvent
 *   invites/{code}                      -> { familyId }
 */
class Repository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    val currentUser: FirebaseUser? get() = auth.currentUser

    // ---------- Login ----------

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(email: String, password: String, name: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        db.collection("users").document(uid)
            .set(mapOf("displayName" to name), SetOptions.merge()).await()
    }

    fun signOut() = auth.signOut()

    // ---------- Profil & Familie ----------

    fun userProfile(uid: String): Flow<UserProfile> = callbackFlow {
        val reg = db.collection("users").document(uid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(UserProfile::class.java) ?: UserProfile())
        }
        awaitClose { reg.remove() }
    }

    fun family(familyId: String): Flow<Family?> = callbackFlow {
        val reg = db.collection("families").document(familyId).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(Family::class.java))
        }
        awaitClose { reg.remove() }
    }

    /** Legt eine neue Familie an und gibt den Einladungscode zurück. */
    suspend fun createFamily(uid: String, name: String): String {
        val code = generateCode()
        val familyRef = db.collection("families").document()
        db.runBatch { b ->
            b.set(familyRef, mapOf("name" to name, "members" to listOf(uid), "inviteCode" to code))
            b.set(db.collection("invites").document(code), mapOf("familyId" to familyRef.id))
            b.set(db.collection("users").document(uid), mapOf("familyId" to familyRef.id), SetOptions.merge())
        }.await()
        return code
    }

    /** Tritt einer Familie per Einladungscode bei. */
    suspend fun joinFamily(uid: String, code: String) {
        val invite = db.collection("invites").document(code.trim().uppercase()).get().await()
        val familyId = invite.getString("familyId")
            ?: throw IllegalArgumentException("Einladungscode nicht gefunden")
        db.collection("families").document(familyId)
            .update("members", FieldValue.arrayUnion(uid)).await()
        db.collection("users").document(uid)
            .set(mapOf("familyId" to familyId), SetOptions.merge()).await()
    }

    // ---------- Termine ----------

    /**
     * Live-Termine für einen Monat. Lädt ab einem Monat vorher, damit auch
     * Termine auftauchen, die vor dem Monat beginnen und hineinreichen.
     */
    fun eventsForMonth(familyId: String, month: YearMonth): Flow<List<CalendarEvent>> = callbackFlow {
        val from = month.minusMonths(1).atDay(1).atStartOfDay().toTimestamp()
        val to = month.plusMonths(1).atDay(1).atStartOfDay().toTimestamp()
        val reg = events(familyId)
            .whereGreaterThanOrEqualTo("start", from)
            .whereLessThan("start", to)
            .orderBy("start")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(CalendarEvent::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun addEvent(familyId: String, event: CalendarEvent) {
        events(familyId).add(event).await()
    }

    suspend fun updateEvent(familyId: String, event: CalendarEvent) {
        events(familyId).document(event.id).set(event).await()
    }

    suspend fun deleteEvent(familyId: String, eventId: String) {
        events(familyId).document(eventId).delete().await()
    }

    private fun events(familyId: String) =
        db.collection("families").document(familyId).collection("events")

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // ohne 0/O und 1/I
        return (1..6).map { chars.random() }.joinToString("")
    }
}
