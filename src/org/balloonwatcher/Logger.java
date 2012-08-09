package org.balloonwatcher;

import android.os.Environment;
import android.util.Log;
import android.telephony.SmsManager;
import android.telephony.SignalStrength;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.location.Location;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Date;
import java.lang.StringBuilder;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
  WatchingActivity mActivity;
  Watcher mWatcher;
  Timer mTimer;
  File mLogFile;
  FileWriter mLogWriter;
  boolean mStopped;
  BroadcastReceiver mSMSSentReceiver;

  int mLogInterval;
  boolean mEnableSMS;
  int mSMSInterval;
  String mSMSRecipient;

  public static final String TAG = "Logger";
  public static final String ACTION_SMS_SENT = "org.balloonwatcher.SMS_SENT_ACTION";
  public static final String LOG_FILE_NAME = "watcher.log";

  public void setLogInterval(int val) { mLogInterval = val; }
  public void setEnableSMS(boolean val) { mEnableSMS = val; }
  public void setSMSInterval(int val) { mSMSInterval = val; }
  public void setSMSRecipient(String val) { mSMSRecipient = val; }

  public Logger(WatchingActivity activity, Watcher watcher) {
    mActivity = activity;
    mWatcher = watcher;
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

    mSMSSentReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        smsSent(this);
      }
    };

    mActivity.registerReceiver(mSMSSentReceiver, new IntentFilter(ACTION_SMS_SENT));

    tryOpenLogWriter();
    logNote("logger started");
  }

  public void stop() {
    mStopped = true;
    logNote("logger stopped");

    mTimer.cancel();
    mActivity.unregisterReceiver(mSMSSentReceiver);
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
        }
      } else {
        Log.e(TAG, "External storage is not mounted, but " + storageState);
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
      sb.append(String.format("Lt %.6f Ln %.6f", loc.getLatitude(), loc.getLongitude()));

      if(loc.hasAltitude()) 
        sb.append(String.format(" Al %.2f", loc.getAltitude()));

      if(loc.hasAccuracy()) 
        sb.append(String.format(" Ac %.2f", loc.getAccuracy()));

      if(loc.hasBearing()) 
        sb.append(String.format(" Br %.1f", loc.getBearing()));

      if(loc.hasSpeed()) 
        sb.append(String.format(" Sp %.2f", loc.getSpeed()));
    } else {
      sb.append("Location unknown");
    }
    sb.append(" ");

    if(ss != null) {
      sb.append(String.format("GSM %d", ss.getGsmSignalStrength()));
    } else {
      sb.append("Signal unknown");
    }
    sb.append(" ");

    return sb.toString();
  }

  public String shortLog() {
    StringBuilder sb = new StringBuilder();
    Location loc = mWatcher.getLocation();
    Date now = new Date();

    sb.append(new SimpleDateFormat("HH:mm").format(now));

    if(loc != null) {
      sb.append(String.format("T%.6fG%.6f", loc.getLatitude(), loc.getLongitude()));

      if(loc.hasAltitude())
        sb.append(String.format("A%.0f", loc.getAltitude()));

      if(loc.hasAccuracy())
        sb.append(String.format("C%.0f", loc.getAccuracy()));

      if(loc.hasSpeed())
        sb.append(String.format("S%.0f", loc.getSpeed()));
    } else {
      sb.append("Loc?");
    }

    return sb.toString();
  }

  public void log() {
    String text = longLog();
    Log.i(TAG, "log(): " + text);
    writeLog(text);
  }

  public void sms() {
    try {
      if(mEnableSMS) {
        SmsManager sms = SmsManager.getDefault();
        String body = shortLog();
        ArrayList<String> messages = sms.divideMessage(body);

        if(messages.size() > 1) {
          Log.w(TAG, "SMS was divided to more parts, sending just the first one");
          Log.w(TAG, "  The first part: " + messages.get(0));
          Log.w(TAG, "  The whole SMS: " + body);
          mActivity.showError("Log SMS had to be divided: " + body + "; sent only " + messages.get(0));
        }

        Log.i(TAG, "Sending SMS to " + mSMSRecipient + ": " + messages.get(0));
        logNote("sending SMS: " + messages.get(0));
        sms.sendTextMessage(
          mSMSRecipient, null, messages.get(0),
          PendingIntent.getBroadcast(mActivity, 0, new Intent(ACTION_SMS_SENT), 0),
          null);
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void smsSent(BroadcastReceiver receiver) {
    String error = null;

    switch(receiver.getResultCode()) {
      case Activity.RESULT_OK:
        break;
      case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
        error = "Generic failure";
        break;
      case SmsManager.RESULT_ERROR_NO_SERVICE:
        error = "No service";
        break;
      case SmsManager.RESULT_ERROR_NULL_PDU:
        error = "Null PDU";
        break;
      case SmsManager.RESULT_ERROR_RADIO_OFF:
        error = "Radio off";
        break;
      default:
        error = "Unknown error";
        break;
    }

    if(error == null) {
      Log.i(TAG, "SMS was successfuly sent");
      logNote("sms successfully sent");
    } else {
      Log.e(TAG, "Error sending SMS: " + error);
      logNote("error sending SMS: " + error);
    }
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
