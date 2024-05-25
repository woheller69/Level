package me.greenrobot.apps.level

import android.app.Application
import me.greenrobot.apps.level.util.PreferenceHelper

class LevelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.initPrefs(this)
    }
}
