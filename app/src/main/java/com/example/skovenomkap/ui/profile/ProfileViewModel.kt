package com.example.skovenomkap.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {



    private val _text = MutableLiveData<String>().apply {
        value = "Username"
    }
    val text: LiveData<String> = _text


}