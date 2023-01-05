package com.great_cam.great_cam;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ZoomState;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.great_cam.great_cam.utils.CameraHelper;
import com.great_cam.great_cam.viewmodels.CameraViewModel;

import java.util.Objects;

enum FlashType {
    AUTO, OFF, ON
}

public class CameraActivity extends AppCompatActivity {
    private TextView optionPicture, optionVideo;
    private ImageView cancelPicture, refreshPicture, validPicture, flash, btnSwitch, imgPreview, btnPicture;
    private LinearLayout handler, previewButtons, optionSelector;
    private CameraHelper camera;
    private PreviewView preview;
    private CameraViewModel cameraViewModel;
    private ConstraintLayout root;
    // private SeekBar slider;
    private FlashType flashtype = FlashType.OFF;


    private final String VIDEO_PLAY = "@drawable/ic_video_stop";
    private final String VIDEO_STOP = "@drawable/ic_video_play";


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
                preview.setVisibility(View.GONE);
                handler.setVisibility(View.GONE);
                imgPreview.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.VISIBLE);
            } else {
                preview.setVisibility(View.VISIBLE);
                handler.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.GONE);
                imgPreview.setVisibility(View.GONE);
                imgPreview.setImageBitmap(null);

            }
        });

        cameraViewModel.isVideoActive.observe(this, videoActive -> {
            if (camera != null) {
                turnFlashOff();
            }
            if (videoActive) {
                btnPicture.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_video_play));
                optionVideo.setTypeface(null, Typeface.BOLD);
                optionVideo.setTextSize(20.0f);
                optionPicture.setTextSize(16.0f);
            } else {
                btnPicture.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_btn_camera));
                optionPicture.setTypeface(null, Typeface.BOLD);
                optionPicture.setTextSize(20.0f);
                optionVideo.setTextSize(16.0f);
            }
        });

        cameraViewModel.isVideoRunning.observe(this, videoRunning -> {
            if (videoRunning) {
                optionSelector.setVisibility(View.GONE);
                btnSwitch.setVisibility(View.GONE);
                btnPicture.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_video_stop));
            } else {
                boolean isVideoActive = Boolean.TRUE.equals(cameraViewModel.isVideoActive.getValue());
                optionSelector.setVisibility(View.VISIBLE);
                btnSwitch.setVisibility(View.VISIBLE);
                btnPicture.setImageDrawable(ContextCompat.getDrawable(this, isVideoActive ? R.drawable.ic_video_play : R.drawable.ic_btn_camera));
            }
        });
    }

    private void cameraActionsHandler() {
        onSeekBarChange();
        onTapOptionController();
        onTapSwitchCamera();
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


    private void onTapOptionController() {
        onTapPictureOption();
        onTapVideoOption();
    }

    private void onTapPictureOption() {
        optionPicture.setOnClickListener(v -> {

            if (Boolean.FALSE.equals(cameraViewModel.isVideoActive.getValue())) {
                return;
            }
            cameraViewModel.isVideoActive.setValue(false);
            camera.bindCamera(true);
        });
    }

    private void onTapVideoOption() {
        optionVideo.setOnClickListener(v -> {


            if (Boolean.TRUE.equals(cameraViewModel.isVideoActive.getValue())) {
                return;
            }
            cameraViewModel.isVideoActive.setValue(true);
            camera.bindCameraVideo(true);
        });

    }

    private void onTapTakePicture() {
        btnPicture.setOnClickListener(v -> {

            if (Boolean.TRUE.equals(cameraViewModel.isVideoRunning.getValue())) {
                camera.stopVideo();
                camera.disableTorch();
                cameraViewModel.isVideoRunning.setValue(false);
                return;
            }

            if (Boolean.TRUE.equals(cameraViewModel.isVideoActive.getValue())) {
                Log.e("onTapPlayVideo", "video is active");
                if (camera.recorder != null) {
                    Log.e("onTapPlayVideo", "Start capture video");

                    camera.captureVideo();
                    if (FlashType.AUTO == flashtype) {
                        camera.enableTorch();
                    }
                    cameraViewModel.isVideoRunning.setValue(true);
                }


                return;
            }

            camera.capturePhoto(new ImageCapture.OnImageSavedCallback() {
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
            }, flashtype == FlashType.AUTO);


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
                    turnFlashOff();
                    break;
            }
        });
    }

    private void turnFlashOff() {
        int image;
        image = getResources().getIdentifier("@drawable/ic_flash_off", null, getPackageName());
        flash.setImageDrawable(ContextCompat.getDrawable(this, image));
        flashtype = FlashType.OFF;
        camera.disableTorch();
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


            if (previousSpan > getCurrentSpan && getPreviousSpan <= previousSpan) {
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

    }

    private void onTapCloseCamera() {
        cancelPicture.setOnClickListener(v -> closeCamera());
    }

    private void onTapSwitchCamera() {
        btnSwitch.setOnClickListener(v -> {
            boolean backCamera = Boolean.TRUE.equals(cameraViewModel.backCamera.getValue());
            if (Boolean.TRUE.equals(cameraViewModel.isVideoActive.getValue())) {
                camera.bindCameraVideo(!backCamera);
            } else {
                camera.bindCamera(!backCamera);

            }
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
        btnSwitch = getView(R.id.switchCamera);
        imgPreview = getView(R.id.imgPreview);
        handler = getView(R.id.handler);
        previewButtons = getView(R.id.previewButtons);
        cancelPicture = getView(R.id.cancelPicture);
        validPicture = getView(R.id.validPicture);
        refreshPicture = getView(R.id.refreshPicture);
        optionPicture = getView(R.id.optionPicture);
        optionVideo = getView(R.id.optionVideo);
        flash = getView(R.id.flash);
        optionSelector = getView(R.id.optionSelector);
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