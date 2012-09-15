package org.balloonwatcher;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.media.MediaPlayer;
import android.util.Log;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.app.Activity;
import android.os.Bundle;

class Spokesperson {
  WatchingActivity mActivity;
  Watcher mWatcher;
  Logger mLogger;
  Cameraman mCameraman;

  Timer mSoundTimer;
  MediaPlayer mSoundPlayer;

  BroadcastReceiver mSMSSentReceiver;
  BroadcastReceiver mSMSMessageReceiver;

  boolean mEnabled;
  int mSoundLength;

  public static final String TAG = "Spokesperson";
  public static final String ACTION_SMS_SENT = "org.balloonwatcher.SMS_SENT_ACTION";

  public void setEnabled(boolean val) { mEnabled = val; }
  public boolean isEnabled() { return mEnabled; }
  public void setSoundLength(int val) { mSoundLength = val; }
  public void setLogger(Logger val) { mLogger = val; }
  public void setCameraman(Cameraman val) { mCameraman = val; }

  public Spokesperson(WatchingActivity activity, Watcher watcher) {
    mActivity = activity;
    mWatcher = watcher;

    mEnabled = false;
  }

  public void start() {
    if(mEnabled) {
      mSoundTimer = new Timer();
      mSoundPlayer = MediaPlayer.create(mActivity, R.raw.alert);

      mSMSSentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
          smsSent(this);
        }
      };

      mSMSMessageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
          smsReceived(intent);
        }
      };

      mActivity.registerReceiver(mSMSSentReceiver, new IntentFilter(ACTION_SMS_SENT));
      mActivity.registerReceiver(mSMSMessageReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

      Log.i(TAG, "Spokesperson started");
    } else {
      Log.i(TAG, "Spokesperson disabled, but pretends to be starting");
    }
  }

  public void stop() {
    if(mEnabled) {
      mSoundTimer.cancel();
      mSoundPlayer.stop();
      mActivity.unregisterReceiver(mSMSSentReceiver);
      mActivity.unregisterReceiver(mSMSMessageReceiver);
      Log.i(TAG, "Spokesperson stopped");
    } else {
      Log.i(TAG, "Spokesperson disabled, so there is nothing to stop");
    }
  }

  public void sendSMS(String recipient, String body) {
    sendSMS(recipient, body, false);
  }

  public void sendSMS(String recipient, String body, boolean force) {
    try {
      if(force || mEnabled) {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(body);

        if(messages.size() > 1) {
          Log.w(TAG, "SMS was divided to more parts, sending just the first one");
          Log.w(TAG, "  The first part: " + messages.get(0));
          Log.w(TAG, "  The whole SMS: " + body);
          mActivity.showError("Log SMS had to be divided: " + body + "; sent only " + messages.get(0));
        }

        Log.i(TAG, "Sending SMS to " + recipient + ": " + messages.get(0));
        sms.sendTextMessage(
          recipient, null, messages.get(0),
          PendingIntent.getBroadcast(mActivity, 0, new Intent(ACTION_SMS_SENT), 0),
          null);
      } else {
        Log.w(TAG, "sendSMS('" + recipient + "', '" + body + "', " + force +")"
            + " called, but spokesperson is disabled");
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void smsSent(BroadcastReceiver receiver) {
    try {
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
      } else {
        Log.e(TAG, "Error sending SMS: " + error);
        mActivity.showError("Unable to send SMS: " + error);
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void smsReceived(Intent intent) {
    try {
      Bundle extras = intent.getExtras();
      if(extras == null) 
        return;

      Object[] pdus = (Object[]) extras.get("pdus");

      for(int i = 0; i < pdus.length; ++i) {
        processSMS(SmsMessage.createFromPdu((byte[]) pdus[i]));
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void processSMS(SmsMessage msg) {
    String from = msg.getDisplayOriginatingAddress();
    String body = msg.getDisplayMessageBody();
    Log.i(TAG, "received SMS from " + from + ": " + body);

    String[] parts = body.split("\\s+");

    if(parts[0].toLowerCase().equals("bw")) {
      for(int i = 1; i < parts.length; ++i) {
        final String part = parts[i];
        final String lpart = part.toLowerCase();

        if(lpart.equals("sendlog")) {
          sendSMS(from, mLogger.shortLog(), true);
        } else if(lpart.equals("restart")) {
          mActivity.restart();
        } else if(lpart.equals("photo")) {
          mCameraman.photo();
        } else if(lpart.equals("sound")) {
          sound();
        } else {
          Log.w(TAG, "Unknown SMS instruction '" + part + "'");
        }
      }
    } else {
      Log.i(TAG, "this SMS is ignored");
    }
  }

  public void sound() {
    Log.i(TAG, "starting sound");
    mSoundPlayer.setLooping(true);
    mSoundPlayer.setVolume(1.0f, 1.0f);
    mSoundPlayer.start();

    mSoundTimer.schedule(new TimerTask() {
      public void run() {
        try {
          Log.i(TAG, "stopping sound");
          mSoundPlayer.stop();
          mSoundPlayer.prepare();
        } catch(Exception e) {
          Log.wtf(TAG, e);
        }
      }
    }, 1000 * mSoundLength);
  }

}
