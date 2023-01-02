package com.great_cam.great_cam;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
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
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;
import com.great_cam.great_cam.utils.CameraHelper;
import com.great_cam.great_cam.viewmodels.CameraViewModel;

public class CameraActivity extends AppCompatActivity {
    private ImageView cancelPicture, refreshPicture, validPicture, flash, btnClose, imgPreview, btnPicture;
    private LinearLayout handler, previewButtons;
    private CameraHelper camera;
    private PreviewView preview;
    private CameraViewModel cameraViewModel;
    private ConstraintLayout root;
    private SeekBar slider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        setLayout();
        cameraViewModel = new ViewModelProvider(this, ViewModelProvider.Factory.from(CameraViewModel.initializer)).get(CameraViewModel.class);
        startCameraStreaming();
        cameraActionsHandler();
        observe();

    }

    private void observe() {
        cameraViewModel.showPreview.observe(this, show -> {
            if (show) {
                slider.setVisibility(View.GONE);
                preview.setVisibility(View.GONE);
                handler.setVisibility(View.GONE);
                imgPreview.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.VISIBLE);
            } else {
                slider.setVisibility(View.VISIBLE);
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
        focusImage();
        onTapCloseCamera();
        onTapRefresh();
        onTapValidate();
        changeTorchStatus();
    }

    private void setCameraHandler() {
        if (camera != null) {
            camera = null;
        }
        camera = new CameraHelper(this.getApplicationContext(), this, preview);

    }

    private void startCameraStreaming() {
        setCameraHandler();
        onClickTakePicture();
    }


    private boolean hasTorch() {
        if (camera == null) return false;
        return camera.hasTorch();
    }

    private void changeTorchStatus() {
        if (!hasTorch()) return;

        flash.setOnClickListener(v -> {
            if (camera.isTorchEnabled()) {
                camera.disableTorch();
                int image = getResources().getIdentifier("@drawable/ic_flash_off", null, getPackageName());
                flash.setImageDrawable(getResources().getDrawable(image));
            } else {
                camera.enableTorch();
                int image = getResources().getIdentifier("@drawable/ic_flash_on", null, getPackageName());
                flash.setImageDrawable(getResources().getDrawable(image));
            }
        });
    }

    private void hideFlashIfNoTorch() {
        if (!hasTorch()) {
            flash.setVisibility(View.INVISIBLE);
            flash.setEnabled(false);
        }
    }

    private void hideFlash() {
        if (!hasTorch()) return;
        if (camera.isTorchEnabled()) {
            camera.disableTorch();

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void focusImage() {
        preview.setOnTouchListener((view, motionEvent) -> camera.setFocus(motionEvent));
    }

    private void onTapValidate() {
        validPicture.setOnClickListener(v -> validate());
    }

    private void onTapRefresh() {
        refreshPicture.setOnClickListener(v -> refresh());
    }

    private void onClickTakePicture() {
        camera.enableCamera(btnPicture, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Log.i("onImageSaved", "" + outputFileResults.getSavedUri());
                String path = outputFileResults.getSavedUri().getPath();
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
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void onSeekBarChange() {


        slider.setOnTouchListener((view, motionEvent) -> false);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int progress = seekBar.getProgress();
                Log.i("SeekBar is changing", " " + progress);
                Log.i("SeekBar is changing", " " + i);
                camera.setZoom(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void onTapCloseCamera() {

        cancelPicture.setOnClickListener(v -> closeCamera());
        btnClose.setOnClickListener(view -> closeCamera());

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
        slider = getView(R.id.zoomSlider);
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

    private <T> T getView(int resource) {
        return (T) this.findViewById(resource);
    }

}