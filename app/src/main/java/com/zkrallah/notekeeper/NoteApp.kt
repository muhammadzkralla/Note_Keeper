package com.zkrallah.notekeeper

import android.app.Application

class NoteApp : Application(){

    companion object {
        lateinit var ctx: Application
    }

    override fun onCreate() {
        ctx = this
        super.onCreate()
    }
}