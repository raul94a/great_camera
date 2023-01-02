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
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
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
    private String filepath;
    public ListenableFuture<ProcessCameraProvider> cameraProvider;
    private ImageCapture imageCapture;
    private PreviewView previewView;
    private Preview preview;
    private ProcessCameraProvider provider;

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
    public void enableCamera(View buttonTriggerCamera, ImageCapture.OnImageSavedCallback onSaveImage) {

        cameraProvider = ProcessCameraProvider.getInstance(context);
        cameraProvider.addListener(() -> {
            try {
                provider = cameraProvider.get();

                startCameraX(provider);


                buttonTriggerCamera.setOnClickListener(view -> capturePhoto(onSaveImage));


            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));


    }

    public Executor getExecutor() {
        return ContextCompat.getMainExecutor(context);
    }


    public void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        //preview Use case
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        //
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(new Size(1024, 1024))
                .build();

        CameraSelector cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);


    }


    public void capturePhoto(ImageCapture.OnImageSavedCallback onSaveImage) {
        String filePath = context.getCacheDir() + File.separator + System.currentTimeMillis() + ".jpg";
        File file = new File(filePath);
        setFilepath(filePath);
        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).build()
                , getExecutor()
                , onSaveImage);
    }

    private void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getFilepath() {
        String filepath = this.filepath;
        this.filepath = "";
        return filepath;
    }

    public interface onSaveImageCallback {
        void onSave();
    }


}


