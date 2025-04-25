package com.example.skovenomkap.ui.createchallenge// com.example.skovenomkap.FriendsAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.skovenomkap.R

class FriendsAdapter(
    val friendsList: MutableList<Friend>,
    val onFriendSelected: (String, Boolean) -> Unit // Callback function
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_item, parent, false) //Create friend_item.xml
        return FriendViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val currentItem = friendsList[position]
        holder.usernameTextView.text = currentItem.username
        holder.checkBox.setOnCheckedChangeListener(null) // Clear listener to avoid issues
        holder.checkBox.isChecked = false // Set default to unchecked

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onFriendSelected(currentItem.uid, isChecked)
        }
    }

    override fun getItemCount(): Int {
        return friendsList.size
    }
}
