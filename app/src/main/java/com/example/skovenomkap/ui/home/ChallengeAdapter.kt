// ChallengeAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.skovenomkap.R
import com.example.skovenomkap.ui.home.Challenge

class ChallengeAdapter(private val challengeList: List<Challenge>) :
    RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val challengeTypeTextView: TextView = itemView.findViewById(R.id.challengeTypeTextView)
        val challengeStatusTextView: TextView = itemView.findViewById(R.id.challengeStatusTextView)
        // Add more views to display other challenge information
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.challenge_item, parent, false) // Create challenge_item.xml
        return ChallengeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val currentItem = challengeList[position]
        holder.challengeTypeTextView.text = currentItem.type
        holder.challengeStatusTextView.text = currentItem.status
        // Bind more data to the views
    }

    override fun getItemCount(): Int {
        return challengeList.size
    }
}
