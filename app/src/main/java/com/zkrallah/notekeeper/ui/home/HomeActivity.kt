package com.zkrallah.notekeeper.ui.home

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.zkrallah.notekeeper.R
import com.zkrallah.notekeeper.databinding.ActivityHomeBinding


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        currentUser = auth.currentUser!!

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.account) {
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setTitle("Your E-mail is : ")
            builder.setMessage(currentUser.email)
            builder.setPositiveButton("SIGN OUT") { _,_ ->
                auth.signOut()
                Toast.makeText(this, "Your Email is now ${auth.currentUser?.email}", Toast.LENGTH_LONG).show()
                finish()
            }
            val dialog = builder.create()
            dialog.show()

            return true
        }
        return super.onOptionsItemSelected(item)
    }

}