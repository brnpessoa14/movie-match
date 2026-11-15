package br.com.cinematch.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Appearance {
    val labels = arrayOf("Sistema", "Claro", "Escuro")
    private val modes = intArrayOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)
    fun selected(context: Context) = context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("choice", 0).coerceIn(0, 2)
    fun apply(context: Context) = AppCompatDelegate.setDefaultNightMode(modes[selected(context)])
    fun save(context: Context, choice: Int) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putInt("choice", choice.coerceIn(0, 2)).apply()
        apply(context)
    }
}
