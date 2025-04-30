package com.example.skovenomkap

import android.os.Bundle
import android.text.format.DateUtils
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.skovenomkap.FirebaseHelper.getUsers
import com.example.skovenomkap.FirebaseHelper.ranking
import com.example.skovenomkap.ui.home.HomeFragment
import com.example.skovenomkap.ui.home.Udfordrings
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ChallengeDetailActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_detail)

        val tvType       = findViewById<TextView>(R.id.tvType)
        val tvStatus     = findViewById<TextView>(R.id.tvStatus)
        val progressBarLinearLayout: LinearLayout  = findViewById(R.id.progressbarLinearLayout)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val participantsLinearLayout: LinearLayout  = findViewById(R.id.participantsLinearLayout)
        val objectiveText: TextView = findViewById(R.id.objectiveTextView)

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
                .toObject(Udfordrings::class.java)

            if (doc != null) {

                tvType.text = doc.type
                tvStatus.text = doc.status

                // suppose settings["targetSteps"] and result["stepsTaken"]
                val settings = doc.settings as? Map<String, Any> ?: emptyMap()

                val ranking = ranking(doc)
                val entriesSorted = ranking.entries.sortedByDescending {it.value}
                val byCount: Map<String,Int> = entriesSorted
                    .associate { entry -> entry.key to entry.value }


                when(doc.type){
                    "Tid" -> {
                        val endDate = doc.settings["challengeLength"] as? Timestamp    // your Plant.date or other end timestamp
                        val endMillis = endDate?.toDate()!!.time
                        val nowMillis = Calendar.getInstance().time.time
                        val startTime = doc.timestamp.time.toInt()
                        progressBar.max = endMillis.toInt() - startTime

                        val progress = nowMillis-startTime
                        progressBar.progress = progress.toInt()

// shows e.g. "in 2 days", "in 3 h", "in 5 min"
                        val relativeEnd: CharSequence = DateUtils.getRelativeTimeSpanString(
                            endMillis,
                            nowMillis,
                            DateUtils.MINUTE_IN_MILLIS,              // minimum unit
                            DateUtils.FORMAT_ABBREV_RELATIVE         // "mins" vs "minutes"
                        )

                        objectiveText.text = "Den der finder flest forskellige planteslægter inden $relativeEnd har vundet."
                    }
                    "Mål" -> {
                        val target = (settings["Mål"] as? Number)?.toInt() ?: 0
                        progressBar.max = target
                        val progress = byCount.maxByOrNull { it.value }!!.value
                        progressBar.progress = progress

                        objectiveText.text = "Den første der finder $target forskellige planteslægter har vundet."
                    }
                }
//                val done = (result["stepsTaken"] as? Number)?.toInt() ?: 0
//                val percent = if (target > 0) (done * 100 / target).coerceIn(0, 100) else 0
//                progressBar.progress = percent


                getUsers().addOnSuccessListener { usersSnapshot ->
                    val byId = usersSnapshot.documents.associateBy { it.id }
                    for (uid in byCount.keys) {
                        // Option A: create programmatically
                        val doc = byId[uid]
                        val username = doc?.getString("username") ?: "Ingen brugernavn"

                        val tv = TextView(baseContext).apply {
                            text = "\'$username\' har fundet ${byCount[uid]} plante(r)."
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
