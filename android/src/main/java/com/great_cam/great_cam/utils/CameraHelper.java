package com.great_cam.great_cam.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraHelper {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final String[] WRITE_PER = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int CAMERA_REQUEST_CODE = 10;
    private static final int WRITE_CODE = 11;
    private Context context;
    private LifecycleOwner lifecycleOwner;
    private ListenableFuture<ProcessCameraProvider> cameraProvider;
    private ImageCapture imageCapture;
    public Camera camera;
    public CameraInfo cameraInfo;
    private PreviewView previewView;
    private Preview preview;
    private CameraSelector cameraSelector;
    public ProcessCameraProvider provider;
    public CameraControl cameraControl;
    public int takePictureDelay = 250;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    public CameraHelper(Context context, LifecycleOwner lifecycleOwner, PreviewView view) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = view;

    }

    public void unbindAll() {
        if (provider != null) {
            provider.unbindAll();
        }
    }

    public void setZoom(float value) {
        cameraControl.setZoomRatio(value);
    }

    public boolean hasTorch() {
        if (cameraInfo == null) {
            return true;
        }
        return cameraInfo.hasFlashUnit();
    }

    public void enableTorch() {
        assert cameraControl != null;
        cameraControl.enableTorch(true);
    }

    public void disableTorch() {
        assert cameraControl != null;
        cameraControl.enableTorch(false);
    }

    public boolean isTorchEnabled() {
        if (!hasTorch()) return false;
        try {
            return cameraInfo.getTorchState().getValue() == TorchState.ON;
        } catch (NullPointerException exception) {
            return false;
        }
    }


    public boolean setFocus(MotionEvent motionEvent) {

        if (camera == null) return false;
        MeteringPointFactory pointFactory = previewView.getMeteringPointFactory();
        MeteringPoint point = pointFactory.createPoint(motionEvent.getX(), motionEvent.getY());

        FocusMeteringAction focus = new FocusMeteringAction.Builder(point).build();


        camera.getCameraControl().startFocusAndMetering(focus);
        return true;
    }


    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
        ActivityCompat.requestPermissions(activity, WRITE_PER, WRITE_CODE);
    }

    public void enableCamera() {

        cameraProvider = ProcessCameraProvider.getInstance(context);
        cameraProvider.addListener(() -> {
            try {
                provider = cameraProvider.get();
                startCameraX();


            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));


    }

    public Executor getExecutor() {
        return ContextCompat.getMainExecutor(context);
    }


    public void startCameraX() {


        bindBackCamera();


    }

    private void createPreview() {
        //preview Use case
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    private void createImageCapture() {
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(new Size(1024, 1024))
                .build();
    }


    public void capturePhoto(ImageCapture.OnImageSavedCallback onSaveImage, boolean autoFlash) {
        if(autoFlash){
            enableTorch();
            try {
                Thread.sleep(takePictureDelay);

            } catch (Exception e) {
                Log.e("Thread sleep error", e.toString());
            }

        }


        String filePath = context.getCacheDir() + File.separator + System.currentTimeMillis() + ".jpg";
        File file = new File(filePath);
        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).build()
                , getExecutor()
                , onSaveImage);


    }


    private void bindBackCamera() {
        provider.unbindAll();
        createPreview();
        createImageCapture();
        cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
        cameraInfo = camera.getCameraInfo();
        cameraControl = camera.getCameraControl();
    }

    private void bindFrontCamera() {
        provider.unbindAll();
        createPreview();
        createImageCapture();
        cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        try {
            if (provider.hasCamera(cameraSelector)) {
                camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
                cameraInfo = camera.getCameraInfo();
                cameraControl = camera.getCameraControl();
            }

        } catch (CameraInfoUnavailableException exp) {
            Log.i("Binding front camera exception", exp.toString());
            bindBackCamera();
        }
    }

    public void bindCamera(boolean backCamera) {
        if (backCamera) {
            bindBackCamera();
        } else {
            bindFrontCamera();
        }
    }

    public void setTakePictureDelay(int delay){
        if(delay <= 0){
            takePictureDelay = 250;
            return;
        }

        takePictureDelay = delay;

    }


}


