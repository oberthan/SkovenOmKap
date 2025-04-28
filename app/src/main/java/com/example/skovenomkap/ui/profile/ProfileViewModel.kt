package com.example.skovenomkap.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class ProfileViewModel : ViewModel() {

    private val _plants = MutableLiveData<List<Plant>>()
    val plants: LiveData<List<Plant>> = _plants

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadUserPlants()
    }

    private fun loadUserPlants() {
        val userId = auth.currentUser?.uid ?: return // Get current user's UID

        db.collection("users")
            .document(userId)
            .collection("plants")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val plantList = mutableListOf<Plant>()
                for (document in querySnapshot.documents) {
                    val plant = document.toObject(Plant::class.java)?.copy(name = document.id)
                    plant?.let { plantList.add(it) }
                }
                _plants.value = plantList
            }
            .addOnFailureListener { exception ->
                // Handle the error (e.g., log it, display a message)
                println("Error getting plants: ${exception.message}")
            }
    }
}
