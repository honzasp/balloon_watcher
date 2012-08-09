package org.balloonwatcher;

import android.app.Activity;
import android.os.Bundle;
import android.location.Location;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.preference.PreferenceManager;

public class WatchingActivity extends Activity {
  TextView mLocationText;
  TextView mBatteryText;
  TextView mSignalText;
  TextView mSMSText;
  TextView mErrorText;

  Watcher mWatcher;
  Logger mLogger;
  SharedPreferences mPreferences;

  public static final String TAG = "WatchingActivity";

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.watching);

    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    mWatcher = new Watcher(this);
    mWatcher.start();

    mLogger = new Logger(this, mWatcher);
    mLogger.setLogInterval(Integer.valueOf(mPreferences.getString("log_interval", "")));
    mLogger.setEnableSMS(mPreferences.getBoolean("send_sms", false));
    mLogger.setSMSInterval(Integer.valueOf(mPreferences.getString("sms_interval", "")));
    mLogger.setSMSRecipient(mPreferences.getString("sms_recipient", ""));
    mLogger.start();

    mLocationText = (TextView) findViewById(R.id.location_text);
    mBatteryText = (TextView) findViewById(R.id.battery_text);
    mSignalText = (TextView) findViewById(R.id.signal_text);
    mErrorText = (TextView) findViewById(R.id.error_text);
    mSMSText = (TextView) findViewById(R.id.sms_text);
  }

  public void onDestroy() {
    super.onDestroy();
    mWatcher.stop();
    mLogger.stop();
  }

  public void locationChanged() {
    mLocationText.setText(mWatcher.getLocation().toString());
  }

  /*
  public void batteryStateChanged(Intent intent) {
    int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
    int batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
    double battery = (double) batteryLevel / batteryScale;
    mBatteryText.setText(String.format("%.2f", battery));
    log("Battery: " + String.format("%.2f", battery));
  }
  */

  public void signalStrengthChanged() {
    mSignalText.setText("GSM signal strength: " +
        Integer.valueOf(mWatcher.getSignalStrength().getGsmSignalStrength()).toString());
  }

}
