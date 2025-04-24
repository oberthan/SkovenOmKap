package com.example.skovenomkap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    // Get instance of Firebase Auth
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {

        // Initialize Firebase Auth
        auth = Firebase.auth

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailField = findViewById<EditText>(R.id.editEmail)
        val passwordField = findViewById<EditText>(R.id.editPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val signupButton = findViewById<Button>(R.id.buttonSignup)




        loginButton.setOnClickListener {
/*            if (email == "admin" && password == "1234") {
                succesful(email)
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }*/

            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val user = auth.currentUser
                        succesful(user?.email.toString())
                        // Update UI
                    } else {
                        // If sign in fails, display a message to the user.
                        // Display message
                        Toast.makeText(this, "Ugyldige legitimationsoplysninger", Toast.LENGTH_SHORT).show()
                    }
                }

        }
        signupButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val user = auth.currentUser
                        succesful(user?.email.toString())
                        // Update UI
                    } else {
                        // If sign in fails, display a message to the user.
                        // Display message
                    }
                }

        }
    }
    fun succesful(username: String){
        // Save login state
//        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//            .edit() {
//                putBoolean("is_logged_in", true)
//            }

        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit() {
                putString("local_user", username.split("@")[0])
            }

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}