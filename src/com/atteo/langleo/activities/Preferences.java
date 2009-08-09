package com.atteo.langleo.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.atteo.langleo.R;

public class Preferences extends PreferenceActivity {
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_preferences);
	}
}
