package com.zkrallah.notekeeper

class RandomQuote {
    private val quotesList = listOf(
        "Doing the magic !",
        "Do you know the Earth is round ?",
        "I'm sorry for making you wait :(",
        "Do you enjoy this app?",
        "You know I'm better than Google keep right ?",
        "I think you are patient, right ?",
        "Just wait a sec :D",
        "Did you sleep will today ?"
    )

    val randomQuote = quotesList.random()
}