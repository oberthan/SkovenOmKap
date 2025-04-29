package com.example.skovenomkap.ui

import android.content.Intent.getIntent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat.recreate
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skovenomkap.FirebaseHelper
import com.example.skovenomkap.R
import com.example.skovenomkap.databinding.FragmentPhotoReportBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class PhotoReportFragment : Fragment() {

    private var _binding: FragmentPhotoReportBinding? = null
    private val binding get() = _binding!!

    private val client = OkHttpClient() // OkHttp client
    private val apiKey = "2b10aEuExa6xtpVOHSSVEbgj1u" // Replace with your actual API key


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun getPathFromUri(uri: Uri): String? {
        val contentResolver = requireContext().contentResolver
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUriString = requireArguments().getString("imageUri")
            ?: throw IllegalArgumentException("imageUri is missing in arguments")
        val imageUri = imageUriString.toUri()
        Log.d("com.example.skovenomkap.ui.PhotoReportFragment", "Received image URI: $imageUri") // Add log

//        val imagePath = getPathFromUri(imageUri)
        val imagePath = imageUri.path

        if (imagePath != null) {
            val imageFile = File(imagePath)

            lifecycleScope.launch(Dispatchers.IO) {
                sendImageToPlantNet(imageFile)
            }
        } else {
            Log.e("com.example.skovenomkap.ui.PhotoReportFragment","Could not resolve file path from URI: $imageUri")
        }
    }
    private suspend fun sendImageToPlantNet(imageFile: File) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("images", imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .addFormDataPart("organs", "auto")
            .build()

        val request = Request.Builder()
            .url("https://my-api.plantnet.org/v2/identify/all?include-related-images=false&no-reject=false&nb-results=1&lang=en&type=kt&api-key=$apiKey")
            .header("accept", "application/json")
            .post(requestBody)
            .build()

        try {

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    binding.plantInformationTextView.text = "Vi kunne ikke finde en plante der ligner din"
                    return
                }

                val responseBody = response.body?.string()


                val jsonObject = responseBody?.let { JSONObject(it) }

                withContext(Dispatchers.Main) {

                    // 1. Access the "results" array
                    val resultsArray = jsonObject?.getJSONArray("results")

                    // 2. Get the first object in the "results" array
                    val firstResult = resultsArray?.getJSONObject(0)

                    // 3. Access the "species" object
                    val speciesObject = firstResult?.getJSONObject("species")

                    // 4. Access the "genus" object
                    val genusObject = speciesObject?.getJSONObject("genus")

                    // 5. Finally, get the "scientificNameWithoutAuthor" from the "genus" object
                    val genusScientificName = genusObject?.getString("scientificNameWithoutAuthor")


                    FirebaseHelper.updatePlant(genusScientificName.toString())

                    binding.plantInformationTextView.text =
                        "Dette ligner en ${jsonObject?.getString("bestMatch")}\nDen er fra slægten ${genusScientificName.toString()}\nJeg er ${
                            firstResult?.getLong("score")
                        } sikker"

                }
            }
        } catch (e: Exception) {
            binding.plantInformationTextView.text = "Vi kunne ikke finde en plante der ligner din"
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Toast.makeText(requireContext(), "Lukkede report", Toast.LENGTH_LONG).show()
        _binding = null
        recreate(requireActivity())
    }


}