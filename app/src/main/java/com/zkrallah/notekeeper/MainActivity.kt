package com.zkrallah.notekeeper


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.zkrallah.notekeeper.databinding.ActivityMainBinding
import com.zkrallah.notekeeper.model.User
import com.zkrallah.notekeeper.ui.home.HomeActivity


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var dialog: AlertDialog
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = getSharedPreferences("rememberMe", MODE_PRIVATE)

        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.progress_bar, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(false)
        dialog = builder.create()

        auth = Firebase.auth

        binding.loginBtn.setOnClickListener {
            dialog.show()
            signIn(binding.edtEmail.text.toString(), binding.edtPwd.text.toString())
        }

        binding.registerBtn.setOnClickListener {
            dialog.show()
            createAccount(binding.edtEmail.text.toString(), binding.edtPwd.text.toString())
        }

    }

    private fun createAccount(email: String, password: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                dialog.dismiss()
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("MainActivity", "createUserWithEmail:success")
                    Toast.makeText(baseContext, "Registration Success.",
                        Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser
                    val editor = preferences.edit()
                    editor.putString("remember", "true")
                    editor.putString("userID", auth.currentUser?.uid)
                    editor.putString("userEmail", auth.currentUser?.email)
                    editor.apply()
                    addUserToFireBase(user)
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("MainActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Registration failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signIn(email: String, password: String) {
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                dialog.dismiss()
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("MainActivity", "signInWithEmail:success")
                    Toast.makeText(baseContext, "Authentication Success.",
                        Toast.LENGTH_SHORT).show()
                    val editor = preferences.edit()
                    editor.putString("remember", "true")
                    editor.putString("userID", auth.currentUser?.uid)
                    editor.putString("userEmail", auth.currentUser?.email)
                    editor.apply()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("MainActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addUserToFireBase(userModel: FirebaseUser?) {
        if (userModel != null){
            val user = User(userModel.uid,
                binding.edtEmail.text.toString(),
                binding.edtPwd.text.toString())

            FirebaseDatabase.getInstance().getReference("Users")
                .child(user.uid).setValue(user)
        }

    }
}