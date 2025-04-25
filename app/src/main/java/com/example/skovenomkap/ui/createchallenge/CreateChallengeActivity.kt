package com.example.skovenomkap.ui.createchallenge

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skovenomkap.R
import com.example.skovenomkap.databinding.ActivityCreateChallengeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class CreateChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateChallengeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var friendsAdapter: FriendsAdapter
    private val selectedFriends = mutableListOf<String>()
    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Friends RecyclerView
        friendsAdapter = FriendsAdapter(mutableListOf()) { friendUid, isSelected ->
            if (isSelected) {
                selectedFriends.add(friendUid)
            } else {
                selectedFriends.remove(friendUid)
            }
        }

        binding.friendsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CreateChallengeActivity)
            adapter = friendsAdapter
        }

        // Fetch Friends
        CoroutineScope(Dispatchers.Main).launch {
            fetchFriends()
        }

        // Start Time Selection
        binding.startTimeEditText.setOnClickListener {
            showDateTimePicker()
        }

        //Implement the listeners for the views
        binding.createChallengeButton.setOnClickListener {
            createChallenge()
        }
    }

    private fun showDateTimePicker() {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                selectedDateTime.set(year, month, day, hour, minute)
                updateDateTimeText()
            }, startHour, startMinute, false).show()
        }, startYear, startMonth, startDay).show()
    }

    private fun updateDateTimeText() {
        val dateFormat = android.text.format.DateFormat.getDateFormat(this)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val formattedDateTime = dateFormat.format(selectedDateTime.time) + " " + timeFormat.format(selectedDateTime.time)
        binding.startTimeEditText.setText(formattedDateTime)
    }

    private fun createChallenge() {
        val challengeType = when (binding.challengeTypeRadioGroup.checkedRadioButtonId) {
            R.id.timeChallengeRadioButton -> "time"
            R.id.goalChallengeRadioButton -> "goal"
            else -> {
                Toast.makeText(this, "Please select a challenge type", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val startTime: Date = selectedDateTime.time

        val settings = mutableMapOf<String, Any>()

        when (challengeType) {
            "time" -> {
                val challengeLengthString = binding.timeChallengeLengthEditText.text.toString()
                val challengeLength = challengeLengthString.toIntOrNull()
                if (challengeLength == null) {
                    Toast.makeText(this, "Invalid challenge length", Toast.LENGTH_SHORT).show()
                    return
                }
                settings["challengeLength"] = challengeLength
            }
            "goal" -> {
                val goalString = binding.goalChallengeGoalEditText.text.toString()
                val goal = goalString.toIntOrNull()
                if (goal == null) {
                    Toast.makeText(this, "Invalid goal", Toast.LENGTH_SHORT).show()
                    return
                }
                settings["goal"] = goal
            }
        }

        val currentUserUid = auth.currentUser?.uid ?: return

        val challenge = Challenge(
            type = challengeType,
            creatorUid = currentUserUid,
            participants = selectedFriends + currentUserUid,
            settings = settings,
            timestamp = startTime,
            status = "active"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("games")
                    .add(challenge)
                    .await()
                Toast.makeText(this@CreateChallengeActivity, "Challenge created!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CreateChallengeActivity, "Error creating challenge: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchFriends() {
        val currentUserUid = auth.currentUser?.uid ?: return

        try {
            val friendUids = db.collection("users")
                .document(currentUserUid)
                .collection("friends")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("friendUid") }

            val friendsList = mutableListOf<Friend>()
            for (friendUid in friendUids) {
                val friendDoc = db.collection("users").document(friendUid).get().await()
                val username = friendDoc.getString("username") ?: "Ingen brugernavn"
                friendsList.add(Friend(friendUid, username))
            }

            friendsAdapter.friendsList.clear()
            friendsAdapter.friendsList.addAll(friendsList)
            friendsAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "Error fetching friends: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

data class Challenge(
    val type: String = "",
    val creatorUid: String = "",
    val participants: List<String> = listOf(),
    val settings: Map<String, Any> = mapOf(),
    val timestamp: Date = Date(),
    val status: String = "active"
)

data class Friend(val uid: String, val username: String)
