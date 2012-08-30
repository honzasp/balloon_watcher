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
        changeBatteryState(intent);
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

  private void changeBatteryState(Intent state) {
    mBatteryState = state;
    mActivity.batteryStateChanged();
  }

  public boolean hasBatteryState() {
    return mBatteryState != null;
  }

  public double batteryLevel() {
    if(mBatteryState != null) {
      final int level = mBatteryState.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
      final int scale = mBatteryState.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
      return (double) level / scale;
    } else {
      return -1.0;
    }
  }

  public String batteryHealth() {
    if(mBatteryState != null) {
      switch(mBatteryState.getIntExtra(BatteryManager.EXTRA_HEALTH, -99)) {
        case BatteryManager.BATTERY_HEALTH_DEAD: 
          return "dead";
        case BatteryManager.BATTERY_HEALTH_GOOD:
          return "good";
        case BatteryManager.BATTERY_HEALTH_OVERHEAT:
          return "overheat";
        case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
          return "overvoltage";
        case BatteryManager.BATTERY_HEALTH_UNKNOWN:
          return "unknown";
        case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
          return "unspecified failure";
        default:
          Log.w(TAG, "Unknown battery health value");
          return "?!";
      }
    } else {
      return "?";
    }
  }

  public String batteryHealthShort() {
    if(mBatteryState != null) {
      switch(mBatteryState.getIntExtra(BatteryManager.EXTRA_HEALTH, -99)) {
        case BatteryManager.BATTERY_HEALTH_DEAD: 
          return "d";
        case BatteryManager.BATTERY_HEALTH_GOOD:
          return "g";
        case BatteryManager.BATTERY_HEALTH_OVERHEAT:
          return "h";
        case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
          return "v";
        case BatteryManager.BATTERY_HEALTH_UNKNOWN:
          return "u";
        case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
          return "f";
        default:
          Log.w(TAG, "Unknown battery health value");
          return "!";
      }
    } else {
      return "?";
    }
  }

  public int batteryTemperature() {
    if(mBatteryState != null) {
      return mBatteryState.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -99);
    } else {
      return -99;
    }
  }

}
