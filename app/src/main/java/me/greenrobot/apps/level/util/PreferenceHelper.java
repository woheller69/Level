package me.greenrobot.apps.level.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

public class PreferenceHelper {
    private static SharedPreferences sharedPrefs;
    private static Resources mRes;

    public static void initPrefs(final Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mRes = context.getResources();
    }

    private static String getPrefKey(@StringRes final int prefKey) {
        return mRes.getString(prefKey);
    }

    private static boolean getHelperBoolean(@StringRes final int prefKey, final boolean defValue) {
        return sharedPrefs.getBoolean(getPrefKey(prefKey), defValue);
    }

    private static String getHelperString(@StringRes final int prefKey, final int defValue) {
        return sharedPrefs.getString(getPrefKey(prefKey), getPrefKey(defValue));
    }

    /**
     * Compares the preference's current value to the called prefKey.
     *
     * @param currentKeyValue the current key string value of a prefKey
     * @param prefKey         a passed prefKey
     * @return true if the prefKey's current value is equal to the passed prefKey, false otherwise
     */
    private static boolean getEquals(final String currentKeyValue, @StringRes final int prefKey) {
        return currentKeyValue.equals(getPrefKey(prefKey));
    }

    public static boolean getShowAngle() {
        return getHelperBoolean(PrefKeys.PREF_SHOW_ANGLE, true);
    }

    public static String getDisplayType() {
        return getHelperString(PrefKeys.PREF_DISPLAY_TYPE, PrefKeys.PREF_DISPLAY_TYPE_ANGLE);
    }

    public static boolean isDisplayTypeInclination() {
        return getEquals(getDisplayType(), PrefKeys.PREF_DISPLAY_TYPE_INCLINATION);
    }

    public static String getDisplayTypeFormat() {
        // format of default display type (angle)
        String format = "00.0\u2009°";
        if (isDisplayTypeInclination()) {
            format = "000.0\u2009'%'";
        }
        return format;
    }

    public static String getDisplayTypeBackgroundText() {
        // background text of default display type (angle)
        String backgroundText = "88.8\u2009°";
        if (isDisplayTypeInclination()) {
            backgroundText = "888.8\u2009%";
        }
        return backgroundText;
    }

    public static float getDisplayTypeMax() {
        // max of default display type (angle)
        float max = 99.9f;
        if (isDisplayTypeInclination()) {
            max = 999.9f;
        }
        return max;
    }

    public static boolean getOrientationLocked() {
        return getHelperBoolean(PrefKeys.PREF_LOCK_ORIENTATION, false);
    }

    private static String getViscosity() {
        return getHelperString(PrefKeys.PREF_VISCOSITY, PrefKeys.PREF_VISCOSITY_MEDIUM);
    }

    public static boolean isViscosityLow() {
        return getEquals(getViscosity(), PrefKeys.PREF_VISCOSITY_LOW);
    }

    public static boolean isViscosityHigh() {
        return getEquals(getViscosity(), PrefKeys.PREF_VISCOSITY_HIGH);
    }

    public static double getViscosityCoefficient() {
        // coefficient of default viscosity (medium)
        double coeff = 0.4d;
        if (isViscosityLow()) {
            coeff = 0.6d;
        } else if (isViscosityHigh()) {
            coeff = 0.2d;
        }
        return coeff;
    }

    public static boolean getSoundEnabled() {
        return getHelperBoolean(PrefKeys.PREF_ENABLE_SOUND, false);
    }
}
