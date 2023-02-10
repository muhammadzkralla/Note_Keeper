package com.zkrallah.notekeeper.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.zkrallah.notekeeper.R
import com.zkrallah.notekeeper.adapter.HomeAdapter
import com.zkrallah.notekeeper.databinding.ActivityHomeBinding
import com.zkrallah.notekeeper.local.entities.Note
import com.zkrallah.notekeeper.ui.add.AddNoteActivity
import com.zkrallah.notekeeper.ui.show.ShowNoteActivity


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: HomeAdapter
    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        currentUser = auth.currentUser!!

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        val layoutManager = GridLayoutManager(this, 2)
        binding.recyclerHome.layoutManager = layoutManager

        updateUI()

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

    }

    private fun updateUI(){
        viewModel.getUserNotes(currentUser.uid)
        viewModel.userNotes.observe(this){
            it?.let {
                adapter = HomeAdapter(it)
                adapter.setItemClickListener(object: HomeAdapter.OnItemClickListener{
                    override fun onItemClick(note: Note) {
                        val intent = Intent(this@HomeActivity, ShowNoteActivity::class.java)
                        intent.putExtra("noteID", note.id.toString())
                        startActivity(intent)
                    }

                })
                binding.recyclerHome.adapter = adapter
            }
        }
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
            builder.setCancelable(true)
            builder.setPositiveButton("SIGN OUT") { _,_ ->
                auth.signOut()
                val preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
                val editor = preferences.edit()
                editor.remove("remember")
                editor.putString("remember", "false")
                editor.apply()
                Toast.makeText(this, "Your Email is now ${auth.currentUser?.email}", Toast.LENGTH_LONG).show()
                finish()
            }
            val dialog = builder.create()
            dialog.show()

            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        updateUI()
        super.onResume()
    }

}