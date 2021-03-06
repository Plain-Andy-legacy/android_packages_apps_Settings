/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2015 Plain-Andy-legacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Toast;
import android.widget.TextView;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;

public class PlainTweakInfo extends SettingsPreferenceFragment implements Indexable, Preference.OnPreferenceChangeListener {

    private static final String LOG_TAG = "PlainTweakInfo";
    
    private static final String KEY_PLAINTWEAK_GOV = "plain_tweak_gov";
    private static final String KEY_PLAINTWEAK_GOV2 = "plain_tweak_gov2";
    private static final String KEY_PLAINTWEAK_MAX = "plain_tweak_maxkhz";
    private static final String KEY_PLAINTWEAK_MAX2 = "plain_tweak_maxkhz2";
    private static final String KEY_PLAINTWEAK_MIN = "plain_tweak_minkhz";
    private static final String KEY_PLAINTWEAK_MIN2 = "plain_tweak_minkhz2";
    private static final String KEY_PLAINTWEAK_SCHED = "plain_tweak_scheduler";
    private static final String KEY_PLAINTWEAK_TCP = "plain_tweak_tcpcong";
    private static final String KEY_STOCK_GOV = "stock_gov";
    private static final String KEY_STOCK_SCHED = "stock_scheduler";
    private static final String KEY_STOCK_MAX = "stock_maxkhz";
    private static final String KEY_STOCK_MIN = "stock_minkhz";
    private static final String KEY_STOCK_TCP = "stock_tcpcong";
    private static final String KEY_MOD_CURRENT_DENSITY = "current_density";
    private static final String KEY_MOD_STOCK_DENSITY = "stock_density";
    private static final String KEY_MOD_CUSTOM_DENSITY = "custom_density";
    private static final String KEY_NOTIFY_PLAINTWEAK = "notify_plaintweak";
    private ListPreference mPlainTweakNotify;
            	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.plaintweak_info);
                               		
        setValueSummary(KEY_MOD_CURRENT_DENSITY, "customdensity");
        setValueSummary(KEY_MOD_STOCK_DENSITY, "ro.sf.lcd_density");
        setValueSummary(KEY_PLAINTWEAK_SCHED, "scheduler");
        setValueSummary(KEY_PLAINTWEAK_GOV, "gov");
        setValueSummary(KEY_PLAINTWEAK_GOV2, "gov2");
        setValueSummary(KEY_PLAINTWEAK_MAX, "maxkhz");
        setValueSummary(KEY_PLAINTWEAK_MAX2, "maxkhz2");
        setValueSummary(KEY_PLAINTWEAK_MIN, "minkhz");
        setValueSummary(KEY_PLAINTWEAK_MIN2, "minkhz2");
        setValueSummary(KEY_PLAINTWEAK_TCP, "tcpcong");
        setValueSummary(KEY_STOCK_GOV, "stockgov");
        setValueSummary(KEY_STOCK_SCHED, "stockscheduler");
        setValueSummary(KEY_STOCK_MAX, "stockmaxkhz");
        setValueSummary(KEY_STOCK_MIN, "stockminkhz");
        setValueSummary(KEY_STOCK_TCP, "stocktcpcong");
           
	}
	
	@Override
    public void onResume() {
		super.onResume();
		mPlainTweakNotify = (ListPreference) findPreference(KEY_NOTIFY_PLAINTWEAK);
		int notifyValue = Settings.System.getInt(getContentResolver(),
                   Settings.System.PLAIN_TWEAK_NOTIFICATIONS, 0);
		mPlainTweakNotify.setValueIndex(notifyValue);
		mPlainTweakNotify.setSummary(mPlainTweakNotify.getEntries()[notifyValue]);
		mPlainTweakNotify.setOnPreferenceChangeListener(this);
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getActivity();
        String key = preference.getKey();
        if (KEY_NOTIFY_PLAINTWEAK.equals(key)) {
            int notifyValue = Integer.valueOf((String) newValue);
            int index = mPlainTweakNotify.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.PLAIN_TWEAK_NOTIFICATIONS,
                    notifyValue);
            mPlainTweakNotify.setSummary(mPlainTweakNotify.getEntries()[index]);
            getActivity().sendBroadcast(new Intent("PLAIN_TWEAK_NOTIFICATIONS"));         
        }
        return true; 		
    }
    


    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals("")) {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void removePreferenceIfBoolFalse(String preference, int resId) {
        if (!getResources().getBoolean(resId)) {
            Preference pref = findPreference(preference);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void addStringPreference(String key, String value) {
        if (value != null) {
            setStringSummary(key, value);
        } else {
            getPreferenceScreen().removePreference(findPreference(key));
        }
    }
}

