package com.zkrallah.notekeeper.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.zkrallah.notekeeper.R
import com.zkrallah.notekeeper.adapter.HomeAdapter
import com.zkrallah.notekeeper.databinding.ActivityHomeBinding
import com.zkrallah.notekeeper.local.entities.Note
import com.zkrallah.notekeeper.ui.add.AddNoteActivity
import com.zkrallah.notekeeper.ui.show.ShowNoteActivity


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var viewModel: HomeViewModel
    private lateinit var preferences: SharedPreferences
    private lateinit var uid: String
    private lateinit var email:String
    private var onlineState = false
    private lateinit var dialog: AlertDialog
    private lateinit var recycler: RecyclerView
    private lateinit var notes: List<Note>
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = getSharedPreferences("rememberMe", MODE_PRIVATE)
        uid = preferences.getString("userID", "").toString()
        email = preferences.getString("userEmail", "").toString()

        notes = arrayListOf()
        onlineState = isOnline(this)
        buildAlertDialog()

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        binding.recyclerHome.layoutManager = GridLayoutManager(this, 2)

        updateUI()

        swipeRefreshLayout = binding.container
        swipeRefreshLayout.setOnRefreshListener {
            if (onlineState)sync(notes)
            swipeRefreshLayout.isRefreshing = false
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

    }

    private fun updateUI(){
        if (uid.isNotEmpty()) {
            viewModel.getUserNotes(uid)
            viewModel.userNotes.observe(this) {
                it?.let {
                    adapter = HomeAdapter(it)
                    adapter.setItemClickListener(object : HomeAdapter.OnItemClickListener {
                        override fun onItemClick(note: Note) {
                            val intent = Intent(this@HomeActivity, ShowNoteActivity::class.java)
                            intent.putExtra("noteID", note.id.toString())
                            startActivity(intent)
                        }

                    })
                    binding.recyclerHome.adapter = adapter
                    notes = it
                }
            }
        }
    }

    private fun sync(list: List<Note>) {
        viewModel.syncFirebase(list, uid)
        viewModel.conflicts.observe(this) {
            it?.let {

                val adapter = HomeAdapter(it)
                adapter.setItemClickListener(object: HomeAdapter.OnItemClickListener{
                    override fun onItemClick(note: Note) {
                        TODO("Not yet implemented")
                    }
                })
                recycler.adapter = adapter
                dialog.show()
            }
        }
    }

    private fun buildAlertDialog() {
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.show_conflicts, null)
        recycler = dialogView.findViewById(R.id.conflicts)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(true)
        builder.setTitle("CONFLICTS")
        builder.setMessage("THIS DATA IS NOT SYNCED !")
        dialog = builder.create()
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        Toast.makeText(context, "OFFLINE", Toast.LENGTH_LONG).show()
        return false
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
            builder.setMessage(email)
            builder.setCancelable(true)
            builder.setPositiveButton("SIGN OUT") { _,_ ->
                val editor = preferences.edit()
                editor.remove("remember")
                editor.remove("userID")
                editor.remove("userEmail")
                editor.putString("remember", "false")
                editor.apply()
                Toast.makeText(this, "Credentials Removed !", Toast.LENGTH_LONG).show()
                finish()
            }
            val dialog = builder.create()
            dialog.show()

            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        onlineState = isOnline(this)
        updateUI()
        super.onResume()
    }

}