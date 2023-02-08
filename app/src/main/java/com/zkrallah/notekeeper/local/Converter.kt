package com.zkrallah.notekeeper.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converter {
    @TypeConverter
    fun listOfStringToString(list: List<String>?): String? {
        return list?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun stringToListOfString(string: String?): List<String>? {
        val listType = object : TypeToken<List<String?>?>() {}.type
        return string?.let { Gson().fromJson(it, listType) }
    }
}