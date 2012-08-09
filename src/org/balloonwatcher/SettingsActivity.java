package org.balloonwatcher;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class SettingsActivity extends PreferenceActivity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }
}
