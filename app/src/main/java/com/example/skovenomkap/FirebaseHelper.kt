package com.example.skovenomkap

import com.example.skovenomkap.ui.home.Udfordrings
import com.example.skovenomkap.ui.profile.Plant
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar

object FirebaseHelper {
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val auth: FirebaseAuth
        get() = Firebase.auth

    fun getUsers(): Task<QuerySnapshot> {
        try {
            val users = db.collection("users").get()
            return users
        } catch (e: Exception) {
            println("Error getting all users: $e")
            throw e
        }
    }

    fun updatePlant(plantName: String) {
        try {
            val updates = hashMapOf<String, Any>(
                "amount" to FieldValue.increment(1),
                "last-seen" to Calendar.getInstance().time
            )
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .collection("plants")
                .document(plantName)
                .set(updates, SetOptions.merge())


        } catch (e: Exception) {
            println("Error getting all users: $e")
            throw e
        }
    }
    fun getPlant(uid: String) : Task<QuerySnapshot> {
        try {
            val plants = db.collection("users").document(uid).collection("plants").get()
            return plants

        } catch (e: Exception) {
            throw e
        }
    }
    fun getPlant() : Task<QuerySnapshot> {
        return getPlant(auth.currentUser!!.uid)

    }

    suspend fun leading(challenge: Udfordrings): Map.Entry<String, Int>? {

        // 4) pick the max—or null if empty
        return ranking(challenge).maxByOrNull { it.value }
    }
    suspend fun ranking(challenge: Udfordrings) : Map<String, Int> {
        // 1) collect the Tasks but do NOT attach any listeners
        val tasks: List<Task<QuerySnapshot>> = challenge.participants.map { uid ->
            getPlant(uid)    // already returns Task<QuerySnapshot>
        }

        // 2) await until ALL of them complete successfully
        //    Tasks.whenAllSuccess(...) itself returns a Task<List<QuerySnapshot>>
        val snapshots: List<QuerySnapshot> =
            Tasks.whenAllSuccess<QuerySnapshot>(tasks).await()

        // 3) zip UIDs ↔ snapshots, count each user’s “recent” plants
        val counts: Map<String, Int> = challenge.participants
            .zip(snapshots)
            .associate { (uid, snap) ->
                val cnt = snap.documents
                    .mapNotNull { doc ->
                        doc.toObject(Plant::class.java)?.also {
                            it.date = doc.getDate("last-seen")
                        }
                    }
                    .count { it.date!! > challenge.timestamp }
                uid to cnt
            }
        return counts
    }
}