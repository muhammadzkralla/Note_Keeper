package com.zkrallah.notekeeper.ui.add

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.zkrallah.notekeeper.adapter.AddNoteAdapter
import com.zkrallah.notekeeper.databinding.ActivityAddNoteBinding
import com.zkrallah.notekeeper.local.entities.Note
import java.text.SimpleDateFormat
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private lateinit var imageUris: ArrayList<String>
    private val REQUEST_CODE = 200
    private lateinit var preferences: SharedPreferences
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = getSharedPreferences("rememberMe", MODE_PRIVATE)
        uid = preferences.getString("userID", "").toString()

        imageUris = arrayListOf()

        binding.images.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL ,false)
        val viewModel = ViewModelProvider(this)[AddNoteViewModel::class.java]

        binding.addImage.setOnClickListener {
            openGalleryForImages()
        }

        onBackPressedDispatcher.addCallback(this) {
            val title = binding.title.editText?.text.toString()
            val body = binding.body.editText?.text.toString()
            val time = Calendar.getInstance().time
            val formatter = SimpleDateFormat("MMM dd yyyy HH:mm", Locale.ROOT)
            val date = formatter.format(time)
            if (title.isNotEmpty() || body.isNotEmpty()) {
                val note = Note(
                    title,
                    body,
                    date,
                    uid,
                    imageUris.ifEmpty { null })

                viewModel.insert(note)
            } else
                Toast
                    .makeText(this@AddNoteActivity,
                        "Blank Note Discarded",
                        Toast.LENGTH_LONG)
                    .show()

            finish()
        }

    }

    private fun openGalleryForImages() {
        // For latest versions API LEVEL 19+
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE){

            // if multiple images are selected
            if (data?.clipData != null) {
                val count = data.clipData?.itemCount

                for (i in 0 until count!!) {
                    val imageUri: Uri = data.clipData?.getItemAt(i)!!.uri
                    this.contentResolver
                        .takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    imageUris.add(imageUri.toString())
                }

            } else if (data?.data != null) {
                // if single image is selected
                val imageUri: Uri = data.data!!
                this.contentResolver
                    .takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                imageUris.add(imageUri.toString())
            }
            binding.images.adapter = AddNoteAdapter(imageUris)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}