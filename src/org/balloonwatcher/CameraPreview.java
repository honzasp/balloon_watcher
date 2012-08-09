package org.balloonwatcher;

import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;
import android.util.Log;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
  private SurfaceHolder mHolder;
  private Camera mCamera;

  public static final String TAG = "CameraPreview";

  public CameraPreview(Context context, Camera camera) {
    super(context);
    mCamera = camera;

    mHolder = getHolder();
    mHolder.addCallback(this);
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  public void surfaceCreated(SurfaceHolder holder) {
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    if (mHolder.getSurface() == null){
      return;
    }

    try {
      mCamera.stopPreview();
    } catch (Exception e){
    }

    try {
      mCamera.setPreviewDisplay(mHolder);
      mCamera.startPreview();
    } catch (Exception e){
      Log.d(TAG, "Error starting camera preview: " + e.getMessage());
    }
  }

}
