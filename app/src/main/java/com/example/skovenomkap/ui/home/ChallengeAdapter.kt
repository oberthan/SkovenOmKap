// UdfordringsAdapter.kt
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.skovenomkap.FirebaseHelper
import com.example.skovenomkap.FirebaseHelper.getPlant
import com.example.skovenomkap.FirebaseHelper.getUsers
import com.example.skovenomkap.R
import com.example.skovenomkap.ui.home.Udfordrings
import com.example.skovenomkap.ui.profile.Plant
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class UdfordringsAdapter(
        private val challengeList: List<Udfordrings>,
        private val onClick: (Udfordrings) -> Unit
    ) : RecyclerView.Adapter<UdfordringsAdapter.UdfordringsViewHolder>() {

    class UdfordringsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val challengeTypeTextView: TextView = itemView.findViewById(R.id.challengeTypeTextView)
        val challengeStatusTextView: TextView = itemView.findViewById(R.id.challengeStatusTextView)
        val participantsContainer: LinearLayout = itemView.findViewById(R.id.participantsLinearLayout)
        val målTextView: TextView = itemView.findViewById(R.id.målTextView)
        val yourPlantsTextView: TextView = itemView.findViewById(R.id.yourPlantsTextView)
        // Add more views to display other challenge information
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UdfordringsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.challenge_item, parent, false) // Create challenge_item.xml
        return UdfordringsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UdfordringsViewHolder, position: Int) {
        val currentItem = challengeList[position]
        holder.challengeTypeTextView.text = currentItem.type
        holder.challengeStatusTextView.text = currentItem.status
        holder.itemView.setOnClickListener {
            onClick(currentItem)
        }
    // Bind more data to the views
        var byId: Map<String, DocumentSnapshot> = mapOf()
        holder.participantsContainer.removeAllViews()
        getUsers().addOnSuccessListener { usersSnapshot ->
            byId = usersSnapshot.documents.associateBy { it.id }
            for (uid in currentItem.participants) {
                // Option A: create programmatically
                val doc = byId[uid]
                val username = doc?.getString("username") ?: "Ingen brugernavn"

                val tv = TextView(holder.itemView.context).apply {
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
                holder.participantsContainer.addView(tv)
            }


            var underTekst = ""
            var overTekst = ""
            when (currentItem.status) {
                "Aktiv" -> {
                    underTekst = "ud af"
                    getPlant().addOnSuccessListener { plantsSnapshot ->
                        val plants = plantsSnapshot.documents.mapNotNull { doc ->
                            // 1) deserialize everything else
                            val plant = doc.toObject(Plant::class.java)
                            if (plant != null) {
                                // 2) grab the Firestore field "last-seen" as a Date
                                //    (DocumentSnapshot.getDate() returns java.util.Date?)
                                val lastSeenDate = doc.getDate("last-seen")
                                // 3) assign into your Plant.date
                                plant.date = lastSeenDate
                                plant
                            } else {
                                null
                            }
                        }
                        val myplants = plants.filter { plant ->
                            plant.date!! > currentItem.timestamp
                        }

                        holder.yourPlantsTextView.text = "Du har ${myplants.size} planter"
                    }
                }

                "Slut" -> {
                    underTekst = "De fik først"

                    holder.yourPlantsTextView.text =
                        "${byId[currentItem.winner]?.getString("username")} har vundet!"
                }

                "Kommende" -> {
                    underTekst = "Du skal finde"

                    val endDate = currentItem.timestamp    // your Plant.date or other end timestamp
                    val endMillis = endDate.time
                    val nowMillis = Calendar.getInstance().time.time

                    val relativeEnd: CharSequence = DateUtils.getRelativeTimeSpanString(
                        endMillis,
                        nowMillis,
                        DateUtils.MINUTE_IN_MILLIS,              // minimum unit
                        DateUtils.FORMAT_ABBREV_RELATIVE         // "mins" vs "minutes"
                    )
                    holder.yourPlantsTextView.text = "Starter ${relativeEnd}"
                }
            }

            when (currentItem.type) {
                "mål" -> {
                    holder.målTextView.text =
                        "$underTekst ${currentItem.settings.get("mål")} planter"
                }

                "Tid" -> {
                    val endDate =
                        currentItem.settings["challengeLength"] as? Timestamp    // your Plant.date or other end timestamp
                    val endMillis = endDate?.toDate()!!.time
                    val nowMillis = Calendar.getInstance().time.time

// shows e.g. "in 2 days", "in 3 h", "in 5 min"
                    val relativeEnd: CharSequence = DateUtils.getRelativeTimeSpanString(
                        endMillis,
                        nowMillis,
                        DateUtils.MINUTE_IN_MILLIS,              // minimum unit
                        DateUtils.FORMAT_ABBREV_RELATIVE         // "mins" vs "minutes"
                    )


                    holder.målTextView.text = "Slutter: ${relativeEnd}"
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return challengeList.size
    }
}
