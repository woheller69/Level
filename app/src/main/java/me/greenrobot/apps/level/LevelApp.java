package me.greenrobot.apps.level;

import android.app.Application;

import me.greenrobot.apps.level.util.PreferenceHelper;

public class LevelApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceHelper.initPrefs(this);
    }
}
