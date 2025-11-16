package com.ulcer.care.detection.ulcare

import android.content.Context
import androidx.core.content.edit

object UserPrefs {
    private const val FILE = "user_prefs"
    private const val KEY_AGE = "age"
    private const val KEY_GENDER = "gender" // "L" atau "P"

    fun saveIdentity(context: Context, age: Int, gender: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putInt(KEY_AGE, age)
            putString(KEY_GENDER, gender)
        }
    }

    fun getAge(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_AGE, -1)

    fun getGender(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_GENDER, null)

    fun hasIdentity(context: Context): Boolean =
        getAge(context) > 0 && !getGender(context).isNullOrBlank()
}