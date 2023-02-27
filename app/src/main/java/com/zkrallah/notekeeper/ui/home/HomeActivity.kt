package com.zkrallah.notekeeper.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.zkrallah.notekeeper.R
import com.zkrallah.notekeeper.RandomQuote
import com.zkrallah.notekeeper.adapter.HomeAdapter
import com.zkrallah.notekeeper.adapter.SyncNoteAdapter
import com.zkrallah.notekeeper.databinding.ActivityHomeBinding
import com.zkrallah.notekeeper.local.entities.Note
import com.zkrallah.notekeeper.ui.add.AddNoteActivity
import com.zkrallah.notekeeper.ui.show.ShowNoteActivity


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var preferences: SharedPreferences
    private lateinit var uid: String
    private lateinit var email: String
    private lateinit var dialog: AlertDialog
    private lateinit var recycler: RecyclerView
    private lateinit var notes: List<Note>
    private lateinit var conflicts: MutableSet<Note>
    private lateinit var notesToBeSynced: MutableSet<Note>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = getSharedPreferences("rememberMe", MODE_PRIVATE)
        uid = preferences.getString("userID", "").toString()
        email = preferences.getString("userEmail", "").toString()

        notes = arrayListOf()
        notesToBeSynced = mutableSetOf()
        buildAlertDialog()

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        swipeRefreshLayout = binding.container

        updateUI()

        swipeRefreshLayout.setOnRefreshListener {
            updateUI()
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        if (isOnline(this)){
            Handler(Looper.getMainLooper()).postDelayed({
                sync(notes)
            }, 3000)
        }

    }

    private fun updateUI() {
        if (uid.isNotEmpty()) {
            viewModel.getUserNotes(uid)
            viewModel.userNotes.observe(this) {
                it?.let {
                    val adapter = HomeAdapter(it)
                    adapter.setItemClickListener(object : HomeAdapter.OnItemClickListener {
                        override fun onItemClick(note: Note) {
                            val intent = Intent(this@HomeActivity, ShowNoteActivity::class.java)
                            intent.putExtra("noteID", note.id.toString())
                            startActivity(intent)
                        }

                    })
                    binding.recyclerHome.adapter = adapter
                    val layoutManager =
                        StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
                    binding.recyclerHome.layoutManager = layoutManager
                    notes = it
                }
            }
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun sync(list: List<Note>) {
        viewModel.syncFirebase(list, uid)
        viewModel.conflicts.observe(this) {
            it?.let {
                val adapter = SyncNoteAdapter(it)
                if (it.isNotEmpty()) {
                    conflicts = it.toSet() as MutableSet<Note>
                    notesToBeSynced = adapter.toBeSynced
                    recycler.adapter = adapter
                    dialog.show()
                }

                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun buildAlertDialog() {
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.show_conflicts, null)
        recycler = dialogView.findViewById(R.id.conflicts)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val builder = AlertDialog.Builder(this, R.style.MyDialogTheme)
        builder.setView(dialogView)
        builder.setCancelable(true)
        builder.setTitle("CONFLICTS")
        builder.setMessage(
            "THIS DATA IS NOT SYNCED" +
                    ", PLEASE SELECT NOTES TO KEEP " +
                    "AND THE OTHERS WILL BE REMOVED AUTOMATICALLY."
        )
        builder.setPositiveButton("SYNC") { _, _ ->
            val notesToBeDeleted = (conflicts subtract  notesToBeSynced) as MutableSet<Note>
            viewModel.syncNotes(notes, notesToBeSynced, notesToBeDeleted, uid)
            buildLoadingDialog()
            dialog.show()
            viewModel.finishedLoadingState.observe(this){ state ->
                if (state){
                    finish()
                    startActivity(intent)
                }
            }
        }
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun buildLoadingDialog() {
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.loading_dialog, null)
        val quoteTV = dialogView.findViewById<TextView>(R.id.quote)
        quoteTV.text = RandomQuote().randomQuote
        val builder = AlertDialog.Builder(this, R.style.MyDialogTheme)
        builder.setView(dialogView)
        builder.setCancelable(false)
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.account) {
            val builder = AlertDialog.Builder(this, R.style.MyDialogTheme)
            builder.setCancelable(false)
            builder.setTitle("Your E-mail is : ")
            builder.setMessage(email)
            builder.setCancelable(true)
            builder.setPositiveButton("SIGN OUT") { _, _ ->
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
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()

            return true
        }
        if (id == R.id.sync){
            if (isOnline(this)) sync(notes)
            else Toast.makeText(this, "OFFLINE MODE", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        updateUI()
        super.onResume()
    }

}