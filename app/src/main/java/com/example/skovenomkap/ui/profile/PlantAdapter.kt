import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.example.skovenomkap.R // Replace with your actual R class
import com.example.skovenomkap.ui.profile.Plant

class PlantAdapter(var plants: List<Plant>) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantNameTextView: TextView = itemView.findViewById(R.id.plantNameTextView)
        val plantDescriptionTextView: TextView = itemView.findViewById(R.id.plantDescriptionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false) // Replace with your item layout
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.plantNameTextView.text = plant.name
        holder.plantDescriptionTextView.text = "Du har fundet ${plant.amount},\n Den seneste blev fundet ${plant.date}"
    }

    override fun getItemCount(): Int = plants.size
}
