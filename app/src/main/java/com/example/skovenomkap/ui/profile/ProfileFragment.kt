package com.example.skovenomkap.ui.profile

import PlantAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skovenomkap.databinding.FragmentProfileBinding
import java.util.Calendar
import java.util.Date

data class Plant(
    val name: String = "",
    val amount: Int = 0,
    val date: Date = Calendar.getInstance().time
)

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    private lateinit var plantAdapter: PlantAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[ProfileViewModel::class.java]

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textUsername
//        notificationsViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
            textView.text = this.activity?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("local_user", "Username")

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Initialize RecyclerView and Adapter
        plantAdapter = PlantAdapter(emptyList()) // Start with an empty list
        binding.plantsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = plantAdapter
        }

        // Observe the plants LiveData from the ViewModel
        viewModel.plants.observe(viewLifecycleOwner) { plants ->
            plantAdapter.plants = plants
            plantAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}