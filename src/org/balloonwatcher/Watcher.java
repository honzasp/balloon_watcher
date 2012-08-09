package org.balloonwatcher;

import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.util.Log;

public class Watcher {
  WatchingActivity mActivity;
  LocationManager mLocationManager;
  TelephonyManager mTelephonyManager;

  LocationListener mLocationListener;
  PhoneStateListener mPhoneStateListener;
  BroadcastReceiver mBatteryReceiver;

  Location mLocation;
  SignalStrength mSignalStrength;
  Intent mBatteryState;

  public static final String TAG = "Watcher";

  public Watcher(WatchingActivity activity) {
    mActivity = activity;
  }

  public void start() {
    Log.i(TAG, "Watcher started");

    mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
    mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);

    mLocationListener = new LocationListener() {
      public void onLocationChanged(Location location) {
        changeLocation(location);
      }
      public void onStatusChanged(String provider, int status, Bundle extras) {}
      public void onProviderEnabled(String provider) {}
      public void onProviderDisabled(String provider) {}
    };

    mPhoneStateListener = new PhoneStateListener() {
      public void onSignalStrengthsChanged(SignalStrength strength) {
        changeSignalStrength(strength);
      }
    };

    mBatteryReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        //changeBatteryState(intent);
      }
    };

    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    mActivity.registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  }
  
  public void stop() {
    mLocationManager.removeUpdates(mLocationListener);
    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    mActivity.unregisterReceiver(mBatteryReceiver);

    Log.i(TAG, "Watcher stopped");
  }

  private void changeLocation(Location location) {
    mLocation = location;
    mActivity.locationChanged();
  }

  public Location getLocation() {
    return mLocation;
  }

  private void changeSignalStrength(SignalStrength strength) {
    mSignalStrength = strength;
    mActivity.signalStrengthChanged();
  }

  public SignalStrength getSignalStrength() {
    return mSignalStrength;
  }
}
