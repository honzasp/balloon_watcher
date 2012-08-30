package org.balloonwatcher;

import java.util.ArrayList;
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

  BroadcastReceiver mSMSSentReceiver;
  BroadcastReceiver mSMSMessageReceiver;

  boolean mEnabled;

  public static final String TAG = "Spokesperson";
  public static final String ACTION_SMS_SENT = "org.balloonwatcher.SMS_SENT_ACTION";

  public void setEnabled(boolean val) { mEnabled = val; }
  public boolean isEnabled() { return mEnabled; }

  public Spokesperson(WatchingActivity activity, Watcher watcher) {
    mActivity = activity;
    mWatcher = watcher;

    mEnabled = false;
  }

  public void start() {
    if(mEnabled) {
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
      mActivity.unregisterReceiver(mSMSSentReceiver);
      mActivity.unregisterReceiver(mSMSMessageReceiver);
      Log.i(TAG, "Spokesperson stopped");
    } else {
      Log.i(TAG, "Spokesperson disabled, so there is nothing to stop");
    }
  }

  public void sendSMS(String recipient, String body) {
    try {
      if(mEnabled) {
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
        Log.w(TAG, "sendSMS('" + body + "') called, but spokesperson is disabled");
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void smsSent(BroadcastReceiver receiver) {
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
  }

  private void smsReceived(Intent intent) {
    Bundle extras = intent.getExtras();
    if(extras == null) 
      return;

    Object[] pdus = (Object[]) extras.get("pdus");

    for(int i = 0; i < pdus.length; ++i) {
      processSMS(SmsMessage.createFromPdu((byte[]) pdus[i]));
    }
  }

  private void processSMS(SmsMessage msg) {
    String from = msg.getDisplayOriginatingAddress();
    String body = msg.getDisplayMessageBody();
    Log.i(TAG, "received SMS from " + from + ": " + body);
  }

}
