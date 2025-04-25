package com.example.skovenomkap.ui.friends

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skovenomkap.databinding.FragmentFriendsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class User(val uid: String, val username: String)

class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersList = mutableListOf<User>()
    private lateinit var userAdapter: UserAdapter
    private var allUsers: List<User> = emptyList()
    private var dataLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAdapter = UserAdapter(usersList)
        binding.recyclerViewFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        binding.addFriendButton.setOnClickListener {
            showAddFriendDialog()
        }

        if (!dataLoaded) {
            lifecycleScope.launch {
                fetchAllUsers()
            }
        } else {
            filterAndDisplayFriends()
        }
    }

    private fun showAddFriendDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Friend")

        val input = EditText(requireContext())
        input.hint = "Enter username"
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val username = input.text.toString().trim()
            if (username.isNotEmpty()) {
                lifecycleScope.launch {
                    addFriend(username)
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private suspend fun addFriend(username: String) {
        val currentUserUid = auth.currentUser?.uid ?: return

        try {
            // Find the user with the given username from the local list
            val friend = allUsers.find { it.username == username }

            if (friend == null) {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                return
            }

            val friendUid = friend.uid
            if (friendUid == currentUserUid) {
                Toast.makeText(requireContext(), "You can't add yourself", Toast.LENGTH_SHORT).show()
                return
            }

            // Add the friend to the current user's friend list
            db.collection("users")
                .document(currentUserUid)
                .collection("friends")
                .document(friendUid)
                .set(mapOf("friendUid" to friendUid))
                .await()

            // Add the current user to the friend's user friend list
            db.collection("users")
                .document(friendUid)
                .collection("friends")
                .document(currentUserUid)
                .set(mapOf("friendUid" to currentUserUid))
                .await()

            Toast.makeText(requireContext(), "Friend added", Toast.LENGTH_SHORT).show()
            filterAndDisplayFriends() // Refresh the list
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error adding friend: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun fetchAllUsers() {
        try {
            val querySnapshot = db.collection("users").get().await()
            allUsers = querySnapshot.documents.map { document ->
                User(document.id, document.getString("username") ?: "No Username")
            }
            dataLoaded = true
            filterAndDisplayFriends()
        } catch (e: Exception) {
            println("Error getting all users: $e")
        }
    }

    private fun filterAndDisplayFriends() {
        val currentUserUid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(currentUserUid)
            .collection("friends")
            .get()
            .addOnSuccessListener { result ->
                usersList.clear()
                for (document in result) {
                    val friendUid = document.getString("friendUid") ?: continue
                    val friend = allUsers.find { it.uid == friendUid }
                    friend?.let {
                        usersList.add(it)
                    }
                }
                userAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                println("Error getting friend list: $exception")
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
