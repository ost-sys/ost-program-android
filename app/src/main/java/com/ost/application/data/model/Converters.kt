package com.ost.application.data.model

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringSet(set: Set<String>): String {
        return set.joinToString(separator = ",")
    }

    @TypeConverter
    fun toStringSet(data: String): Set<String> {
        return if (data.isEmpty()) {
            emptySet()
        } else {
            data.split(",").toSet()
        }
    }
}