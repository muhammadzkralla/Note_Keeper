package com.zkrallah.notekeeper.ui.add

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.zkrallah.notekeeper.databinding.ActivityAddNoteBinding
import com.zkrallah.notekeeper.local.entities.Note
import java.text.SimpleDateFormat
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel = ViewModelProvider(this)[AddNoteViewModel::class.java]

        onBackPressedDispatcher.addCallback(this ) {
            val title = binding.title.editText?.text.toString()
            val body = binding.body.editText?.text.toString()
            val time = Calendar.getInstance().time
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
            val date = formatter.format(time)
            if (title.isNotEmpty() || body.isNotEmpty()){
                val note = Note(title
                    , body
                    , date
                    , Firebase.auth.currentUser!!.uid
                    ,null)

                viewModel.insert(note)
            } else
                Toast.makeText(this@AddNoteActivity, "Blank Note Discarded", Toast.LENGTH_LONG).show()

            finish()
        }

    }
}