/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainTweakInfo extends SettingsPreferenceFragment implements Indexable {

    private static final String LOG_TAG = "PlainTweakInfo";
    private static final String KEY_PLAINTWEAK_GOV = "plain_tweak_gov";
    private static final String KEY_PLAINTWEAK_SCHED = "plain_tweak_scheduler";
    private static final String KEY_PLAINTWEAK_MAX = "plain_tweak_maxkhz";
    private static final String KEY_PLAINTWEAK_MIN = "plain_tweak_minkhz";
    private static final String KEY_PLAINTWEAK_TCP = "plain_tweak_tcpcong";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.plaintweak_info);

        setValueSummary(KEY_PLAINTWEAK_GOV, "gov");
        setValueSummary(KEY_PLAINTWEAK_SCHED, "scheduler");
        setValueSummary(KEY_PLAINTWEAK_MAX, "maxkhz");
        setValueSummary(KEY_PLAINTWEAK_MIN, "minkhz");
        setValueSummary(KEY_PLAINTWEAK_TCP, "tcpcong");

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

