package com.example.skovenomkap.ui.home

import ChallengeAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skovenomkap.ChallengeDetailActivity
import com.example.skovenomkap.ui.createchallenge.CreateChallengeActivity
import com.example.skovenomkap.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class Challenge(
    val challengeId: String = "", // Firebase document ID
    val type: String = "", // e.g., "steps", "distance", "time"
    val status: String = "active", // "active", "finished", "incoming"
    val creatorUid: String = "", // UID of the user who created the challenge
    val participants: List<String> = listOf(), // List of UIDs
    val settings: Map<String, Any> = mapOf(), // Challenge-specific settings (e.g., target steps, distance, time limit)
    val result: Map<String, Any> = mapOf(), // Challenge-specific results(e.g., steps taken, distance run, time spent)
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
            challengeList.addAll(incomingChallenges)
            challengeList.addAll(activeChallenges)
            challengeList.addAll(finishedChallenges)
            challengeAdapter.notifyDataSetChanged()

//        } catch (e: Exception) {
//            println("Error fetching challenges: $e")
//        }
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
