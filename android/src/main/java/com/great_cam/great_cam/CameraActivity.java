package com.great_cam.great_cam;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

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
import com.great_cam.great_cam.utils.GraphicOverlay;
import com.great_cam.great_cam.viewmodels.CameraViewModel;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

enum FlashType {
    AUTO, OFF, ON
}

public class CameraActivity extends AppCompatActivity {
    private VideoView videoPreview;
    private TextView optionPicture, optionVideo, timer;
    private ImageView cancelPicture, refreshPicture, validPicture, flash, btnSwitch, imgPreview, btnPicture, btnStartVideoPreview;
    private LinearLayout handler, previewButtons, optionSelector;
    private CameraHelper camera;
    private PreviewView preview;
    private CameraViewModel cameraViewModel;
    private ConstraintLayout root;
    private GraphicOverlay overlay;
    // private SeekBar slider;
    private FlashType flashtype = FlashType.OFF;
    private MediaController mc;
    private float previousSpan;
    private boolean canFocus;
    private int seconds;
    private ExecutorService ex;
    private Looper loop;
    private MediaPlayer mp;

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


                optionSelector.setVisibility(View.GONE);
                preview.setVisibility(View.GONE);

                handler.setVisibility(View.GONE);
                previewButtons.setVisibility(View.VISIBLE);
                if (Boolean.FALSE.equals(cameraViewModel.isVideoActive.getValue())) {
                    imgPreview.setVisibility(View.VISIBLE);
                } else {

                    if (cameraViewModel.picturePath.getValue() != null && !cameraViewModel.picturePath.getValue().isEmpty()) {

                        Log.i("Cache dir", cameraViewModel.picturePath.getValue());
                        String path = cameraViewModel.picturePath.getValue();
                        File file = new File(path);


                        Uri uri = Uri.fromFile(file);
                        Log.i("URI", "" + uri);

                        if (file.exists()) {


                            file.setReadable(true, false);
                            mc = new MediaController(this);
                            btnStartVideoPreview.setVisibility(View.VISIBLE);
                            videoPreview.setVisibility(View.VISIBLE);


                            btnStartVideoPreview.setOnClickListener(a -> {
                                videoPreview.setVideoPath(file.getAbsolutePath());
                                mc.setMediaPlayer(videoPreview);
                                videoPreview.releasePointerCapture();


                                mc.show();
                                videoPreview.start();
                                btnStartVideoPreview.setVisibility(View.GONE);


                            });
                            videoPreview.setOnErrorListener((error, a, cb) -> {
                                Log.i("Error", "" + error.toString());
                                Log.i("Error", "" + a + " " + cb);
                                return false;
                            });
                            videoPreview.setOnCompletionListener(MediaPlayer::stop);

                        }

                    }
                }

            } else {
                optionSelector.setVisibility(View.VISIBLE);
                preview.setVisibility(View.VISIBLE);
                handler.setVisibility(View.VISIBLE);
                previewButtons.setVisibility(View.GONE);
                imgPreview.setVisibility(View.GONE);
                videoPreview.setVisibility(View.GONE);
                videoPreview.setMediaController(null);
                imgPreview.setImageBitmap(null);
                btnStartVideoPreview.setVisibility(View.GONE);
                if (btnStartVideoPreview.hasOnClickListeners()) {
                    btnStartVideoPreview.setOnClickListener(null);
                }

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
                startTimer();
                handler.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
                optionSelector.setVisibility(View.GONE);
                // btnSwitch.setVisibility(View.GONE);
                btnPicture.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_video_stop));
            } else {
                stopTimer();
                handler.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
                boolean isVideoActive = Boolean.TRUE.equals(cameraViewModel.isVideoActive.getValue());
                optionSelector.setVisibility(View.VISIBLE);
                //   btnSwitch.setVisibility(View.VISIBLE);
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
        camera = new CameraHelper(this.getApplicationContext(), this, preview,overlay);

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
            boolean backCamera = Boolean.TRUE.equals(cameraViewModel.backCamera.getValue());
            if (Boolean.FALSE.equals(cameraViewModel.isVideoActive.getValue())) {
                return;
            }
            cameraViewModel.isVideoActive.setValue(false);
            camera.bindCamera(backCamera);
        });
    }

    private void onTapVideoOption() {
        optionVideo.setOnClickListener(v -> {
            boolean backCamera = Boolean.TRUE.equals(cameraViewModel.backCamera.getValue());
            if (Boolean.TRUE.equals(cameraViewModel.isVideoActive.getValue())) {
                return;
            }
            cameraViewModel.isVideoActive.setValue(true);
            camera.bindCameraVideo(backCamera);
        });

    }

    private void onTapTakePicture() {
        btnPicture.setOnClickListener(v -> {

            if (Boolean.TRUE.equals(cameraViewModel.isVideoRunning.getValue())) {
                camera.stopVideo(options -> {
    Log.i("OPTIONS?", " " + options);
                    Log.i("OPTIONS?", " " + options);
                    Log.i("OPTIONS?", " " + options);
                    Log.i("OPTIONS?", " " + options);
                    Log.i("OPTIONS?", " " + options);
                    Log.i("OPTIONS?", " " + options);
                    Log.i("OPTIONS?", " " + options);
                    Log.i("OPTIONS?", " " + options);








                    cameraViewModel.picturePath.setValue(options);
                });
                camera.disableTorch();
                cameraViewModel.show();
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
            float currentZoomProgress = detector.getScaleFactor() * 2;
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
                if (Boolean.TRUE.equals(cameraViewModel.isVideoRunning.getValue())) {
                    sleep(75);
                    camera.captureVideo();
                }
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
        if (Boolean.TRUE.equals(cameraViewModel.isVideoActive.getValue())) {
            try{

            videoPreview.releasePointerCapture();
            videoPreview.stopPlayback();
            videoPreview.suspend();
            videoPreview.clearAnimation();
            mc.clearAnimation();
            videoPreview.setVideoURI(null);

            mc.hide();
            cameraViewModel.isVideoRunning.setValue(false);
            }catch (Exception ignore){}

        }
        boolean imageRemoved = cameraViewModel.removeImage();
        Log.i("onRefreshCamera", "Image removed: " + imageRemoved);
        try {
            Thread.sleep(50);
        } catch (Exception ignore) {
        }
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
        btnSwitch = getView(R.id.switchCamera);
        imgPreview = getView(R.id.imgPreview);
        videoPreview = getView(R.id.videoPreview);
        handler = getView(R.id.handler);
        previewButtons = getView(R.id.previewButtons);
        cancelPicture = getView(R.id.cancelPicture);
        validPicture = getView(R.id.validPicture);
        refreshPicture = getView(R.id.refreshPicture);
        optionPicture = getView(R.id.optionPicture);
        optionVideo = getView(R.id.optionVideo);
        flash = getView(R.id.flash);
        optionSelector = getView(R.id.optionSelector);
        timer = getView(R.id.timer);
        btnStartVideoPreview = getView(R.id.btnStartVideoPreview);
        overlay = getView(R.id.overlay);


    }

    @Override
    public void onBackPressed() {
        //TODO: ONLY FOR TESTING
        if (cameraViewModel.isVideoActive.getValue()) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("path", cameraViewModel.picturePath.getValue());
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
            return;

        }
        Intent returnIntent = new Intent();
        returnIntent.putExtra("path", "");
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private void startTimer() {
        timer.setVisibility(View.VISIBLE);
        loop = Looper.getMainLooper();
        Handler handler = new Handler(loop);

        ex = Executors.newSingleThreadExecutor();

        ex.execute(() -> {
            while (true) {

                if (ex == null) break;

                handler.post(() -> {
                    ++seconds;
                    setTimerView();

                });


                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

    }

    private void stopTimer() {

        if (ex == null) return;
        timer.setVisibility(View.GONE);
        ex.shutdown();
        seconds = 0;
        ex = null;
        loop = null;
        setTimerView();
    }

    @SuppressLint("SetTextI18n")
    private void setTimerView() {
        if (seconds < 60) {
            timer.setText("00:" + (seconds < 10 ? "0" + seconds : seconds));
        } else if (seconds < 3600) {
            int minutes = this.seconds / 60;
            int seconds = this.seconds % 60;
            boolean shouldModifyMinutes = minutes < 10;
            boolean shouldModifySeconds = seconds < 10;
            String minutesString = shouldModifyMinutes ? "0" + minutes : "" + minutes;
            String secondsString = shouldModifySeconds ? "0" + seconds : "" + seconds;
            timer.setText(minutesString + ":" + secondsString);
        } else {
            int hours = this.seconds / 3600;
            int minutes = (this.seconds % 3600) / 60;
            int seconds = this.seconds - (hours * 3600 + minutes * 60);
            boolean shouldModifyHours = hours < 10;
            boolean shouldModifyMinutes = minutes < 10;
            boolean shouldModifySeconds = seconds < 10;
            String mHours = shouldModifyHours ? "0" + hours : "" + hours;
            String mMin = shouldModifyMinutes ? "0" + minutes : "" + minutes;
            String mSeconds = shouldModifySeconds ? "0" + seconds : "" + seconds;
            timer.setText(mHours + ":" + mMin + ":" + mSeconds);


        }
    }

    private <T> T getView(int resource) {
        return (T) this.findViewById(resource);
    }


    ///LifeCycle


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy", cameraViewModel.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("onResume", cameraViewModel.toString());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("onPause", cameraViewModel.toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("onStop", cameraViewModel.toString());
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignore) {
        }
    }
}