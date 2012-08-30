package org.balloonwatcher;

import java.util.Locale;
import android.app.Activity;
import android.os.Bundle;
import android.location.Location;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.view.SurfaceView;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.preference.PreferenceManager;
import android.hardware.Camera;

public class WatchingActivity extends Activity {
  TextView mLocationText;
  TextView mBatteryText;
  TextView mSignalText;
  TextView mErrorText;
  SurfaceView mCameraPreview;

  Watcher mWatcher;
  Logger mLogger;
  Cameraman mCameraman;
  Spokesperson mSpokesperson;

  BroadcastReceiver mErrorMsgReceiver;

  SharedPreferences mPreferences;

  public static final String TAG = "WatchingActivity";
  public static final String ACTION_SHOW_ERROR = "org.balloonwatcher.ACTION_SHOW_ERROR";

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.watching);

    mLocationText = (TextView) findViewById(R.id.location_text);
    mBatteryText = (TextView) findViewById(R.id.battery_text);
    mSignalText = (TextView) findViewById(R.id.signal_text);
    mErrorText = (TextView) findViewById(R.id.error_text);
    mCameraPreview = (SurfaceView) findViewById(R.id.camera_preview);

    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    mErrorMsgReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        mErrorText.setText(intent.getStringExtra("message"));
      }
    };

    registerReceiver(mErrorMsgReceiver, new IntentFilter(ACTION_SHOW_ERROR));

    mWatcher = new Watcher(this);

    mSpokesperson = new Spokesperson(this, mWatcher);
    mSpokesperson.setEnabled(mPreferences.getBoolean("enable_spokesperson", false));

    mLogger = new Logger(this, mWatcher, mSpokesperson);
    mLogger.setLogInterval(Integer.valueOf(mPreferences.getString("log_interval", "")));
    mLogger.setEnableSMS(mSpokesperson.isEnabled() && mPreferences.getBoolean("send_sms", false));
    mLogger.setSMSInterval(Integer.valueOf(mPreferences.getString("sms_interval", "")));
    mLogger.setSMSRecipient(mPreferences.getString("sms_recipient", ""));

    mCameraman = new Cameraman(this, mLogger, mWatcher, mCameraPreview);
    mCameraman.setEnablePhotos(mPreferences.getBoolean("take_photos", false));
    mCameraman.setPhotoInterval(Integer.valueOf(mPreferences.getString("photo_interval", "")));
    mCameraman.setEnableVideo(mPreferences.getBoolean("capture_video", false));
    mCameraman.setVideoInterval(Integer.valueOf(mPreferences.getString("video_interval", "")));
    mCameraman.setVideoLength(Integer.valueOf(mPreferences.getString("video_length", "")));

    mLogger.start();
    mWatcher.start();
    mSpokesperson.start();
    mCameraman.start();
  }

  public void onDestroy() {
    super.onDestroy();

    unregisterReceiver(mErrorMsgReceiver);

    mCameraman.stop();
    mSpokesperson.stop();
    mWatcher.stop();
    mLogger.stop();
  }

  public void locationChanged() {
    try {
      StringBuilder sb = new StringBuilder();
      Location loc = mWatcher.getLocation();

      sb.append(String.format(Locale.US, "Lat %.6f Lng %.6f\n", loc.getLatitude(), loc.getLongitude()));

      if(loc.hasAltitude()) 
        sb.append(String.format(Locale.US, "Altitude %.1f m\n", loc.getAltitude()));

      if(loc.hasAccuracy()) 
        sb.append(String.format(Locale.US, "Accuracy %.1f m\n", loc.getAccuracy()));

      if(loc.hasBearing()) 
        sb.append(String.format(Locale.US, "Bearing %.1f\n", loc.getBearing()));

      if(loc.hasSpeed()) 
        sb.append(String.format(Locale.US, "Speed %.2f m/s\n", loc.getSpeed()));

      sb.append(String.format(Locale.US, "Time %d\n", loc.getTime()));

      mLocationText.setText(sb.toString());
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void batteryStateChanged() {
    try {
      mBatteryText.setText(String.format(Locale.US, "%.0f %% %d\u2103 (%s)",
            mWatcher.batteryLevel() * 100,
            mWatcher.batteryTemperature(),
            mWatcher.batteryHealth()
      ));
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void signalStrengthChanged() {
    try {
      mSignalText.setText("GSM signal strength: " +
          Integer.valueOf(mWatcher.getSignalStrength().getGsmSignalStrength()).toString());
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void showError(String msg) {
    try {
      Log.i(TAG, "showError() with " + msg);
      Intent intent = new Intent(ACTION_SHOW_ERROR);
      intent.putExtra("message", msg);
      sendBroadcast(intent);
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }
}
