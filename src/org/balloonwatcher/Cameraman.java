package org.balloonwatcher;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.content.pm.PackageManager;
import android.util.Log;
import android.hardware.Camera;
import android.location.Location;
import android.graphics.ImageFormat;
import android.os.Environment;

class Cameraman {
  WatchingActivity mActivity;
  Logger mLogger;
  Watcher mWatcher;
  Timer mTimer;

  Camera mCamera;
  boolean mEnablePhotos;
  int mPhotoInterval;
  boolean mEnableVideo;
  int mVideoInterval;
  int mVideoLength;

  public static final String TAG = "Cameraman";

  public static final int CAMERA_JPEG_QUALITY = 95;
  public static final int CAMERA_PICTURE_FORMAT = ImageFormat.JPEG;
  public static final String CAMERA_SCENE_MODE = Camera.Parameters.SCENE_MODE_SPORTS;
  public static final int CAMERA_ZOOM = 0;

  public void setCamera(Camera cam) { mCamera = cam; }
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

    if(mEnablePhotos) {
      mTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() { photo(); }
      }, mPhotoInterval * 1000, mPhotoInterval * 1000);
    }

    if(mEnableVideo) {
      mTimer.scheduleAtFixedRate(new TimerTask() {
        public void run() { video(); }
      }, mVideoInterval * 1000, mVideoInterval * 1000);
    }
  }

  public void stop() {
    Log.i(TAG, "Cameraman stopped");
    mTimer.cancel();
  }

  public void photo() {
    try {
      if(mCamera == null) {
        Log.w(TAG, "photo(): mCamera is null, no photo");
        return;
      }
            
      Log.i(TAG, "taking photo...");

      Camera.Parameters params = mCamera.getParameters();
      params.setJpegQuality(CAMERA_JPEG_QUALITY);
      params.setPictureFormat(CAMERA_PICTURE_FORMAT);
      params.setSceneMode(CAMERA_SCENE_MODE);

      if(params.getMaxZoom() < CAMERA_ZOOM) {
        Log.w(TAG, String.format("camera max zoom (%d) < CAMERA_ZOOM (%d)", params.getMaxZoom(), CAMERA_ZOOM));
        params.setZoom(params.getMaxZoom());
      } else {
        params.setZoom(CAMERA_ZOOM);
      }

      Location loc = mWatcher.getLocation();
      if(loc != null) {
        params.setGpsLatitude(loc.getLatitude());
        params.setGpsLongitude(loc.getLongitude());
        params.setGpsTimestamp(loc.getTime());
        if(loc.hasAltitude()) 
          params.setGpsAltitude(loc.getAltitude());
      }

      try {
        mCamera.setParameters(params);
      } catch(Exception e) {
        Log.e(TAG, "setting camera parameters failed", e);
      }

      mCamera.takePicture(null, null, new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
          mActivity.preparePreview();
          savePicture(data); 
        }
      });
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void video() {
    try {
      Log.i(TAG, "capturing video...");
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void savePicture(byte[] data) {
    try {
      File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
      String imageName = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
      File imageFile = new File(root, imageName + ".jpg");

      Log.i(TAG, String.format("saving picture to %s...", imageFile.toString()));

      OutputStream out = null;
      try {
        out = new BufferedOutputStream(new FileOutputStream(imageFile));
        out.write(data);
      } catch(IOException e) {
        Log.e(TAG, "IO error writing picture", e);
      } finally {
        if(out != null) {
          out.close();
        }
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

}
