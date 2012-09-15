package org.balloonwatcher;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
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

  WatchingActivity mActivity;
  Logger mLogger;
  Watcher mWatcher;

  Semaphore mCameraSemaphore;
  ScheduledThreadPoolExecutor mSTPE;
  Camera mCamera;

  SurfaceView mSurfaceView;

  boolean mEnablePhotos;
  int mPhotoInterval;
  String mSceneMode;
  boolean mEnableVideo;
  int mVideoInterval;
  int mVideoLength;

  public static final String TAG = "Cameraman";
  public static final int STPE_POOL_SIZE = 5;

  public static final int     PHOTO_JPEG_QUALITY = 95;
  public static final int     PHOTO_PICTURE_FORMAT = ImageFormat.JPEG;
  public static final int     PHOTO_ZOOM = 0;

  public static final int     VIDEO_QUALITY = CamcorderProfile.QUALITY_LOW;

  public void setEnablePhotos(boolean val) { mEnablePhotos = val; }
  public void setPhotoInterval(int val) { mPhotoInterval = val; }
  public void setSceneMode(String val) { mSceneMode = val; }
  public void setEnableVideo(boolean val) { mEnableVideo = val; }
  public void setVideoInterval(int val) { mVideoInterval = val; }
  public void setVideoLength(int val) { mVideoLength = val; }

  public Cameraman(WatchingActivity activity, Logger logger, Watcher watcher, SurfaceView sv) {
    mActivity = activity;
    mLogger = logger;
    mWatcher = watcher;
    mSurfaceView = sv;
    mSTPE = new ScheduledThreadPoolExecutor(STPE_POOL_SIZE);

    mCameraSemaphore = new Semaphore(1);

    mEnablePhotos = false;
    mPhotoInterval = -1;
    mEnableVideo = false;
    mVideoInterval = -1;
    mVideoLength = -1;

    prepareCamera();
  }

  private void restartPreview() {
    try {
      final SurfaceHolder holder = mSurfaceView.getHolder();

      if(holder.getSurface() == null)
        return;

      if(mCamera == null)
        return;
      
      try {
        mCamera.stopPreview();
      } catch(Exception e) { }

      mCamera.setDisplayOrientation(90);
      mCamera.setPreviewDisplay(holder);
      mCamera.startPreview();
    } catch(Exception e) {
      Log.e(TAG, "Error restarting camera preview", e);
      mActivity.showError("Error restarting camera preview");
    }
  }

  public void start() {
    Log.i(TAG, "Cameraman started");

    if(mEnablePhotos) {
      mSTPE.scheduleAtFixedRate(new Runnable() {
        public void run() {
          photo();
        }
      }, mPhotoInterval / 2, mPhotoInterval, TimeUnit.SECONDS);
    }

    if(mEnableVideo) {
      mSTPE.scheduleAtFixedRate(new Runnable() {
        public void run() {
          video();
        }
      }, mVideoInterval / 2, mVideoInterval, TimeUnit.SECONDS);
    }
  }

  public void stop() {
    if(mSTPE != null) mSTPE.shutdownNow();
    if(mCamera != null) mCamera.release();
    Log.i(TAG, "Cameraman stopped");
  }

  public void photo() {
    try {
      if(mCamera == null) {
        Log.i(TAG, "photo(): mCamera is null");
        return;
      }

      Log.i(TAG, "photo() acquiring camera...");
      mCameraSemaphore.acquire();
      Log.i(TAG, "photo() acquired camera");

      try {
        configurePhotoCamera(mCamera.getParameters());
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
          public void onPictureTaken(byte[] data, Camera camera) {
            restartPreview();
            savePicture(data);
            mCameraSemaphore.release();
          }
        });
      } catch(Exception e) {
        mCameraSemaphore.release();
        throw e;
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  public void video() {
    /*try {
      if(mCamera == null) {
        Log.i(TAG, "video(): mCamera is null");
        return;
      }

      Log.i(TAG, "video() acquiring camera...");
      mCameraSemaphore.acquire();
      Log.i(TAG, "video() acquired camera");

      try {
        mCamera.unlock();
        final MediaRecorder recorder = new MediaRecorder();
        recorder.setCamera(mCamera);
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setVideoSize(640, 480);

        recorder.setOutputFile(videoFile().toString());
        recorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());
        recorder.prepare();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          public void run() {
            Log.i(TAG, "stopping recorder");
            recorder.stop();
            recorder.release();
            mCamera.lock();
            restartPreview();
            mCameraSemaphore.release();
          }
        }, mVideoLength * 1000);

        Log.i(TAG, "starting recorder");
        recorder.start();
      } catch(Exception e) {
        mCameraSemaphore.release();
        mCamera.lock();
        throw e;
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }*/
  }

  private void prepareCamera() {
    try {
      if(!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        return;

      mCamera = Camera.open();

      if(mSurfaceView.getHolder() != null) {
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
          public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            restartPreview();
          }

          public void surfaceCreated(SurfaceHolder holder) {}
          public void surfaceDestroyed(SurfaceHolder holder) {}
        });
      } else {
        Log.e(TAG, "mSurfaceView().getHolder() returned null!");
      }
    } catch(Exception e) {
      Log.wtf(TAG, e);
    }
  }

  private void configurePhotoCamera(Camera.Parameters params) {
    params.setJpegQuality(PHOTO_JPEG_QUALITY);
    params.setPictureFormat(PHOTO_PICTURE_FORMAT);
    params.setSceneMode(mSceneMode);

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
        out = new BufferedOutputStream(new FileOutputStream(file), 8*1024);
        out.write(data);
      } catch(IOException e) {
        Log.e(TAG, "IO error writing picture", e);
        mActivity.showError("IO error writing picture");
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
}
