package br.com.cinematch

import android.app.Application
import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import br.com.cinematch.data.Profile
import br.com.cinematch.data.Appearance

class CineMatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Appearance.apply(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0
            override fun onActivityStarted(activity: Activity) { started++ }
            override fun onActivityStopped(activity: Activity) {
                started--
                if (started == 0 && !activity.isChangingConfigurations && !Profile.externalFlow) Profile.clearSession()
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
