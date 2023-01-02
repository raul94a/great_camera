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

import com.great_cam.great_cam.utils.CameraHelper;
import com.great_cam.great_cam.viewmodels.CameraViewModel;

public class CameraActivity extends AppCompatActivity {
    private CameraHelper camera;
    private PreviewView preview;
    private ImageView btnPicture;
    private ImageView btnClose;
    private ImageView imgPreview;
    private LinearLayout handler;
    private LinearLayout previewButtons;
    private CameraViewModel cameraViewModel;
    private ConstraintLayout root;
    private ImageView cancelPicture, refreshPicture, validPicture;
    private String direction;


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
                preview.setVisibility(View.GONE);
                imgPreview.setVisibility(View.VISIBLE);
                handler.setVisibility(View.GONE);
                previewButtons.setVisibility(View.VISIBLE);
            } else {
                preview.setVisibility(View.VISIBLE);
                handler.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.GONE);
                imgPreview.setVisibility(View.GONE);
                imgPreview.setImageBitmap(null);

            }
        });
    }

    private void cameraActionsHandler() {
        focusImage();
        onTapCloseCamera();
        onTapRefresh();
        onTapValidate();
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


    GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {

            Log.i("MotionEnvet1", " " + e1);
            Log.i("MotionEnvet1", " " + e2);

            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    };


    ScaleGestureDetector.SimpleOnScaleGestureListener zoomListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            Log.i("onScale begin detector", " " + detector.getScaleFactor());
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            Log.i("onScale  detector", " " + detector.getScaleFactor());

            super.onScaleEnd(detector);
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {

            Log.i("Detector", " " + detector.getCurrentSpan());


            zoomHandler(detector);


            return super.onScale(detector);
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private void focusImage() {

//        root.setOnTouchListener((v, motionEvent)->{
//            return new ScaleGestureDetector(getApplicationContext(), zoomListener).onTouchEvent(motionEvent);
//        });

        preview.setOnTouchListener((view, motionEvent) -> {

            new ScaleGestureDetector(getApplicationContext(), zoomListener).onTouchEvent(motionEvent);
            return camera.setFocus(motionEvent);


        });
    }

    private void zoomHandler(ScaleGestureDetector detector) {
            if(Boolean.TRUE.equals(cameraViewModel.isUpdatingZoom.getValue())){
                return;
            }
            cameraViewModel.isUpdatingZoom.setValue(true);

            ZoomState state = camera.cameraInfo.getZoomState().getValue();
            Log.i("ZoomState", " " + state);
            float value = cameraViewModel.previousFocus.getValue();
            float x2 = 0.0f;
            x2 = detector.getCurrentSpanX();


            final float copyV = x2;
            Log.i("PreviousX", " " + x2);
            Log.i("ViewModelPreviousFocus", " " + value);

            if (copyV > value) {
                if (state.getZoomRatio() > 9.5f) {

                    cameraViewModel.setPreviousFocus(9999.0f);
                    cameraViewModel.isUpdatingZoom.setValue(false);
                    return;
                }
                camera.cameraControl.setZoomRatio(state.getZoomRatio() + 0.3f);
                cameraViewModel.setPreviousFocus(copyV);

            } else {
                if (state.getZoomRatio() == 1.1f) {
                    cameraViewModel.setPreviousFocus(0.0f);
                    cameraViewModel.isUpdatingZoom.setValue(false);

                    return;
                }
                camera.cameraControl.setZoomRatio(state.getZoomRatio() - 0.3f);
                cameraViewModel.setPreviousFocus(copyV);

            }

            cameraViewModel.isUpdatingZoom.setValue(false);



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

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("ImageCaptureException", exception.toString());
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
        preview = getView(R.id.cameraPreview);
        btnPicture = getView(R.id.buttTakePicture);
        btnClose = getView(R.id.buttCancelCamera);
        imgPreview = getView(R.id.imgPreview);
        handler = getView(R.id.handler);
        previewButtons = getView(R.id.previewButtons);
        cancelPicture = getView(R.id.cancelPicture);
        validPicture = getView(R.id.validPicture);
        refreshPicture = getView(R.id.refreshPicture);
    }

    private <T> T getView(int resource) {
        return (T) this.findViewById(resource);
    }

}