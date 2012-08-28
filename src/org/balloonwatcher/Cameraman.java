package org.balloonwatcher;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.hardware.Camera;
import android.location.Location;
import android.graphics.ImageFormat;
import android.os.Environment;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;

class Cameraman {

  public Cameraman(WatchingActivity activity, Logger logger, Watcher watcher) { }
  public void start() { }
  public void stop() { }

  /*
  WatchingActivity mActivity;
  Logger mLogger;
  Watcher mWatcher;
  Timer mTimer;

  BroadcastReceiver mTakePhotoReceiver;
  BroadcastReceiver mCaptureVideoReceiver;

  boolean mEnablePhotos;
  int mPhotoInterval;
  boolean mEnableVideo;
  int mVideoInterval;
  int mVideoLength;

  public static final String TAG = "Cameraman";
  public static final String ACTION_TAKE_PHOTO = "org.balloonwatcher.ACTION_TAKE_PHOTO";
  public static final String ACTION_CAPTURE_VIDEO = "org.balloonwatcher.ACTION_CAPTURE_VIDEO";

  public static final int     PHOTO_JPEG_QUALITY = 95;
  public static final int     PHOTO_PICTURE_FORMAT = ImageFormat.JPEG;
  public static final String  PHOTO_SCENE_MODE = Camera.Parameters.SCENE_MODE_SPORTS;
  public static final int     PHOTO_ZOOM = 0;

  public static final int     VIDEO_QUALITY = CamcorderProfile.QUALITY_HIGH;

  public void setEnablePhotos(boolean val) { mEnablePhotos = val; }
  public void setPhotoInterval(int val) { mPhotoInterval = val; }
  public void setEnableVideo(boolean val) { mEnableVideo = val; }
  public void setVideoInterval(int val) { mVideoInterval = val; }
  public void setVideoLength(int val) { mVideoLength = val; }

  public Cameraman(WatchingActivity activity, Logger logger, Watcher watcher) {
    mActivity = activity;
    mLogger = logger;
    mWatcher = watcher;
    mTimer = new Timer();

    mEnablePhotos = false;
    mPhotoInterval = -1;
    mEnableVideo = false;
    mVideoInterval = -1;
    mVideoLength = -1;
  }

  public void start() {
    Log.i(TAG, "Cameraman started");

    mTakePhotoReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        photo();
      }
    };

    mCaptureVideoReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        video();
      }
    };

    mActivity.registerReceiver(mTakePhotoReceiver, new IntentFilter(ACTION_TAKE_PHOTO));
    mActivity.registerReceiver(mCaptureVideoReceiver, new IntentFilter(ACTION_CAPTURE_VIDEO));

    if(mEnablePhotos) {
      mTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() { 
          mActivity.sendBroadcast(new Intent(ACTION_TAKE_PHOTO));
        }
      }, mPhotoInterval * 1000, mPhotoInterval * 1000);
    }

    if(mEnableVideo) {
      mTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() { 
          mActivity.sendBroadcast(new Intent(ACTION_CAPTURE_VIDEO));
        }
      }, mVideoInterval * 1000, mVideoInterval * 1000);
    }
  }

  public void stop() {
    Log.i(TAG, "Cameraman stopped");

    mActivity.unregisterReceiver(mTakePhotoReceiver);
    mActivity.unregisterReceiver(mCaptureVideoReceiver);
    mTimer.cancel();
  }

  public void photo() {
    try {
      final Camera camera = mActivity.getCamera();

      if(camera == null) {
        Log.w(TAG, "photo(): mActivity.getCamera() returned null, skip");
      } else {

        try {
          Log.i(TAG, "taking photo...");

          Camera.Parameters params = camera.getParameters();
          preparePhotoCamera(params);

          try {
            camera.setParameters(params);
          } catch(Exception e) {
            Log.e(TAG, "setting camera parameters failed", e);
          }

          camera.takePicture(null, null, new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera cam) {
              mActivity.returnCamera(camera);
              savePicture(data); 
            }
          });
        } catch(Exception e) {
          mActivity.returnCamera(camera);
          throw e;
        }
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void video() {
    final Camera camera;
    try {
      camera = mActivity.getCamera();
      if(camera == null) {
        Log.w(TAG, "video(): mActivity.getCamera() returned null, skip");
      } else {
        try {
          camera.unlock();

          final MediaRecorder recorder = new MediaRecorder();
          recorder.setCamera(camera);
          recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
          recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
          recorder.setProfile(CamcorderProfile.get(VIDEO_QUALITY));
          recorder.setOutputFile(videoFile().toString());
          recorder.setPreviewDisplay(mActivity.getPreview().getHolder().getSurface());
          recorder.prepare();

          Log.i(TAG, "capturing video...");
          recorder.start();

          mTimer.schedule(new TimerTask() {
            public void run() { stopVideo(camera, recorder); }
          }, mVideoLength * 1000);
        } catch(Exception e) {
          mActivity.returnCamera(camera);
          throw e;
        }
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void stopVideo(Camera camera, MediaRecorder recorder) {
    try {
      recorder.stop();
      recorder.release();
      camera.lock();
      mActivity.returnCamera(camera);
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void preparePhotoCamera(Camera.Parameters params) {
    params.setJpegQuality(PHOTO_JPEG_QUALITY);
    params.setPictureFormat(PHOTO_PICTURE_FORMAT);
    params.setSceneMode(PHOTO_SCENE_MODE);

    if(params.getMaxZoom() < PHOTO_ZOOM) {
      Log.w(TAG, String.format("camera max zoom (%d) < PHOTO_ZOOM (%d)", params.getMaxZoom(), PHOTO_ZOOM));
      params.setZoom(params.getMaxZoom());
    } else {
      params.setZoom(PHOTO_ZOOM);
    }

    Location loc = mWatcher.getLocation();
    if(loc != null) {
      params.setGpsLatitude(loc.getLatitude());
      params.setGpsLongitude(loc.getLongitude());
      params.setGpsTimestamp(loc.getTime());
      if(loc.hasAltitude()) 
        params.setGpsAltitude(loc.getAltitude());
    }

    List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
    if(pictureSizes != null) { 
      Camera.Size bestSize = pictureSizes.get(0);

      for(Camera.Size size : pictureSizes) 
        if(bestSize.width * bestSize.height < size.width * size.height) 
          bestSize = size;

      params.setPictureSize(bestSize.width, bestSize.height);
    } else {
      Log.e(TAG, "params.getSupportedPictureSizes() returned null!");
    }
  }

  private void savePicture(byte[] data) {
    try {
      File file = pictureFile();
      Log.i(TAG, String.format("saving picture to %s...", file.toString()));

      OutputStream out = null;
      try {
        out = new BufferedOutputStream(new FileOutputStream(file));
        out.write(data);
      } catch(IOException e) {
        Log.e(TAG, "IO error writing picture", e);
      } finally {
        if(out != null)
          out.close();
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private File pictureFile() {
    return mediaFile(".jpg");
  }

  private File videoFile() {
    return mediaFile(".mp4");
  }

  private File mediaFile(String suffix) {
    File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    return new File(root, fileName + suffix);
  }

  */
}
