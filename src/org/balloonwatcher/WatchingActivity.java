package org.balloonwatcher;

import android.app.Activity;
import android.os.Bundle;
import android.location.Location;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.preference.PreferenceManager;
import android.hardware.Camera;

public class WatchingActivity extends Activity {
  TextView mLocationText;
  TextView mBatteryText;
  TextView mSignalText;
  TextView mErrorText;

  Watcher mWatcher;
  Logger mLogger;
  Cameraman mCameraman;

  SharedPreferences mPreferences;

  public static final String TAG = "WatchingActivity";

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.watching);

    mLocationText = (TextView) findViewById(R.id.location_text);
    mBatteryText = (TextView) findViewById(R.id.battery_text);
    mSignalText = (TextView) findViewById(R.id.signal_text);
    mErrorText = (TextView) findViewById(R.id.error_text);

    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    mWatcher = new Watcher(this);

    mLogger = new Logger(this, mWatcher);
    mLogger.setLogInterval(Integer.valueOf(mPreferences.getString("log_interval", "")));
    mLogger.setEnableSMS(mPreferences.getBoolean("send_sms", false));
    mLogger.setSMSInterval(Integer.valueOf(mPreferences.getString("sms_interval", "")));
    mLogger.setSMSRecipient(mPreferences.getString("sms_recipient", ""));

    mCameraman = new Cameraman(this, mLogger, mWatcher);
    /*
    mCameraman.setEnablePhotos(mPreferences.getBoolean("take_photos", false));
    mCameraman.setPhotoInterval(Integer.valueOf(mPreferences.getString("photo_interval", "")));
    mCameraman.setEnableVideo(mPreferences.getBoolean("capture_video", false));
    mCameraman.setVideoInterval(Integer.valueOf(mPreferences.getString("video_interval", "")));
    mCameraman.setVideoLength(Integer.valueOf(mPreferences.getString("video_length", "")));
    */

    mLogger.start();
    mWatcher.start();
    mCameraman.start();

  }

  public void onDestroy() {
    super.onDestroy();

    mCameraman.stop();
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

  public void showError(String msg) {
    try {
      mErrorText.setText(msg);
      Log.i(TAG, "showError() with " + msg);
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }
}
