package org.woheller69.level;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.ContextCompat;

import org.woheller69.level.orientation.Orientation;
import org.woheller69.level.orientation.OrientationListener;
import org.woheller69.level.orientation.OrientationProvider;
import org.woheller69.level.util.PreferenceHelper;
import org.woheller69.level.view.LevelView;
import org.woheller69.level.view.RulerView;

/*
 *  This file is part of Level (an Android Bubble Level).
 *  <https://github.com/avianey/Level>
 *
 *  Copyright (C) 2014 Antoine Vianey
 *
 *  Level is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Level is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Level. If not, see <http://www.gnu.org/licenses/>
 */
public class Level extends AppCompatActivity implements OrientationListener {

    private static Level CONTEXT;

    private OrientationProvider provider;

    private LevelView levelView;
    private RulerView rulerView;

    /**
     * Gestion du son
     */
    private SoundPool soundPool;
    private boolean soundEnabled;
    private int bipSoundID;
    private int bipRate;
    private long lastBip;

    public static Level getContext() {
        return CONTEXT;
    }

    public static OrientationProvider getProvider() {
        return getContext().provider;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        CONTEXT = this;
        levelView = findViewById(R.id.main_levelView);
        // sound

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                    .build())
                    .build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
        }

        bipSoundID = soundPool.load(this, R.raw.bip, 1);
        bipRate = getResources().getInteger(R.integer.bip_rate);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.menu_ruler).setChecked(rulerView!=null);
        menu.findItem(R.id.menu_calibrate).setVisible(rulerView==null);
        menu.findItem(R.id.menu_settings).setVisible(rulerView==null);
        menu.findItem(R.id.menu_about).setVisible(rulerView==null);
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_calibrate) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.calibrate_title)
                    .setIcon(null)
                    .setCancelable(true)
                    .setPositiveButton(R.string.calibrate, (dialog, id) -> provider.saveCalibration())
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.reset, (dialog, id) -> provider.resetCalibration())
                    .setMessage(R.string.calibrate_message)
                    .show();
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/level")));
            return true;
        } else if (item.getItemId() == R.id.menu_ruler) {
            showRuler(!item.isChecked());
        }
        return false;
    }

    private void showRuler(boolean ruler) {
        if (ruler){
            RelativeLayout rulerLayout = (RelativeLayout) findViewById(R.id.main_layout);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            float dpmm =  (float) (displayMetrics.ydpi/25.4);

            rulerView = new RulerView(this, dpmm, dpmm*25.4/32);
            rulerView.setBackgroundColor(ContextCompat.getColor(this,R.color.silver));
            rulerLayout.addView(rulerView);
            levelView.setVisibility(View.INVISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LOW_PROFILE|
                                View.SYSTEM_UI_FLAG_FULLSCREEN|
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LOW_PROFILE|
                                View.SYSTEM_UI_FLAG_FULLSCREEN|
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            }
            invalidateOptionsMenu();
        } else {
            levelView.setVisibility(View.VISIBLE);
            RelativeLayout rulerLayout = (RelativeLayout) findViewById(R.id.main_layout);
            if (rulerView!=null) rulerLayout.removeView(rulerView);
            rulerView = null;
            getWindow().getDecorView().setSystemUiVisibility(0);
            invalidateOptionsMenu();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Level", "Level resumed");
        provider = OrientationProvider.getInstance();
        // chargement des effets sonores
        soundEnabled = PreferenceHelper.getSoundEnabled();
        // orientation manager
        if (provider.isSupported()) {
            provider.startListening(this);
        } else {
            Toast.makeText(this, getText(R.string.not_supported), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        showRuler(false);
        if (provider.isListening()) {
            provider.stopListening();
        }
    }

    @Override
    public void onDestroy() {
        if (soundPool != null) {
            soundPool.release();
        }
        super.onDestroy();
    }

    @Override
    public void onOrientationChanged(Orientation orientation, float pitch, float roll, float balance) {
        if (soundEnabled
                && orientation.isLevel(pitch, roll, balance, provider.getSensibility())
                && System.currentTimeMillis() - lastBip > bipRate) {
            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_RING);
            float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_RING);
            float volume = streamVolumeCurrent / streamVolumeMax;
            lastBip = System.currentTimeMillis();
            soundPool.play(bipSoundID, volume, volume, 1, 0, 1);
        }
        levelView.onOrientationChanged(orientation, pitch, roll, balance);
    }

    @Override
    public void onCalibrationReset(boolean success) {
        Toast.makeText(this, success ?
                        R.string.calibrate_restored : R.string.calibrate_failed,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCalibrationSaved(boolean success) {
        Toast.makeText(this, success ?
                        R.string.calibrate_saved : R.string.calibrate_failed,
                Toast.LENGTH_LONG).show();
    }
}
