package com.example.skovenomkap

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        val progressBar  = findViewById<ProgressBar>(R.id.progressBar)

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
            if (doc.exists()) {
                val data = doc.data!!
                tvType.text   = data["type"]   as String
                tvStatus.text = data["status"] as String

                // suppose settings["targetSteps"] and result["stepsTaken"]
                val settings = data["settings"] as? Map<String, Any> ?: emptyMap()
                val result   = data["result"]   as? Map<String, Any> ?: emptyMap()
                val target    = (settings["goal"] as? Number)?.toInt() ?: 0
                val done      = (result["stepsTaken"]   as? Number)?.toInt() ?: 0
                val percent   = if (target > 0) (done * 100 / target).coerceIn(0, 100) else 0
                progressBar.progress = percent
            }
        }
    }
}
