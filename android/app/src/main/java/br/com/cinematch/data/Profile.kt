package br.com.cinematch.data

import android.content.Context

class Profile(context: Context) {
    private val preferences = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    val id: Int get() = preferences.getInt("user_id", 0)
    val ready: Boolean get() = preferences.getBoolean("ready", false)
    fun save(user: User) { preferences.edit().putInt("user_id", user.id).putString("name", user.name).apply() }
    fun complete() { preferences.edit().putBoolean("ready", true).apply() }
    fun clear() { preferences.edit().clear().apply() }
}
