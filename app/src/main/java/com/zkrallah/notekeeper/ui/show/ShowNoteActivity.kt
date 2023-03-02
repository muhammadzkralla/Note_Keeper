package com.zkrallah.notekeeper.ui.show

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.zkrallah.notekeeper.R
import com.zkrallah.notekeeper.adapter.AddNoteAdapter
import com.zkrallah.notekeeper.databinding.ActivityShowNoteBinding
import com.zkrallah.notekeeper.local.entities.Note

class ShowNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowNoteBinding
    private lateinit var viewModel: ShowNoteViewModel
    private lateinit var currentNote: Note

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ShowNoteViewModel::class.java]

        binding.images.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val id = intent.getStringExtra("noteID")?.toLong()

        if (id != null) {
            viewModel.getNote(id)
            viewModel.note.observe(this) {
                currentNote = it
                binding.title.editText?.setText(it.title)
                binding.title.editText?.inputType = InputType.TYPE_NULL
                binding.title.editText?.isFocusable = false
                binding.body.editText?.setText(it.body)
                binding.images.adapter = AddNoteAdapter(it.images)
            }
        } else {
            Toast.makeText(this, "FAILED TO GET NOTE", Toast.LENGTH_LONG).show()
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            val title = binding.title.editText?.text.toString()
            val body = binding.body.editText?.text.toString()
            currentNote.title = title
            currentNote.body = body
            if (title.isNotEmpty() || body.isNotEmpty()) {
                viewModel.update(currentNote)
            } else
                Toast
                    .makeText(
                        this@ShowNoteActivity,
                        "Blank Note Discarded",
                        Toast.LENGTH_LONG
                    )
                    .show()

            finish()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.delete, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.delete) {
            viewModel.deleteNote(currentNote.id)
            Toast.makeText(this@ShowNoteActivity, "Note Deleted !", Toast.LENGTH_SHORT)
                .show()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}

