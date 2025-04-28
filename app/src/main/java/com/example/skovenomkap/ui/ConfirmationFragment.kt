package com.example.skovenomkap.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.skovenomkap.databinding.FragmentConfirmationBinding
import android.net.Uri
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import android.util.Log // Import Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.skovenomkap.MainActivity
import java.io.File
import androidx.core.net.toUri
import com.example.skovenomkap.R


class ConfirmationFragment : Fragment() {

    private var _binding: FragmentConfirmationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUriString = requireArguments().getString("imageUri")
            ?: throw IllegalArgumentException("imageUri is missing in arguments")
        val imageUri = imageUriString.toUri()
        Log.d("com.example.skovenomkap.ui.ConfirmationFragment", "Received image URI: $imageUri") // Add log

        Glide.with(requireContext())
            .load(imageUri)
            .into(binding.confirmationImageView)

        binding.confirmButton.setOnClickListener {
            // Send to PlantNet and navigate to results
            sendImageToPlantNet(imageUri.toString())
        }

        binding.cancelButton.setOnClickListener {
            // Delete the image and go back to the camera
            deleteImage(imageUri)
            findNavController().popBackStack() // Go back to CameraFragment
        }
    }

    private fun sendImageToPlantNet(imageUriString: String) {
        // Convert the string back to a Uri
        val imageUri = imageUriString.toUri()
        val imagePath = getPathFromUri(imageUri)

        if (imagePath != null) {
            val bundle = Bundle().apply {
                putString("imageUri", imagePath.toString())
            }
            findNavController().navigate(R.id.navigation_photo_report, bundle)

        } else {
            Log.e("com.example.skovenomkap.ui.ConfirmationFragment", "Could not resolve file path from URI: $imageUri")
            showErrorToast("Could not resolve file path from URI")
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        val contentResolver = requireContext().contentResolver
        val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }
        return null
    }


    private fun deleteImage(uri: Uri) {
        requireContext().contentResolver.delete(uri, null, null)
    }

    private fun showErrorToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
