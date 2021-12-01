package org.woheller69.level;

import android.app.Application;

import org.woheller69.level.util.PreferenceHelper;

public class LevelApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceHelper.initPrefs(this);
    }
}
