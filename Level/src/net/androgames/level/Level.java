package net.androgames.level;

import net.androgames.level.config.Provider;
import net.androgames.level.orientation.Orientation;
import net.androgames.level.orientation.OrientationListener;
import net.androgames.level.orientation.OrientationProvider;
import net.androgames.level.view.LevelView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/*
 *  This file is part of Level (an Android Bubble Level).
 *  <https://github.com/avianey/Level>
 *  
 *  Copyright (C) 2012 Antoine Vianey
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
public class Level extends Activity implements OrientationListener {
	
	private static Level CONTEXT;
	
	private static final int DIALOG_CALIBRATE_ID = 1;
	private static final int TOAST_DURATION = 10000;
	
	private OrientationProvider provider;
	
    private LevelView view;
    private WakeLock wakeLock;
    
	/** Gestion du son */
	private SoundPool soundPool;
	private boolean soundEnabled;
	private int bipSoundID;
	private int bipRate;
	private long lastBip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        CONTEXT = this;
        view = (LevelView) findViewById(R.id.level);
        // sound
    	soundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
    	bipSoundID = soundPool.load(this, R.raw.bip, 1);
    	bipRate = getResources().getInteger(R.integer.bip_rate);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}
    
    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.calibrate:
	            showDialog(DIALOG_CALIBRATE_ID);
	            return true;
	        case R.id.preferences:
	            startActivity(new Intent(this, LevelPreferences.class));
	            return true;
        }
        return false;
    }
    
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
	        case DIALOG_CALIBRATE_ID:
	        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        	builder.setTitle(R.string.calibrate_title)
	        			.setIcon(null)
	        			.setCancelable(true)
	        			.setPositiveButton(R.string.calibrate, new DialogInterface.OnClickListener() {
	        	           	public void onClick(DialogInterface dialog, int id) {
	        	        	   	provider.saveCalibration();
	        	           	}
	        			})
	        	       	.setNegativeButton(R.string.cancel, null)
	        	       	.setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
	        	           	public void onClick(DialogInterface dialog, int id) {
	        	           		provider.resetCalibration();
	        	           	}
	        	       	})
	        	       	.setMessage(R.string.calibrate_message);
	        	dialog = builder.create();
	            break;
	        default:
	            dialog = null;
        }
        return dialog;
    }
    
    protected void onResume() {
    	super.onResume();
    	Log.d("Level", "Level resumed");
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	provider = Provider.valueOf(
    			prefs.getString(LevelPreferences.KEY_SENSOR, 
    					LevelPreferences.PROVIDER_ACCELEROMETER)).getProvider();
    	// chargement des effets sonores
        soundEnabled = prefs.getBoolean(LevelPreferences.KEY_SOUND, false);
        // orientation manager
        if (provider.isSupported()) {
    		provider.startListening(this);
    	} else {
    		Toast.makeText(this, getText(R.string.not_supported), TOAST_DURATION).show();
    	}
        // wake lock
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(
        		PowerManager.SCREEN_BRIGHT_WAKE_LOCK, this.getClass().getName());
        wakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (provider.isListening()) {
        	provider.stopListening();
    	}
		wakeLock.release();
    }
    
    @Override
    public void onDestroy() {
		if (soundPool != null) {
			soundPool.release();
		}
		super.onDestroy();
    }

	@Override
	public void onOrientationChanged(Orientation orientation, float pitch, float roll) {
		if (soundEnabled 
				&& orientation.isLevel(pitch, roll, provider.getSensibility())
				&& System.currentTimeMillis() - lastBip > bipRate) {
			AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_RING);
			float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_RING);
			float volume = streamVolumeCurrent / streamVolumeMax;
			lastBip = System.currentTimeMillis();
			soundPool.play(bipSoundID, volume, volume, 1, 0, 1);
		}
		view.onOrientationChanged(orientation, pitch, roll);
	}

	@Override
	public void onCalibrationReset(boolean success) {
		Toast.makeText(this, success ? 
				R.string.calibrate_restored : R.string.calibrate_failed, 
				Level.TOAST_DURATION).show();
	}

	@Override
	public void onCalibrationSaved(boolean success) {
		Toast.makeText(this, success ? 
				R.string.calibrate_saved : R.string.calibrate_failed,
				Level.TOAST_DURATION).show();
	}

    public static Level getContext() {
		return CONTEXT;
	}
    
    public static OrientationProvider getProvider() {
    	return getContext().provider;
    }
    
}
