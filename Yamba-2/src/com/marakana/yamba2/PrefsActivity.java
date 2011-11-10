package com.marakana.yamba2;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefsActivity extends PreferenceActivity { // <1>

  @Override
  protected void onCreate(Bundle savedInstanceState) { // <2>
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.prefs); // <3>
  }

}
