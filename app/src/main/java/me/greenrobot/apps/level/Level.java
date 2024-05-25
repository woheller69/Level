package me.greenrobot.apps.level;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.woheller69.level.R;

import me.greenrobot.apps.level.orientation.Orientation;
import me.greenrobot.apps.level.orientation.OrientationListener;
import me.greenrobot.apps.level.orientation.OrientationProvider;
import me.greenrobot.apps.level.util.PreferenceHelper;
import me.greenrobot.apps.level.view.LevelView;
import me.greenrobot.apps.level.view.RulerView;
import me.greenrobot.apps.level.view.VerticalSeekBar;

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

    private static OrientationProvider provider;

    private LevelView levelView;
    private RulerView rulerView;
    private VerticalSeekBar rulerCalView;
    private VerticalSeekBar rulerCoarseCalView;
    private AppCompatImageButton rulerResetButtonView;

    /**
     * Gestion du son
     */
    private SoundPool soundPool;
    private boolean soundEnabled;
    private int bipSoundID;
    private int bipRate;
    private long lastBip;
    private Context context;

    public static OrientationProvider getProvider() {
        return provider;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        menu.findItem(R.id.menu_ruler).setIcon(rulerView==null ? R.drawable.ic_ruler : R.drawable.ic_bubble);
        menu.findItem(R.id.menu_settings).setVisible(rulerView==null);
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        return true;
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        if (rulerView != null) {
            setFullscreenMode();
        }

        super.onPanelClosed(featureId, menu);
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_calibrate) {
            if (rulerView==null){
                new AlertDialog.Builder(this)
                        .setTitle(R.string.calibrate_title)
                        .setIcon(null)
                        .setCancelable(true)
                        .setPositiveButton(R.string.calibrate, (dialog, id) -> provider.saveCalibration())
                        .setNegativeButton(android.R.string.cancel, null)
                        .setNeutralButton(R.string.reset, (dialog, id) -> provider.resetCalibration())
                        .setMessage(R.string.calibrate_message)
                        .show();
            } else {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                int progress = sp.getInt("pref_rulercal",100);
                int coarseprogress = sp.getInt("pref_rulercoarsecal",2000);
                RelativeLayout rulerLayout = (RelativeLayout) findViewById(R.id.main_layout);
                if (rulerCalView == null){
                    Toast.makeText(this,getString(R.string.calibrate)+" \u25b2\u25bc", Toast.LENGTH_LONG).show();
                    rulerCalView = new VerticalSeekBar(this);
                    rulerCalView.setMax(200);
                    rulerCalView.setProgress(progress);
                    rulerCalView.setThumb(ContextCompat.getDrawable(context, R.drawable.ic_fine));
                    rulerCalView.getThumb().setColorFilter(getThemeColor(context,R.attr.colorAccent), PorterDuff.Mode.MULTIPLY);
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(rulerLayout.getWidth()*3/8,0,0,0);
                    rulerCalView.setLayoutParams(layoutParams);
                    rulerCoarseCalView = new VerticalSeekBar(this);
                    rulerCoarseCalView.setMax(6000);
                    rulerCoarseCalView.setProgress(coarseprogress);
                    rulerCoarseCalView.setThumb(ContextCompat.getDrawable(context, R.drawable.ic_coarse));
                    rulerCoarseCalView.getThumb().setColorFilter(getThemeColor(context,R.attr.colorAccent), PorterDuff.Mode.MULTIPLY);
                    RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    layoutParams2.setMargins(0,0,rulerLayout.getWidth()*3/8,0);
                    layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    rulerCoarseCalView.setLayoutParams(layoutParams2);

                    rulerResetButtonView = new AppCompatImageButton(this);
                    RelativeLayout.LayoutParams layoutParams3 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams3.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                    layoutParams3.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                    rulerResetButtonView.setLayoutParams(layoutParams3);
                    rulerResetButtonView.setImageResource(R.drawable.ic_reset);
                    rulerLayout.addView(rulerResetButtonView);
                    rulerLayout.addView(rulerCalView);
                    rulerLayout.addView(rulerCoarseCalView);

                    rulerCalView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            rulerView.setCalib(getDpmmCal(progress,rulerCoarseCalView.getProgress()), getDpmmCal(progress,rulerCoarseCalView.getProgress())*25.4/32);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putInt("pref_rulercal",progress);
                            editor.apply();
                        }
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {}
                    });
                    rulerCoarseCalView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            rulerView.setCalib(getDpmmCal(rulerCalView.getProgress(),progress), getDpmmCal(rulerCalView.getProgress(),progress)*25.4/32);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putInt("pref_rulercoarsecal",progress);
                            editor.apply();
                        }
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {}
                    });
                    rulerResetButtonView.setOnClickListener(view -> {
                        rulerCalView.setProgress(100);
                        rulerCoarseCalView.setProgress(2000);
                    });
                } else {
                    rulerLayout.removeView(rulerCalView);
                    rulerLayout.removeView(rulerCoarseCalView);
                    rulerLayout.removeView(rulerResetButtonView);
                    rulerCalView = null;
                    rulerCoarseCalView = null;
                    rulerResetButtonView = null;
                }
            }

            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/level")));
            recreate(); //fix strange action bar position when coming from ruler
            return true;
        } else if (item.getItemId() == R.id.menu_ruler) {
            showRuler(!item.isChecked());
        }
        return false;
    }

    private void showRuler(boolean ruler) {
        if (ruler){
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            RelativeLayout rulerLayout = (RelativeLayout) findViewById(R.id.main_layout);
            int progress = sp.getInt("pref_rulercal",100);
            int coarseProgress = sp.getInt("pref_rulercoarsecal",2000);
            float dpmm =  getDpmmCal(progress,coarseProgress);

            rulerView = new RulerView(this, dpmm, dpmm*25.4/32);
            rulerView.setBackgroundColor(ContextCompat.getColor(this,R.color.silver));
            rulerLayout.addView(rulerView);
            levelView.setVisibility(View.INVISIBLE);
            setFullscreenMode();
            invalidateOptionsMenu();
        } else {
            levelView.setVisibility(View.VISIBLE);
            RelativeLayout rulerLayout = (RelativeLayout) findViewById(R.id.main_layout);
            if (rulerView!=null) rulerLayout.removeView(rulerView);
            rulerView = null;
            if (rulerCalView !=null) rulerLayout.removeView(rulerCalView);
            rulerCalView = null;
            if (rulerCoarseCalView !=null) rulerLayout.removeView(rulerCoarseCalView);
            rulerCoarseCalView = null;
            if (rulerResetButtonView !=null) rulerLayout.removeView(rulerResetButtonView);
            rulerResetButtonView = null;
            getWindow().getDecorView().setSystemUiVisibility(0);
            invalidateOptionsMenu();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        provider = OrientationProvider.getInstance(this);
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

    public float getDpmmCal(int progress, int coarseProgress){
        float dpmm =  (float) (getResources().getDisplayMetrics().ydpi/25.4);
        return dpmm*(1+(progress+coarseProgress-100f-2000f)/5000f);
    }

    public static int getThemeColor(Context context, int colorResId) {
        TypedValue typedValue = new TypedValue();
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, new int[] {colorResId});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    private void setFullscreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE|
                            View.SYSTEM_UI_FLAG_FULLSCREEN|
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
