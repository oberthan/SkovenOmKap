package com.example.skovenomkap.ui.home

import ChallengeAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skovenomkap.ChallengeDetailActivity
import com.example.skovenomkap.FirebaseHelper.getPlant
import com.example.skovenomkap.ui.createchallenge.CreateChallengeActivity
import com.example.skovenomkap.databinding.FragmentHomeBinding
import com.example.skovenomkap.ui.profile.Plant
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

data class Challenge(
    val challengeId: String = "", // Firebase document ID
    val type: String = "", // e.g., "steps", "distance", "time"
    var status: String = "active", // "active", "finished", "incoming"
    val creatorUid: String = "", // UID of the user who created the challenge
    val participants: List<String> = listOf(), // List of UIDs
    val settings: Map<String, Any> = mapOf(), // Challenge-specific settings (e.g., target steps, distance, time limit)
    val winner: String = "", // Challenge-specific results(e.g., steps taken, distance run, time spent)
    val timestamp: Date = Date(), // Timestamp of when the challenge was created
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val challengeList = mutableListOf<Challenge>()
    private lateinit var challengeAdapter: ChallengeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        challengeAdapter = ChallengeAdapter(challengeList) { challenge ->
            val intent = Intent(requireContext(), ChallengeDetailActivity::class.java).apply {
                putExtra("challengeId", challenge.challengeId)
            }
            startActivity(intent)
        }


        binding.recyclerViewChallenges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = challengeAdapter
        }

        binding.createChallengeButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateChallengeActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                fetchChallenges()
                binding.swipeRefresh.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            fetchChallenges()
        }
    }

    private suspend fun fetchChallenges() {
        val currentUserUid = auth.currentUser?.uid ?: return

//        try {
//            // Fetch incoming challenges
            val incomingChallenges = db.collection("games")
                .whereArrayContains("participants", currentUserUid)
                .whereEqualTo("status", "incoming")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { document ->
                    document.toObject(Challenge::class.java)?.copy(challengeId = document.id)
                }
        println(incomingChallenges)

        for (challenge in incomingChallenges) {
            if (challenge.timestamp < Calendar.getInstance().time) {
                db.collection("games")
                    .document(challenge.challengeId)
                    .update("status", "active")

                challenge.status = "active"
            }
        }

            // Fetch active challenges where the user is a participant
            val activeChallenges = db.collection("games")
                .whereArrayContains("participants", currentUserUid)
                .whereEqualTo("status", "active")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { document ->
                    document.toObject(Challenge::class.java)?.copy(challengeId = document.id)
                }
        println(activeChallenges)
        for (challenge in activeChallenges) {
            if (challenge.type == "goal") {

            } else if (challenge.type == "time") {
                val challengeLength = challenge.settings["challengeLength"] as? Date
                val endDate = challenge.settings["challengeLength"] as? Timestamp    // your Plant.date or other end timestamp
                val endMillis = endDate?.toDate()!!.time
                val nowMillis = Calendar.getInstance().time.time
                if (endMillis < nowMillis) {
                    val winner = leading(challenge)
                    db.collection("games")
                        .document(challenge.challengeId)
                        .set(
                            hashMapOf<String, Any>(
                                "status" to "finished",
                                "winner" to winner!!.key
                            ), SetOptions.merge()
                        )
                        .await()
                    challenge.status = "finished"
                }
            }

        }

            // Fetch finished challenges where the user is a participant|
            val finishedChallenges = db.collection("games")
                .whereArrayContains("participants", currentUserUid)
                .whereEqualTo("status", "finished")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { document ->
                    document.toObject(Challenge::class.java)?.copy(challengeId = document.id)
                }

        challengeList.clear()
            challengeList.addAll(activeChallenges)
            challengeList.addAll(incomingChallenges)
            challengeList.addAll(finishedChallenges)
            challengeAdapter.notifyDataSetChanged()

//        } catch (e: Exception) {
//            println("Error fetching challenges: $e")
//        }
    }

    private suspend fun leading(challenge: Challenge): Map.Entry<String, Int>? {
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

        // 4) pick the max—or null if empty
        return counts.maxByOrNull { it.value }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
//            fetchChallenges()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
