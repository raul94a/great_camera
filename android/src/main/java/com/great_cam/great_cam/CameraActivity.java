package com.great_cam.great_cam;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraState;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ZoomState;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.great_cam.great_cam.utils.CameraHelper;
import com.great_cam.great_cam.viewmodels.CameraViewModel;

import java.io.File;
import java.util.Objects;

enum FlashType {
    AUTO, OFF, ON
}

public class CameraActivity extends AppCompatActivity {
    private ImageView cancelPicture, refreshPicture, validPicture, flash, btnClose, imgPreview, btnPicture;
    private LinearLayout handler, previewButtons;
    private CameraHelper camera;
    private PreviewView preview;
    private CameraViewModel cameraViewModel;
    private ConstraintLayout root;
    // private SeekBar slider;
    private FlashType flashtype = FlashType.OFF;

    private final String CAMERA_BACK_ASSET = "@drawable/ic_camera_back";
    private final String CAMERA_FRONT_ASSET = "@drawable/ic_camera_front";
    private final String CAMERA_SWITCH = "@drawable/ic_camera_switch";


    private boolean startTracking = true;
    private int progress;
    private float currentZoomProgress;
    private float previousSpan;
    private boolean canFocus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        setLayout();
        initializeCameraHelper();
        startCameraStreaming();
        cameraActionsHandler();
        observe();

    }

    private void initializeCameraHelper() {
        cameraViewModel = new ViewModelProvider(this,
                ViewModelProvider.Factory.from(CameraViewModel.initializer)
        ).get(CameraViewModel.class);
    }

    private void observe() {
        cameraViewModel.showPreview.observe(this, show -> {
            if (show) {
                //             slider.setVisibility(View.GONE);
                preview.setVisibility(View.GONE);
                handler.setVisibility(View.GONE);
                imgPreview.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.VISIBLE);
            } else {
                //           slider.setVisibility(View.VISIBLE);
                preview.setVisibility(View.VISIBLE);
                handler.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.GONE);
                imgPreview.setVisibility(View.GONE);
                imgPreview.setImageBitmap(null);

            }
        });
    }

    private void cameraActionsHandler() {
        onSeekBarChange();
        onTapSwitchCamera();
        // focusImage();
        onTapCloseCamera();
        onTapRefresh();
        onTapValidate();
        changeTorchStatus();
        onTapTakePicture();
    }

    private void setCameraHandler() {
        if (camera != null) {
            camera = null;
        }
        camera = new CameraHelper(this.getApplicationContext(), this, preview);

    }

    private void startCameraStreaming() {
        setCameraHandler();
        enableCamera();
    }

    private void onTapTakePicture() {
        btnPicture.setOnClickListener(v -> {

           /* camera.capturePhoto(new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Log.i("onImageSaved", "" + outputFileResults.getSavedUri());
                    String path = Objects.requireNonNull(outputFileResults.getSavedUri()).getPath();
                    cameraViewModel.picturePath.setValue(path);
                    Log.i("Path", " " + path);
                    imgPreview.setImageBitmap(BitmapFactory.decodeFile(path));
                    cameraViewModel.show();
                    hideFlash();

                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e("ImageCaptureException", exception.toString());
                }
            }, flashtype == FlashType.AUTO);*/
            if(camera.recording != null){
                camera.stopVideo();
                return;
            }
            camera.captureVideo();

        });
    }

    private boolean hasTorch() {
        if (camera == null) return false;
        return camera.hasTorch();
    }

    private void changeTorchStatus() {
        if (!hasTorch()) return;

        flash.setOnClickListener(v -> {
            switch (flashtype) {
                case OFF:
                    int image = getResources().getIdentifier("@drawable/ic_flash_auto", null, getPackageName());
                    flash.setImageDrawable(ContextCompat.getDrawable(this, image));
                    flashtype = FlashType.AUTO;
                    break;
                case AUTO:
                    image = getResources().getIdentifier("@drawable/ic_flash_on", null, getPackageName());
                    flash.setImageDrawable(ContextCompat.getDrawable(this, image));
                    flashtype = FlashType.ON;
                    camera.enableTorch();
                    break;
                default:
                    image = getResources().getIdentifier("@drawable/ic_flash_off", null, getPackageName());
                    flash.setImageDrawable(ContextCompat.getDrawable(this, image));
                    flashtype = FlashType.OFF;
                    camera.disableTorch();
                    break;
            }
        });
    }

    private void hideFlash() {
        if (!hasTorch()) return;
        if (camera.isTorchEnabled()) {
            camera.disableTorch();

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void focusImage(MotionEvent motionEvent) {
        camera.setFocus(motionEvent);
       // preview.setOnTouchListener((view, motionEvent) -> camera.setFocus(motionEvent));
    }

    private void onTapValidate() {
        validPicture.setOnClickListener(v -> validate());
    }

    private void onTapRefresh() {
        refreshPicture.setOnClickListener(v -> refresh());
    }

    private void enableCamera() {
        camera.enableCamera();
    }

    ScaleGestureDetector.SimpleOnScaleGestureListener listener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            canFocus = false;
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            canFocus = true;
            super.onScaleEnd(detector);
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            canFocus = false;
            float getPreviousSpan = detector.getPreviousSpan();
            float getCurrentSpan = detector.getCurrentSpan();
            if (previousSpan == getCurrentSpan) {
                return super.onScale(detector);
            }


            Log.i("Scale Factor", "" + detector.getScaleFactor());
            Log.i("Current span", "" + detector.getCurrentSpan());
            Log.i("Previous SPAN", "" + detector.getPreviousSpan());
            Log.i("Saved PreviousSpan", "" + previousSpan);
            Log.i("Current SPANX / SPANY", "" + detector.getCurrentSpanX() + " / " + detector.getCurrentSpanY());
            Log.i("previous SPANX / SPANY", "" + detector.getPreviousSpanX() + " / " + detector.getPreviousSpanY());

            ZoomState zoomState = camera.cameraInfo.getZoomState().getValue();
            float max = zoomState.getMaxZoomRatio();
            float min = zoomState.getMinZoomRatio();
            currentZoomProgress = detector.getScaleFactor() * 2;
            if (currentZoomProgress > max) currentZoomProgress = max;
            if (currentZoomProgress < min) currentZoomProgress = min;


            if(previousSpan > getCurrentSpan && getPreviousSpan <= previousSpan){
                camera.setZoom(min);
                return super.onScale(detector);
            }
            camera.setZoom(currentZoomProgress);
            previousSpan = getPreviousSpan;

            return super.onScale(detector);
        }
    };


    @SuppressLint("ClickableViewAccessibility")
    private void onSeekBarChange() {
        ScaleGestureDetector gd = new ScaleGestureDetector(this, listener);

        preview.setOnTouchListener((a, event) -> {
            int action = event.getAction();
            int pointerUp = MotionEvent.ACTION_POINTER_UP;
            int pointerDown = MotionEvent.ACTION_POINTER_DOWN;
            int actionUp = MotionEvent.ACTION_UP;
            int actionDown = MotionEvent.ACTION_DOWN;
            if (action == actionDown && canFocus) {
                //focusImage(event);
                return true;
            }
            if (action == pointerDown
                    || action == pointerUp
                    || action == actionUp
            ) return true;

            gd.onTouchEvent(event);
            return true;
        });
  /*      slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {


                if (Boolean.TRUE.equals(cameraViewModel.isDrag.getValue())) {

                    camera.setZoom(progress);

                } else {
                   
                    if (Math.abs(progress - i) > 1) {
                        seekBar.setProgress(progress);

                    } else {
                        cameraViewModel.isDrag.setValue(true);
                    }
                }


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i("OnStartTrackingTouch", "...00");
                progress = seekBar.getProgress();
                seekBar.setProgress(progress);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                cameraViewModel.isDrag.setValue(false);
                Log.i("Is drag on stop", " " + cameraViewModel.isDrag.getValue());

            }
        });
*/

    }

    private void onTapCloseCamera() {
        cancelPicture.setOnClickListener(v -> closeCamera());
    }

    private void onTapSwitchCamera() {
        btnClose.setOnClickListener(v -> {
            boolean backCamera = Boolean.TRUE.equals(cameraViewModel.backCamera.getValue());
            camera.bindCamera(!backCamera);
            cameraViewModel.backCamera.setValue(!backCamera);
        });
    }

    private void closeCamera() {
        camera.unbindAll();
        boolean imageRemoved = cameraViewModel.removeImage();
        Log.i("onTapCloseCamera", "Image removed: " + imageRemoved);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("path", "");
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private void refresh() {
        boolean imageRemoved = cameraViewModel.removeImage();
        Log.i("onRefreshCamera", "Image removed: " + imageRemoved);
        cameraViewModel.hide();
    }

    private void validate() {
        camera.unbindAll();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("path", cameraViewModel.picturePath.getValue());
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private void setLayout() {

        root = getView(R.id.cameraLayout);
        //    slider = getView(R.id.zoomSlider);
        preview = getView(R.id.cameraPreview);
        btnPicture = getView(R.id.buttTakePicture);
        btnClose = getView(R.id.buttCancelCamera);
        imgPreview = getView(R.id.imgPreview);
        handler = getView(R.id.handler);
        previewButtons = getView(R.id.previewButtons);
        cancelPicture = getView(R.id.cancelPicture);
        validPicture = getView(R.id.validPicture);
        refreshPicture = getView(R.id.refreshPicture);
        flash = getView(R.id.flash);
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("path", "");
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private <T> T getView(int resource) {
        return (T) this.findViewById(resource);
    }

}