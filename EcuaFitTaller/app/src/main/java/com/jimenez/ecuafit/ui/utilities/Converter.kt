package com.jimenez.ecuafit.ui.utilities
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class Converter {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return Gson().toJson(list)
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @TypeConverter
    fun toTimestamp(value: String?): Date? {
        return value?.let { dateFormat.parse(value) }
    }

    @TypeConverter
    fun fromTimestamp(date: Date?): String? {
        return date?.let { dateFormat.format(date) }
    }
}