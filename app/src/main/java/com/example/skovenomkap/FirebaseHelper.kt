package com.example.skovenomkap

import com.google.android.gms.tasks.Task
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
}