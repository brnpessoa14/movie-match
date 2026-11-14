package br.com.cinematch

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class CineMatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Identidade visual escura também nos componentes criados por código.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
