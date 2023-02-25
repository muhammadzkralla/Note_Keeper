package com.zkrallah.notekeeper.ui.home

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.zkrallah.notekeeper.local.NoteDatabase
import com.zkrallah.notekeeper.local.entities.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.File

class HomeViewModel : ViewModel() {

    private val database = NoteDatabase.getInstance()

    private val _userNotes = MutableLiveData<List<Note>>()
    val userNotes = _userNotes
    private val _conflicts = MutableLiveData<List<Note>>()
    val conflicts = _conflicts
    private val _finishedLoadingState = MutableLiveData(false)
    val finishedLoadingState = _finishedLoadingState

    fun getUserNotes(authorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _userNotes.postValue(database.noteDAO().getUserNotes(authorId))
        }
    }

    /**
     * Getting the conflicts between Room Database and the Firebase
     * Realtime Database.
     */
    fun syncFirebase(list: List<Note>, authorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val reference = FirebaseDatabase.getInstance().getReference("Users")
                .child(authorId).child("Notes")

            // Fetch and store the notes online in the onlineSet variable as
            // Note objects.
            val snapshot = reference.get().await()
            val onlineSet = mutableSetOf<Note>()
            for (dataset in snapshot.children)
                onlineSet.add(dataset.getValue(Note::class.java)!!)

            // The filtering algorithm :
            val offlineSet = list.toSet()
            val all = offlineSet union onlineSet
            val common = offlineSet intersect onlineSet
            val final = all subtract common
            conflicts.postValue(final.toList())
        }
    }

    /**
     * This is the core function of the application,
     * here we receive the local notes, the
     * notes selected by the user to be synced and notes to be
     * removed.
     * It has three parts which are three parallel coroutines,
     * one for online syncing, offline syncing and finally
     * deletion from both which is also two parallel coroutines.
     */
    fun syncNotes(
        local: List<Note>,
        toBeSynced: MutableSet<Note>,
        toBeDeleted: MutableSet<Note>,
        authorId: String
    ) {
        val onlineJob = viewModelScope.launch(Dispatchers.IO) {
            val reference = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(authorId).child("Notes")

            // Fetch and store the notes online in the onlineSet variable as
            // Note objects.
            val snapshot = reference.get().await()
            val onlineSet = mutableSetOf<Note>()
            for (dataset in snapshot.children)
                onlineSet.add(dataset.getValue(Note::class.java)!!)

            // If we found a note selected by the user and
            // not on the Firebase we push it there.
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
        val offlineJob = viewModelScope.launch(Dispatchers.IO) {
            // If we found a note selected by the user and
            // not in the local database we store it there.
            for (note in toBeSynced) {
                if (!local.contains(note)) {
                    var images = listOf<String>()
                    if (note.images != null) images = downloadImages(note)

                    database.noteDAO().insert(
                        Note(
                            note.title,
                            note.body,
                            note.date,
                            authorId,
                            images.ifEmpty { null })
                    )
                }
            }
        }
        val deletionJob = viewModelScope.launch(Dispatchers.IO) {
            deleteUnwantedNotes(local, toBeDeleted, authorId)
        }
        // The collection of jobs to track their progress and
        // trigger when they complete for the UI state.
        val jobs = listOf(
            onlineJob,
            offlineJob,
            deletionJob
        )
        viewModelScope.launch {
            jobs.joinAll()
            // This line will never be executed until the jobs are done.
            _finishedLoadingState.value = true
        }
    }

    /**
     * Deleting the notes that user didn't select from Room or from Firebase
     * depending on where this note exists.
     */
    private fun deleteUnwantedNotes(local: List<Note>, toBeRemoved: Set<Note>, authorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val reference = FirebaseDatabase.getInstance().getReference("Users")
                .child(authorId).child("Notes")

            // Fetching the user's notes from firebase.
            val snapshot = reference.get().await()

            // This brute force searches for unwanted notes in the
            // online data snapshot.
            for (note in toBeRemoved) {
                for (dataset in snapshot.children) {
                    // Create the note object from the data snapshot
                    val dataShot = dataset.getValue(Note::class.java)!!

                    // If we find a match, delete it.
                    if (dataShot == note) {
                        reference.child(dataset.key.toString()).removeValue()

                        // Check if the user had images on this note and delete them
                        // if found for better storage.
                        if (note.images != null){
                            for (count in 0 until note.images!!.size){
                                FirebaseStorage.getInstance()
                                    .getReference("images/" + note.title + note.body + count.toString())
                                    .delete().await()
                            }
                        }
                        break
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            // Deleting notes from the Room database.
            for (note in toBeRemoved)
                if (local.contains(note))
                    database.noteDAO().deleteNote(note.id)
        }
    }

    /**
     * Upload images to Firebase Storage and fetch their download url
     * and then return them as a list of String to add them to
     * the Note object in Firebase realtime database.
     */
    private fun getUrls(note: Note): List<String> {
        val urls = mutableListOf<String>()
        val tasks = mutableListOf<UploadTask>()
        // Create the images root folder instance in the Firebase storage.
        val folder = FirebaseStorage.getInstance().getReference("images")

        // For each image, create its UploadTask to track later.
        for (count in 0 until note.images!!.size) {
            val task = folder.child(note.title + note.body + count.toString())
                .putFile(Uri.parse(note.images!![count]))
            tasks.add(task)
        }

        // Blocking the thread to track the UploadTasks and once they complete,
        // store their download url.
        runBlocking(Dispatchers.Unconfined) {
            tasks.forEach {
                urls.add(it.await().storage.downloadUrl.await().toString())
            }
        }
        return urls
    }

    /**
     * Fetch images from Firebase Storage and store them locally and
     * then return their uris as a list of strings to register their
     * location in the Room database.
     */
    private fun downloadImages(note: Note): List<String> {
        val uris = mutableListOf<String>()
        // Create the images root folder instance in the Firebase storage.
        val storage = FirebaseStorage.getInstance().reference

        // For each image, download the file and store it locally.
        for (count in 0 until note.images!!.size){
            val file = storage.child("images/" + note.title + note.body + count.toString())
            val localFile = File.createTempFile(note.title + note.body + count.toString(), "")

            // Blocking the thread to track the downloading process and once
            // They are downloaded, store their uri.
            runBlocking(Dispatchers.Unconfined) {
                file.getFile(localFile).await()
                uris.add(localFile.toURI().toString())
            }
        }
        return uris
    }
}