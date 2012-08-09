package org.balloonwatcher;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import android.view.View;
import android.content.Intent;

public class StartWatcherActivity extends Activity
{
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.start_watcher);
  }

  public void settingsClicked(View view) {
    startActivity(new Intent(this, SettingsActivity.class));
  }

  public void startClicked(View view) {
    startActivity(new Intent(this, WatchingActivity.class));
  }
}
