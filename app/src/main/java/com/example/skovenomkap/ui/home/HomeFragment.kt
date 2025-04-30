package com.example.skovenomkap.ui.home

import UdfordringsAdapter
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
import com.example.skovenomkap.FirebaseHelper.leading
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

data class Udfordrings(
    val challengeId: String = "", // Firebase document ID
    val type: String = "", // e.g., "steps", "distance", "Tid"
    var status: String = "Aktiv", // "Aktiv", "Slut", "Kommende"
    val creatorUid: String = "", // UID of the user who created the challenge
    val participants: List<String> = listOf(), // List of UIDs
    val settings: Map<String, Any> = mapOf(), // Udfordrings-specific settings (e.g., target steps, distance, time limit)
    val winner: String = "", // Udfordrings-specific results(e.g., steps taken, distance run, time spent)
    val timestamp: Date = Date(), // Timestamp of when the challenge was created
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val challengeList = mutableListOf<Udfordrings>()
    private lateinit var challengeAdapter: UdfordringsAdapter

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

        challengeAdapter = UdfordringsAdapter(challengeList) { challenge ->
            val intent = Intent(requireContext(), ChallengeDetailActivity::class.java).apply {
                putExtra("challengeId", challenge.challengeId)
            }
            startActivity(intent)
        }


        binding.recyclerViewUdfordringss.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = challengeAdapter
        }

        binding.createUdfordringsButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateChallengeActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                fetchUdfordringss()
                binding.swipeRefresh.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            fetchUdfordringss()
        }
    }

    private suspend fun fetchUdfordringss() {
        val currentUserUid = auth.currentUser?.uid ?: return

//        try {
//            // Fetch Kommende challenges
            val KommendeUdfordringss = db.collection("games")
                .whereArrayContains("participants", currentUserUid)
                .whereEqualTo("status", "Kommende")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { document ->
                    document.toObject(Udfordrings::class.java)?.copy(challengeId = document.id)
                }
        println(KommendeUdfordringss)

        for (challenge in KommendeUdfordringss) {
            if (challenge.timestamp < Calendar.getInstance().time) {
                db.collection("games")
                    .document(challenge.challengeId)
                    .update("status", "Aktiv")

                challenge.status = "Aktiv"
            }
        }

            // Fetch Aktiv challenges where the user is a participant
            val AktivUdfordringss = db.collection("games")
                .whereArrayContains("participants", currentUserUid)
                .whereEqualTo("status", "Aktiv")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { document ->
                    document.toObject(Udfordrings::class.java)?.copy(challengeId = document.id)
                }
        println(AktivUdfordringss)
        for (challenge in AktivUdfordringss) {
            if (challenge.type == "Mål") {
                val lead = leading(challenge)
                if (lead != null) {
                    if (lead.value >= challenge.settings["Mål"] as Long) {
                        db.collection("games")
                            .document(challenge.challengeId)
                            .set(
                                hashMapOf<String, Any>(
                                    "status" to "Slut",
                                    "winner" to lead.key
                                ), SetOptions.merge()
                            )
                            .await()
                        challenge.status = "Slut"
                    }
                }
            } else if (challenge.type == "Tid") {
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
                                "status" to "Slut",
                                "winner" to winner!!.key
                            ), SetOptions.merge()
                        )
                        .await()
                    challenge.status = "Slut"
                }
            }

        }

            // Fetch Slut challenges where the user is a participant|
            val SlutUdfordringss = db.collection("games")
                .whereArrayContains("participants", currentUserUid)
                .whereEqualTo("status", "Slut")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { document ->
                    document.toObject(Udfordrings::class.java)?.copy(challengeId = document.id)
                }

        challengeList.clear()
            challengeList.addAll(AktivUdfordringss)
            challengeList.addAll(KommendeUdfordringss)
            challengeList.addAll(SlutUdfordringss)
            challengeAdapter.notifyDataSetChanged()

//        } catch (e: Exception) {
//            println("Error fetching challenges: $e")
//        }
    }



    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
//            fetchUdfordringss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
