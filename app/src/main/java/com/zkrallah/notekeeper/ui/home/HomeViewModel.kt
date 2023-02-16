package com.zkrallah.notekeeper.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.zkrallah.notekeeper.local.NoteDatabase
import com.zkrallah.notekeeper.local.entities.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        viewModelScope.launch(Dispatchers.Unconfined) {
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

    fun syncNotes(local: List<Note>, toBeSynced: MutableSet<Note>, authorId: String) {
        viewModelScope.launch {
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
                            val map = mapOf(
                                "title" to note.title,
                                "body" to note.body,
                                "date" to note.date,
                                "author" to note.author
                            )
                            reference.push().setValue(map)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }
        viewModelScope.launch {
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
    }
}