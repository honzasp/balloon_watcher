package org.balloonwatcher;

import android.os.Environment;
import android.util.Log;
import android.telephony.SignalStrength;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.lang.StringBuilder;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
  WatchingActivity mActivity;
  Watcher mWatcher;
  Spokesperson mSpokesperson;
  Timer mTimer;
  File mLogFile;
  FileWriter mLogWriter;
  boolean mStopped;

  int mLogInterval;
  boolean mEnableSMS;
  int mSMSInterval;
  String mSMSRecipient;

  public static final String TAG = "Logger";
  public static final String LOG_FILE_NAME = "watcher.log";

  public void setLogInterval(int val) { mLogInterval = val; }
  public void setEnableSMS(boolean val) { mEnableSMS = val; }
  public void setSMSInterval(int val) { mSMSInterval = val; }
  public void setSMSRecipient(String val) { mSMSRecipient = val; }

  public Logger(WatchingActivity activity, Watcher watcher, Spokesperson spokesperson) {
    mActivity = activity;
    mWatcher = watcher;
    mSpokesperson = spokesperson;
    mTimer = new Timer();
    mStopped = true;

    mLogInterval = -1;
    mEnableSMS = false;
    mSMSInterval = -1;
    mSMSRecipient = "";
  }

  public void start() {
    mStopped = false;
    Log.i(TAG, "Logger started");

    if(mLogInterval < 0 || mSMSInterval < 0) {
      Log.e(TAG, "mLogInterval or mSMSInterval were not set! Force disabling SMS (if enabled)");
      mEnableSMS = false;
    }

    mTimer.scheduleAtFixedRate(new TimerTask() {
      public void run() { log(); }
    }, mLogInterval * 1000, mLogInterval * 1000);

    if(mEnableSMS) {
      mTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() { sms(); }
      }, mSMSInterval * 1000, mSMSInterval * 1000);
    }

    tryOpenLogWriter();
    logNote("logger started");
  }

  public void stop() {
    mStopped = true;
    logNote("logger stopped");

    mTimer.cancel();
    if(mLogWriter != null) {
      try {
        mLogWriter.close();
      } catch(Exception e) {
        mLogWriter = null;
      }
    }

    Log.i(TAG, "Logger stopped");
  }

  public void tryOpenLogWriter() {
    try {
      if(mStopped) {
        Log.w(TAG, "tryOpenLogWriter() called, but logger is stopped. Ignore!");
        return;
      }

      if(mLogWriter != null) {
        Log.w(TAG, "tryOpenLogWriter() called, but mLogWriter isn't null; ignored");
        return;
      }

      Log.i(TAG, "Trying to open log writer");

      String storageState = Environment.getExternalStorageState();
      if(Environment.MEDIA_MOUNTED.equals(storageState)) {
        mLogFile = new File(mActivity.getExternalFilesDir(null), LOG_FILE_NAME);
        try {
          mLogWriter = new FileWriter(mLogFile, true);
        } catch(IOException e) {
          Log.e(TAG, "IO error opening log file", e);
          mActivity.showError("IO error opening log file");
        }
      } else {
        Log.e(TAG, "External storage is not mounted, but " + storageState);
        mActivity.showError("Unable to write logs, external storage is " + storageState);
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public String longDate() {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
  }

  public String longLog() {
    StringBuilder sb = new StringBuilder();
    Location loc = mWatcher.getLocation();
    SignalStrength ss = mWatcher.getSignalStrength();

    sb.append(longDate());
    sb.append(" ");

    if(loc != null) {
      sb.append(String.format(Locale.US, "Lat %.6f Lng %.6f", loc.getLatitude(), loc.getLongitude()));

      if(loc.hasAltitude()) 
        sb.append(String.format(Locale.US, " Alt %.2f", loc.getAltitude()));

      if(loc.hasAccuracy()) 
        sb.append(String.format(Locale.US, " Acc %.2f", loc.getAccuracy()));

      if(loc.hasBearing()) 
        sb.append(String.format(Locale.US, " Brg %.1f", loc.getBearing()));

      if(loc.hasSpeed()) 
        sb.append(String.format(Locale.US, " Spd %.2f", loc.getSpeed()));
    } else {
      sb.append("Location unknown");
    }
    sb.append(" ");

    if(ss != null) {
      sb.append(String.format(Locale.US, "GSM %d", ss.getGsmSignalStrength()));
    } else {
      sb.append("Signal unknown");
    }
    sb.append(" ");

    if(mWatcher.hasBatteryState()) {
      sb.append(String.format(Locale.US, "BtL %.2f BtH %s BtT %d",
            mWatcher.batteryLevel(),
            mWatcher.batteryHealth(),
            mWatcher.batteryTemperature()
          ));
    } else {
      sb.append("Battery unknown");
    }
    sb.append(" ");

    return sb.toString();
  }

  public String shortLog() {
    StringBuilder sb = new StringBuilder();
    Location loc = mWatcher.getLocation();
    SignalStrength ss = mWatcher.getSignalStrength();
    Date now = new Date();

    sb.append(new SimpleDateFormat("HH:mm").format(now));

    if(loc != null) {
      sb.append(String.format(Locale.US, "T%.6fG%.6f", loc.getLatitude(), loc.getLongitude()));

      if(loc.hasAltitude())
        sb.append(String.format(Locale.US, "A%.0f", loc.getAltitude()));

      if(loc.hasAccuracy())
        sb.append(String.format(Locale.US, "C%.0f", loc.getAccuracy()));

      if(loc.hasSpeed())
        sb.append(String.format(Locale.US, "S%.0f", loc.getSpeed()));
    } else {
      sb.append("Loc?");
    }

    if(ss != null) {
      sb.append(String.format(Locale.US, "M%d", ss.getGsmSignalStrength()));
    }

    if(mWatcher.hasBatteryState()) {
      sb.append(String.format(Locale.US, "B%.0f%s", mWatcher.batteryLevel()*100, mWatcher.batteryHealthShort()));
    }

    return sb.toString();
  }

  public void log() {
    try {
      String text = longLog();
      Log.i(TAG, "log(): " + text);
      writeLog(text);
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void sms() {
    mSpokesperson.sendSMS(mSMSRecipient, shortLog());
  }

  public void logNote(String msg) {
    writeLog(longDate() + " " + msg);
  }

  public void writeLog(String msg) {
    if(mLogWriter == null) {
      Log.i(TAG, "writeLog(): mLogWriter == null, tryOpenLogWriter() ...");
      tryOpenLogWriter();
    }

    boolean success = false;

    try {
      if(mLogWriter != null) {
        mLogWriter.write(msg + "\n");
        mLogWriter.flush();
        success = true;
      } else {
        Log.w(TAG, "writeLog(): log writer not opened, tried to log: " + msg);
      }
    } catch(IOException e) {
      Log.e(TAG, "Error writing to mLogWriter in writeLog()", e);

      if(mLogWriter != null) {
        try { mLogWriter.close(); } catch(IOException f) {}
      }
      mLogWriter = null;
    }

    if(!success) {
      mActivity.showError("Unable to write log: " + msg);
    }
  }
}
