package com.example.skovenomkap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameField = findViewById<EditText>(R.id.editUsername)
        val passwordField = findViewById<EditText>(R.id.editPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val signupButton = findViewById<Button>(R.id.buttonSignup)


        val username = usernameField.text.toString()
        val password = passwordField.text.toString()

        loginButton.setOnClickListener {
            if (username == "admin" && password == "1234") {
                succesful(username)
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }
        signupButton.setOnClickListener {
            succesful(username)
        }
    }
    fun succesful(username: String){
        // Save login state
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit() {
                putBoolean("is_logged_in", true)
            }

        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit() {
                putString("local_user", username)
            }

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}