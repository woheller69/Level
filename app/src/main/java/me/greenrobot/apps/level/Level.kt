package me.greenrobot.apps.level

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import me.greenrobot.apps.level.orientation.Orientation
import me.greenrobot.apps.level.orientation.OrientationListener
import me.greenrobot.apps.level.orientation.OrientationProvider
import me.greenrobot.apps.level.util.PreferenceHelper
import me.greenrobot.apps.level.view.LevelView
import me.greenrobot.apps.level.view.RulerView
import me.greenrobot.apps.level.view.VerticalSeekBar

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
class Level : AppCompatActivity(), OrientationListener {
    private var levelView: LevelView? = null
    private var rulerView: RulerView? = null
    private var rulerCalView: VerticalSeekBar? = null
    private var rulerCoarseCalView: VerticalSeekBar? = null
    private var rulerResetButtonView: AppCompatImageButton? = null

    /**
     * Gestion du son
     */
    private var soundPool: SoundPool? = null
    private var soundEnabled = false
    private var bipSoundID = 0
    private var bipRate = 0
    private var lastBip: Long = 0
    private var context: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContentView(R.layout.main)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        levelView = findViewById(R.id.main_levelView)

        // sound
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
                .build()
        } else {
            SoundPool(1, AudioManager.STREAM_RING, 0)
        }

        bipSoundID = soundPool!!.load(this, R.raw.bip, 1)
        bipRate = resources.getInteger(R.integer.bip_rate)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.menu_ruler).setChecked(rulerView != null)
        menu.findItem(R.id.menu_ruler)
            .setIcon(if (rulerView == null) R.drawable.ic_ruler else R.drawable.ic_bubble)
        menu.findItem(R.id.menu_settings).setVisible(rulerView == null)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        return true
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        if (rulerView != null) {
            setFullscreenMode()
        }

        super.onPanelClosed(featureId, menu)
    }

    /* Handles item selections */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_calibrate) {
            if (rulerView == null) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.calibrate_title)
                    .setIcon(null)
                    .setCancelable(true)
                    .setPositiveButton(R.string.calibrate) { dialog: DialogInterface?, id: Int -> provider!!.saveCalibration() }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setNeutralButton(R.string.reset) { dialog: DialogInterface?, id: Int -> provider!!.resetCalibration() }
                    .setMessage(R.string.calibrate_message)
                    .show()
            } else {
                val sp = PreferenceManager.getDefaultSharedPreferences(this)
                val progress = sp.getInt("pref_rulercal", 100)
                val coarseprogress = sp.getInt("pref_rulercoarsecal", 2000)
                val rulerLayout = findViewById<View>(R.id.main_layout) as RelativeLayout
                if (rulerCalView == null) {
                    Toast.makeText(
                        this,
                        getString(R.string.calibrate) + " \u25b2\u25bc",
                        Toast.LENGTH_LONG
                    ).show()
                    rulerCalView = VerticalSeekBar(this)
                    rulerCalView!!.max = 200
                    rulerCalView!!.progress = progress
                    rulerCalView!!.thumb = ContextCompat.getDrawable(context!!, R.drawable.ic_fine)
                    rulerCalView!!.thumb.setColorFilter(
                        getThemeColor(context, R.attr.colorAccent),
                        PorterDuff.Mode.MULTIPLY
                    )
                    val layoutParams = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    layoutParams.setMargins(rulerLayout.width * 3 / 8, 0, 0, 0)
                    rulerCalView!!.layoutParams = layoutParams
                    rulerCoarseCalView = VerticalSeekBar(this)
                    rulerCoarseCalView!!.max = 6000
                    rulerCoarseCalView!!.progress = coarseprogress
                    rulerCoarseCalView!!.thumb =
                        ContextCompat.getDrawable(context!!, R.drawable.ic_coarse)
                    rulerCoarseCalView!!.thumb.setColorFilter(
                        getThemeColor(
                            context,
                            R.attr.colorAccent
                        ), PorterDuff.Mode.MULTIPLY
                    )
                    val layoutParams2 = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    layoutParams2.setMargins(0, 0, rulerLayout.width * 3 / 8, 0)
                    layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    rulerCoarseCalView!!.layoutParams = layoutParams2

                    rulerResetButtonView = AppCompatImageButton(this)
                    val layoutParams3 = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams3.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                    layoutParams3.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                    rulerResetButtonView!!.layoutParams = layoutParams3
                    rulerResetButtonView!!.setImageResource(R.drawable.ic_reset)
                    rulerLayout.addView(rulerResetButtonView)
                    rulerLayout.addView(rulerCalView)
                    rulerLayout.addView(rulerCoarseCalView)

                    rulerCalView!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            rulerView!!.setCalib(
                                getDpmmCal(
                                    progress,
                                    rulerCoarseCalView!!.progress
                                ).toDouble(),
                                getDpmmCal(progress, rulerCoarseCalView!!.progress) * 25.4 / 32
                            )
                            val editor = sp.edit()
                            editor.putInt("pref_rulercal", progress)
                            editor.apply()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    })
                    rulerCoarseCalView!!.setOnSeekBarChangeListener(object :
                        OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            rulerView!!.setCalib(
                                getDpmmCal(rulerCalView!!.progress, progress).toDouble(),
                                getDpmmCal(
                                    rulerCalView!!.progress, progress
                                ) * 25.4 / 32
                            )
                            val editor = sp.edit()
                            editor.putInt("pref_rulercoarsecal", progress)
                            editor.apply()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    })
                    rulerResetButtonView!!.setOnClickListener { view: View? ->
                        rulerCalView!!.progress = 100
                        rulerCoarseCalView!!.progress = 2000
                    }
                } else {
                    rulerLayout.removeView(rulerCalView)
                    rulerLayout.removeView(rulerCoarseCalView)
                    rulerLayout.removeView(rulerResetButtonView)
                    rulerCalView = null
                    rulerCoarseCalView = null
                    rulerResetButtonView = null
                }
            }

            return true
        } else if (item.itemId == R.id.menu_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        } else if (item.itemId == R.id.menu_about) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/penkzhou/level")
                )
            )
            recreate() //fix strange action bar position when coming from ruler
            return true
        } else if (item.itemId == R.id.menu_ruler) {
            showRuler(!item.isChecked)
        }
        return false
    }

    private fun showRuler(ruler: Boolean) {
        if (ruler) {
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val rulerLayout = findViewById<View>(R.id.main_layout) as RelativeLayout
            val progress = sp.getInt("pref_rulercal", 100)
            val coarseProgress = sp.getInt("pref_rulercoarsecal", 2000)
            val dpmm = getDpmmCal(progress, coarseProgress)

            rulerView = RulerView(this, dpmm.toDouble(), dpmm * 25.4 / 32)
            rulerView!!.setBackgroundColor(ContextCompat.getColor(this, R.color.silver))
            rulerLayout.addView(rulerView)
            levelView!!.visibility = View.INVISIBLE
            setFullscreenMode()
            invalidateOptionsMenu()
        } else {
            levelView!!.visibility = View.VISIBLE
            val rulerLayout = findViewById<View>(R.id.main_layout) as RelativeLayout
            if (rulerView != null) rulerLayout.removeView(rulerView)
            rulerView = null
            if (rulerCalView != null) rulerLayout.removeView(rulerCalView)
            rulerCalView = null
            if (rulerCoarseCalView != null) rulerLayout.removeView(rulerCoarseCalView)
            rulerCoarseCalView = null
            if (rulerResetButtonView != null) rulerLayout.removeView(rulerResetButtonView)
            rulerResetButtonView = null
            window.decorView.systemUiVisibility = 0
            invalidateOptionsMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        provider = OrientationProvider.getInstance(this)
        // chargement des effets sonores
        soundEnabled = PreferenceHelper.soundEnabled
        // orientation manager
        if (provider!!.isSupported!!) {
            provider!!.startListening(this)
        } else {
            Toast.makeText(this, getText(R.string.not_supported), Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        showRuler(false)
        if (provider?.isListening == true) {
            provider?.stopListening()
        }
    }

    public override fun onDestroy() {
        if (soundPool != null) {
            soundPool?.release()
        }
        super.onDestroy()
    }

    override fun onOrientationChanged(
        orientation: Orientation?,
        pitch: Float,
        roll: Float,
        balance: Float
    ) {
        if ((soundEnabled
                    && orientation!!.isLevel(
                pitch,
                roll,
                balance,
                provider!!.sensibility
            )) && System.currentTimeMillis() - lastBip > bipRate
        ) {
            val mgr = getSystemService(AUDIO_SERVICE) as AudioManager
            val streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_RING).toFloat()
            val streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_RING).toFloat()
            val volume = streamVolumeCurrent / streamVolumeMax
            lastBip = System.currentTimeMillis()
            soundPool!!.play(bipSoundID, volume, volume, 1, 0, 1f)
        }
        levelView!!.onOrientationChanged(orientation, pitch, roll, balance)
    }

    override fun onCalibrationReset(success: Boolean) {
        Toast.makeText(
            this, if (success) R.string.calibrate_restored else R.string.calibrate_failed,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCalibrationSaved(success: Boolean) {
        Toast.makeText(
            this, if (success) R.string.calibrate_saved else R.string.calibrate_failed,
            Toast.LENGTH_LONG
        ).show()
    }

    fun getDpmmCal(progress: Int, coarseProgress: Int): Float {
        val dpmm = (resources.displayMetrics.ydpi / 25.4).toFloat()
        return dpmm * (1 + (progress + coarseProgress - 100f - 2000f) / 5000f)
    }

    private fun setFullscreenMode() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }


    companion object {
        @JvmStatic
        var provider: OrientationProvider? = null
            private set

        fun getThemeColor(context: Context?, colorResId: Int): Int {
            val typedValue = TypedValue()
            val typedArray =
                context!!.obtainStyledAttributes(typedValue.data, intArrayOf(colorResId))
            val color = typedArray.getColor(0, 0)
            typedArray.recycle()
            return color
        }
    }
}
