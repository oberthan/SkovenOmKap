package com.example.skovenomkap

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

object FirebaseHelper {
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    suspend fun getUsers(): QuerySnapshot? {
        try {
            return db.collection("users").get().await()
        } catch (e: Exception) {
            println("Error getting all users: $e")
            throw e
        }
    }
}