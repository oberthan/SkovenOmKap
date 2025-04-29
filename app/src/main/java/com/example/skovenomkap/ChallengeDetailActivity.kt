package com.example.skovenomkap

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.skovenomkap.FirebaseHelper.getUsers
import com.example.skovenomkap.ui.home.Challenge
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChallengeDetailActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_detail)

        val tvType       = findViewById<TextView>(R.id.tvType)
        val tvStatus     = findViewById<TextView>(R.id.tvStatus)
        val progressBarLinearLayout: LinearLayout  = findViewById(R.id.progressbarLinearLayout)
        val participantsLinearLayout: LinearLayout  = findViewById(R.id.participantsLinearLayout)

        val challengeId = intent.getStringExtra("challengeId")
            ?: run {
                finish()
                return
            }

        // load the challenge document
        CoroutineScope(Dispatchers.Main).launch {
            val doc = db.collection("games")
                .document(challengeId)
                .get()
                .await()
                .toObject(Challenge::class.java)

            if (doc != null) {

                tvType.text = doc.type
                tvStatus.text = doc.status

                // suppose settings["targetSteps"] and result["stepsTaken"]
                val settings = doc.settings as? Map<String, Any> ?: emptyMap()
                val target = (settings["goal"] as? Number)?.toInt() ?: 0
//                val done = (result["stepsTaken"] as? Number)?.toInt() ?: 0
//                val percent = if (target > 0) (done * 100 / target).coerceIn(0, 100) else 0
//                progressBar.progress = percent


                getUsers().addOnSuccessListener { usersSnapshot ->
                    val byId = usersSnapshot.documents.associateBy { it.id }
                    for (uid in doc.participants) {
                        // Option A: create programmatically
                        val doc = byId[uid]
                        val username = doc?.getString("username") ?: "Ingen brugernavn"

                        val tv = TextView(baseContext).apply {
                            text = username
                            // match your xml styling if needed:
                            textSize = 16f
                            // you can also set padding/margins in code:
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                // e.g. small top-margin between names
                                topMargin = (4 * resources.displayMetrics.density).toInt()
                            }
                        }
                        participantsLinearLayout.addView(tv)
                    }
                }
            }
        }
    }
}
