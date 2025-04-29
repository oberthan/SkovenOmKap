package com.example.skovenomkap.ui.createchallenge

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TextView
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
    private var endDateTime: Calendar = Calendar.getInstance()

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
            showDateTimePicker(binding.startTimeEditText, selectedDateTime)
        }

        binding.challengeTypeRadioGroup.setOnCheckedChangeListener {group, checkedId ->
            when(checkedId){
                R.id.timeUdfordringsRadioButton -> {
                    binding.timeUdfordringsLengthEditText.setOnClickListener {
                        showDateTimePicker(binding.timeUdfordringsLengthEditText, endDateTime)
                    }
                    binding.timeUdfordringsLengthEditText.isFocusable = false
                    binding.settingsTextView.text = "Indtast hvornår spillet skal afsluttes"
                }
                R.id.målUdfordringsRadioButton -> {
                    binding.timeUdfordringsLengthEditText.setOnClickListener(null)
                    binding.settingsTextView.text = "Indtast hvor mange planter man skal opnå"
                    binding.timeUdfordringsLengthEditText.isFocusable = true

                }

            }
        }

        //Implement the listeners for the views
        binding.createUdfordringsButton.setOnClickListener {
            createUdfordrings()
        }
    }

    private fun showDateTimePicker(EditText: TextView, dateTime: Calendar) {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                dateTime.set(year, month, day, hour, minute)
                updateDateTimeText(EditText, dateTime)
            }, startHour, startMinute, true).show()
        }, startYear, startMonth, startDay).show()
    }

    private fun updateDateTimeText(EditText: TextView, dateTime: Calendar) {
        val dateFormat = android.text.format.DateFormat.getDateFormat(this)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val formattedDateTime = dateFormat.format(dateTime.time) + " " + timeFormat.format(dateTime.time)
        EditText.text = formattedDateTime
    }

    private fun createUdfordrings() {
        val challengeType = when (binding.challengeTypeRadioGroup.checkedRadioButtonId) {
            R.id.timeUdfordringsRadioButton -> "Tid"
            R.id.målUdfordringsRadioButton -> "mål"
            else -> {
                Toast.makeText(this, "Please select a challenge type", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val startTime: Date = selectedDateTime.time

        val settings = mutableMapOf<String, Any>()

        when (challengeType) {
            "Tid" -> {
                val challengeLengthString = binding.timeUdfordringsLengthEditText.text.toString()
                val challengeLength: Date = endDateTime.time
                if (challengeLength == null) {
                    Toast.makeText(this, "Invalid challenge length", Toast.LENGTH_SHORT).show()
                    return
                }
                settings["challengeLength"] = challengeLength
            }
            "mål" -> {
                val målString = binding.timeUdfordringsLengthEditText.text.toString()
                val mål = målString.toIntOrNull()
                if (mål == null) {
                    Toast.makeText(this, "Invalid mål", Toast.LENGTH_SHORT).show()
                    return
                }
                settings["mål"] = mål
            }
        }

        val currentUserUid = auth.currentUser?.uid ?: return

        val challenge = Udfordrings(
            type = challengeType,
            creatorUid = currentUserUid,
            participants = selectedFriends + currentUserUid,
            settings = settings,
            timestamp = startTime,
            status = "Kommende"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("games")
                    .add(challenge)
                    .await()
                Toast.makeText(this@CreateChallengeActivity, "Udfordrings created!", Toast.LENGTH_SHORT).show()
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

data class Udfordrings(
    val type: String = "",
    val creatorUid: String = "",
    val participants: List<String> = listOf(),
    val settings: Map<String, Any> = mapOf(),
    val timestamp: Date = Date(),
    val status: String = "Kommende"
)

data class Friend(val uid: String, val username: String)
