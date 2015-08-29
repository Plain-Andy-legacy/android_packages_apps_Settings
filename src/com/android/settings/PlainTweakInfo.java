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
import com.android.settings.util.CMDProcessor;

import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class PlainTweakInfo extends SettingsPreferenceFragment implements Indexable, Preference.OnPreferenceChangeListener {

    private static final String LOG_TAG = "PlainTweakInfo";

    private static final String KEY_PLAINTWEAK_GOV = "plain_tweak_gov";
    private static final String KEY_PLAINTWEAK_GOV2 = "plain_tweak_gov2";
    private static final String KEY_PLAINTWEAK_MAX = "plain_tweak_maxkhz";
    private static final String KEY_PLAINTWEAK_MAX2 = "plain_tweak_maxkhz2";
    private static final String KEY_PLAINTWEAK_MIN = "plain_tweak_minkhz";
    private static final String KEY_PLAINTWEAK_MIN2 = "plain_tweak_minkhz2";
    private static final String KEY_PLAINTWEAK_GOV3 = "plain_tweak_gov3";
    private static final String KEY_PLAINTWEAK_MAX3 = "plain_tweak_maxkhz3";
    private static final String KEY_PLAINTWEAK_MIN3 = "plain_tweak_minkhz3";
    private static final String KEY_PLAINTWEAK_GOV4 = "plain_tweak_gov4";
    private static final String KEY_PLAINTWEAK_MAX4 = "plain_tweak_maxkhz4";
    private static final String KEY_PLAINTWEAK_MIN4 = "plain_tweak_minkhz4";
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
    private CharSequence[] availableGovs;
    private CharSequence[] availableClockRate;
    private CharSequence[] availableTcpCong;
    private CharSequence[] availableScheduler;
    private boolean MsmLimiter = new File("/sys/kernel/msm_limiter").exists();
    private int mCores1st;
    private ListPreference mPlainTweakNotify;
    private ListPreference mPlainTweakGov;
    private ListPreference mPlainTweakGov2;
    private ListPreference mPlainTweakGov3;
    private ListPreference mPlainTweakGov4;
    private ListPreference mPlainTweakMin;
    private ListPreference mPlainTweakMin2;
    private ListPreference mPlainTweakMin3;
    private ListPreference mPlainTweakMin4;
    private ListPreference mPlainTweakMax;
    private ListPreference mPlainTweakMax2;
    private ListPreference mPlainTweakMax3;
    private ListPreference mPlainTweakMax4;
    private ListPreference mPlainTweakTCP;
    private ListPreference mPlainTweakScheduler;
    private File storage = new File(Environment.getExternalStorageDirectory());

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.plaintweak_info);
        availableGovs = ReadFileAsEntries("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors");
        availableClockRate = ReadFileAsEntries("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies");
        availableTcpCong = ReadFileAsEntries("/proc/sys/net/ipv4/tcp_available_congestion_control");
        availableScheduler = ReadFileAsEntries("/sys/block/mmcblk0/queue/scheduler");
        mCores1st = ReadCoresToInt("/sys/devices/system/cpu/possible"); //May need updated depending on architecture
        setSystemProp("persist.sys.extstorage", storage.getAbsolutePath());
        setAllSummaries();
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
    mPlainTweakGov = (ListPreference) findPreference(KEY_PLAINTWEAK_GOV);
    mPlainTweakGov.setEntries(availableGovs);
    mPlainTweakGov.setEntryValues(availableGovs);
    mPlainTweakGov.setOnPreferenceChangeListener(this);
    mPlainTweakMax = (ListPreference) findPreference(KEY_PLAINTWEAK_MAX);
    mPlainTweakMax.setEntries(availableClockRate);
    mPlainTweakMax.setEntryValues(availableClockRate);
    mPlainTweakMax.setOnPreferenceChangeListener(this);
    mPlainTweakMin = (ListPreference) findPreference(KEY_PLAINTWEAK_MIN);
    mPlainTweakMin.setEntries(availableClockRate);
    mPlainTweakMin.setEntryValues(availableClockRate);
    mPlainTweakMin.setOnPreferenceChangeListener(this);
    mPlainTweakGov2 = (ListPreference) findPreference(KEY_PLAINTWEAK_GOV2);
    mPlainTweakGov2.setEntries(availableGovs);
    mPlainTweakGov2.setEntryValues(availableGovs);
    mPlainTweakGov2.setOnPreferenceChangeListener(this);
    mPlainTweakMax2 = (ListPreference) findPreference(KEY_PLAINTWEAK_MAX2);
    mPlainTweakMax2.setEntries(availableClockRate);
    mPlainTweakMax2.setEntryValues(availableClockRate);
    mPlainTweakMax2.setOnPreferenceChangeListener(this);
    mPlainTweakMin2 = (ListPreference) findPreference(KEY_PLAINTWEAK_MIN2);
    mPlainTweakMin2.setEntries(availableClockRate);
    mPlainTweakMin2.setEntryValues(availableClockRate);
    mPlainTweakMin2.setOnPreferenceChangeListener(this);
    mPlainTweakScheduler = (ListPreference) findPreference(KEY_PLAINTWEAK_SCHED);
    mPlainTweakScheduler.setEntries(availableScheduler);
    mPlainTweakScheduler.setEntryValues(availableScheduler);
    mPlainTweakScheduler.setOnPreferenceChangeListener(this);
    mPlainTweakTCP = (ListPreference) findPreference(KEY_PLAINTWEAK_TCP);
    mPlainTweakTCP.setEntries(availableTcpCong);
    mPlainTweakTCP.setEntryValues(availableTcpCong);
    mPlainTweakTCP.setOnPreferenceChangeListener(this);
    String mCore3;
    String mCore4;
    if (MsmLimiter) {
      String mCore1 = "Core 1";
      String mCore2 = "Core 2";
      mCore3 = "Core 3";
      mCore4 = "Core 4";
      mPlainTweakGov3 = (ListPreference) findPreference(KEY_PLAINTWEAK_GOV3);
      mPlainTweakMax3 = (ListPreference) findPreference(KEY_PLAINTWEAK_MAX3);
      mPlainTweakMin3 = (ListPreference) findPreference(KEY_PLAINTWEAK_MIN3);
      mPlainTweakGov4 = (ListPreference) findPreference(KEY_PLAINTWEAK_GOV4);
      mPlainTweakMax4 = (ListPreference) findPreference(KEY_PLAINTWEAK_MAX4);
      mPlainTweakMin4 = (ListPreference) findPreference(KEY_PLAINTWEAK_MIN4);
      mPlainTweakGov3.setEntries(availableGovs);
      mPlainTweakGov3.setEntryValues(availableGovs);
      mPlainTweakGov3.setOnPreferenceChangeListener(this);
      mPlainTweakMax3.setEntries(availableClockRate);
      mPlainTweakMax3.setEntryValues(availableClockRate);
      mPlainTweakMax3.setOnPreferenceChangeListener(this);
      mPlainTweakMin3.setEntries(availableClockRate);
      mPlainTweakMin3.setEntryValues(availableClockRate);
      mPlainTweakMin3.setOnPreferenceChangeListener(this);
      mPlainTweakGov4.setEntries(availableGovs);
      mPlainTweakGov4.setEntryValues(availableGovs);
      mPlainTweakGov4.setOnPreferenceChangeListener(this);
      mPlainTweakMax4.setEntries(availableClockRate);
      mPlainTweakMax4.setEntryValues(availableClockRate);
      mPlainTweakMax4.setOnPreferenceChangeListener(this);
      mPlainTweakMin4.setEntries(availableClockRate);
      mPlainTweakMin4.setEntryValues(availableClockRate);
      mPlainTweakMin4.setOnPreferenceChangeListener(this);
      mPlainTweakGov.setTitle(mCore1);
      mPlainTweakMax.setTitle(mCore1);
      mPlainTweakMin.setTitle(mCore1);
      mPlainTweakGov2.setTitle(mCore2);
      mPlainTweakMax2.setTitle(mCore2);
      mPlainTweakMin2.setTitle(mCore2);
      mPlainTweakGov3.setTitle(mCore3);
      mPlainTweakMax3.setTitle(mCore3);
      mPlainTweakMin3.setTitle(mCore3);
      mPlainTweakGov4.setTitle(mCore4);
      mPlainTweakMax4.setTitle(mCore4);
      mPlainTweakMin4.setTitle(mCore4);
      setValueSummary(KEY_PLAINTWEAK_GOV3, "persist.sys.gov3");
      setValueSummary(KEY_PLAINTWEAK_GOV4, "persist.sys.gov4");
      setValueSummary(KEY_PLAINTWEAK_MAX3, "persist.sys.maxkhz3");
      setValueSummary(KEY_PLAINTWEAK_MAX4, "persist.sys.maxkhz4");
      setValueSummary(KEY_PLAINTWEAK_MIN3, "persist.sys.minkhz3");
      setValueSummary(KEY_PLAINTWEAK_MIN4, "persist.sys.minkhz4");
    } else {
      mPlainTweakGov3 = (ListPreference) findPreference(KEY_PLAINTWEAK_GOV3);
      mPlainTweakMax3 = (ListPreference) findPreference(KEY_PLAINTWEAK_MAX3);
      mPlainTweakMin3 = (ListPreference) findPreference(KEY_PLAINTWEAK_MIN3);
      mPlainTweakGov4 = (ListPreference) findPreference(KEY_PLAINTWEAK_GOV4);
      mPlainTweakMax4 = (ListPreference) findPreference(KEY_PLAINTWEAK_MAX4);
      mPlainTweakMin4 = (ListPreference) findPreference(KEY_PLAINTWEAK_MIN4);
      getPreferenceScreen().removePreference(mPlainTweakGov3);
      getPreferenceScreen().removePreference(mPlainTweakMax3);
      getPreferenceScreen().removePreference(mPlainTweakMin3);
      getPreferenceScreen().removePreference(mPlainTweakGov4);
      getPreferenceScreen().removePreference(mPlainTweakMax4);
      getPreferenceScreen().removePreference(mPlainTweakMin4);
    }
    if (mCores1st <= 1) {
        //Dual or Single core
        String mCore1 = "Core 1";
        String mCore2 = "Core 2";
        mPlainTweakGov.setTitle(mCore1);
        mPlainTweakMax.setTitle(mCore1);
        mPlainTweakMin.setTitle(mCore1);
        mPlainTweakGov2.setTitle(mCore2);
        mPlainTweakMax2.setTitle(mCore2);
        mPlainTweakMin2.setTitle(mCore2);
    }
    setAllSummaries();
  }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getActivity();
        String key = preference.getKey();
        Log.d(LOG_TAG, "Updating: "+key+" to "+newValue);
        if (KEY_NOTIFY_PLAINTWEAK.equals(key)) {
            int notifyValue = Integer.valueOf((String) newValue);
            int index = mPlainTweakNotify.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.PLAIN_TWEAK_NOTIFICATIONS,
                    notifyValue);
            mPlainTweakNotify.setSummary(mPlainTweakNotify.getEntries()[index]);
            getActivity().sendBroadcast(new Intent("PLAIN_TWEAK_NOTIFICATIONS"));
            return true;
        } else if (KEY_PLAINTWEAK_GOV.equals(key)) {
          String notifyValue = String.valueOf((String) newValue);
          int index = mPlainTweakGov.findIndexOfValue((String) newValue);
          CMDProcessor.runSuCommand("gov "+notifyValue);
          mPlainTweakGov.setSummary(mPlainTweakGov.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_GOV2.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakGov2.findIndexOfValue((String) newValue);
          //CMDProcessor.startSuCommand();
          mPlainTweakGov2.setSummary(mPlainTweakGov.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_GOV3.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakGov3.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakGov3.setSummary(mPlainTweakGov.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_GOV4.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakGov4.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakGov4.setSummary(mPlainTweakGov.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MIN.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMin.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMin.setSummary(mPlainTweakMin.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MIN2.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMin2.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMin2.setSummary(mPlainTweakMin2.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MIN3.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMin3.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMin3.setSummary(mPlainTweakMin3.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MIN4.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMin4.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMin4.setSummary(mPlainTweakMin4.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MAX.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMax.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMax.setSummary(mPlainTweakMax.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MAX2.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMax2.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMax2.setSummary(mPlainTweakMax2.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MAX3.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMax3.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMax3.setSummary(mPlainTweakMax3.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_MAX4.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakMax4.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakMax4.setSummary(mPlainTweakMax4.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_SCHED.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakScheduler.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakScheduler.setSummary(mPlainTweakScheduler.getEntries()[index]);
          return true;
        } else if (KEY_PLAINTWEAK_TCP.equals(key)) {
          int notifyValue = Integer.valueOf((String) newValue);
          int index = mPlainTweakTCP.findIndexOfValue((String) newValue);
          // op code
          mPlainTweakTCP.setSummary(mPlainTweakTCP.getEntries()[index]);
          return true;
        }
        return false;
    }

    private void setAllSummaries() {
      setValueSummary(KEY_MOD_STOCK_DENSITY, "ro.sf.lcd_density");
      setValueSummary(KEY_STOCK_GOV, "persist.sys.stockgov");
      setValueSummary(KEY_STOCK_SCHED, "persist.sys.stockscheduler");
      setValueSummary(KEY_STOCK_MAX, "persist.sys.stockmaxkhz");
      setValueSummary(KEY_STOCK_MIN, "persist.sys.stockminkhz");
      setValueSummary(KEY_STOCK_TCP, "persist.sys.stocktcpcong");
      setValueSummary(KEY_MOD_CURRENT_DENSITY, "persist.sys.customdensity");
      setValueSummary(KEY_PLAINTWEAK_SCHED, "persist.sys.scheduler");
      setValueSummary(KEY_PLAINTWEAK_GOV, "persist.sys.gov");
      setValueSummary(KEY_PLAINTWEAK_GOV2, "persist.sys.gov2");
      setValueSummary(KEY_PLAINTWEAK_MAX, "persist.sys.maxkhz");
      setValueSummary(KEY_PLAINTWEAK_MAX2, "persist.sys.maxkhz2");
      setValueSummary(KEY_PLAINTWEAK_MIN, "persist.sys.minkhz");
      setValueSummary(KEY_PLAINTWEAK_MIN2, "persist.sys.minkhz2");
      setValueSummary(KEY_PLAINTWEAK_TCP, "persist.sys.tcpcong");
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

    public CharSequence[] ReadFileAsEntries(String filename){
        try {
          BufferedReader reader = new BufferedReader(new FileReader(filename));
          List<String> array = new ArrayList<String>();
          String line;
          while ((line = reader.readLine()) != null) {
              for (String a: line.replace("[", "").replace("]", "").split(" ")){
                    array.add(a);
                    Log.d(LOG_TAG, "Found "+a+" in "+filename);
              }
          }
          reader.close();
          final CharSequence[] returnseq = array.toArray(new CharSequence[array.size()]);
          return returnseq;
          } catch (FileNotFoundException e) {
            CharSequence[] returnseq = { "unable to load"+filename };
            return returnseq;
          } catch (IOException e) {
            CharSequence[] returnseq = { "unable to load"+filename };
            return returnseq;
        }
    }

    public int ReadCoresToInt(String filename){
      try{
          BufferedReader reader = new BufferedReader(new FileReader(filename));
          String line;
          String s;
          while ((line = reader.readLine()) != null) {
              Log.d(LOG_TAG, "Found "+line+" in "+filename);
              s = line.replace("0-", "").trim();
              reader.close();
              return Integer.parseInt(s);
            }
          } catch (FileNotFoundException e) {
              Log.d(LOG_TAG, "unable to load"+filename );
          } catch (IOException e) {
              Log.d(LOG_TAG, "unable to load"+filename );
      }
      return 3;
    }
}
