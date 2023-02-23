package com.zkrallah.notekeeper.ui.home

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.zkrallah.notekeeper.local.NoteDatabase
import com.zkrallah.notekeeper.local.entities.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val database = NoteDatabase.getInstance()

    private val _userNotes = MutableLiveData<List<Note>>()
    val userNotes = _userNotes
    private val _conflicts = MutableLiveData<List<Note>>()
    val conflicts = _conflicts

    fun getUserNotes(authorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _userNotes.postValue(database.noteDAO().getUserNotes(authorId))
        }
    }

    fun syncFirebase(list: List<Note>, authorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseDatabase.getInstance().getReference("Users")
                .child(authorId).child("Notes")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val onlineSet = mutableSetOf<Note>()
                        for (dataset in snapshot.children)
                            onlineSet.add(dataset.getValue(Note::class.java)!!)

                        val offlineSet = list.toSet()
                        val all = offlineSet union onlineSet
                        val common = offlineSet intersect onlineSet
                        val final = all subtract common
                        conflicts.postValue(final.toList())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }
    }

    fun syncNotes(
        local: List<Note>,
        toBeSynced: MutableSet<Note>,
        toBeDeleted: MutableSet<Note>,
        authorId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val reference = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(authorId).child("Notes")

            reference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val onlineSet = mutableSetOf<Note>()
                    for (dataset in snapshot.children)
                        onlineSet.add(dataset.getValue(Note::class.java)!!)

                    for (note in toBeSynced) {
                        if (!onlineSet.contains(note)) {
                            val map = mutableMapOf<String, Any>(
                                "title" to note.title,
                                "body" to note.body,
                                "date" to note.date,
                                "author" to note.author
                            )
                            if (note.images != null) map["images"] = getUrls(note)

                            reference.push().setValue(map)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }
        viewModelScope.launch(Dispatchers.IO) {
            for (note in toBeSynced) {
                if (!local.contains(note))
                    database.noteDAO().insert(
                        Note(
                            note.title,
                            note.body,
                            note.date,
                            authorId,
                            note.images?.ifEmpty { null })
                    )
            }
        }
        deleteUnwantedNotes(local, toBeDeleted, authorId)
    }

    private fun deleteUnwantedNotes(local: List<Note>, toBeRemoved: Set<Note>, authorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val reference = FirebaseDatabase.getInstance().getReference("Users")
                .child(authorId).child("Notes")

            reference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (note in toBeRemoved) {
                        for (dataset in snapshot.children) {
                            val dataShot = dataset.getValue(Note::class.java)!!
                            if (dataShot == note) {
                                reference.child(dataset.key.toString()).removeValue()
                                break
                            }
                        }
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
            viewModelScope.launch(Dispatchers.IO) {
                for (note in toBeRemoved) {
                    if (local.contains(note)) {
                        database.noteDAO().deleteNote(note.id)
                    }
                }
            }
        }
    }

    private fun getUrls(note: Note): List<String> {
        val urls = mutableListOf<String>()
        val tasks = mutableListOf<UploadTask>()
        val folder = FirebaseStorage.getInstance().getReference("images")

        for (count in 0 until note.images!!.size) {
            val task = folder.child(note.title + note.body + count.toString())
                .putFile(Uri.parse(note.images!![count]))
            tasks.add(task)
        }
        runBlocking {
            tasks.forEach {
                urls.add(it.await().storage.downloadUrl.await().toString())
            }
        }
        return urls
    }
}