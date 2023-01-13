package com.great_cam.great_cam.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoOutput;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;


import org.mp4parser.BasicContainer;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.AppendTrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CameraHelper {
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    // Real-time contour detection
    FaceDetectorOptions realTimeOpts =
            new FaceDetectorOptions.Builder()
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();

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
    private GraphicOverlay overlay;
    private PreviewView previewView;
    private Preview preview;
    private CameraSelector cameraSelector;
    public ProcessCameraProvider provider;
    public CameraControl cameraControl;
    public int takePictureDelay = 250;
    public Recorder recorder;
    public VideoCapture<VideoOutput> videoCapture;
    public PendingRecording pendingRecording;
    public Recording recording;
    public FileOutputOptions options;
    private final QualitySelector qualitySelector =
            QualitySelector.fromOrderedList(Arrays.asList(/*Quality.UHD, */Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD));
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private List<String> recordingPaths = new ArrayList<>();


    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    public CameraHelper(Context context, LifecycleOwner lifecycleOwner, PreviewView view,GraphicOverlay overlay) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = view;
        this.overlay = overlay;

    }

    public void unbindAll() {
        if (provider != null) {
            provider.unbindAll();
        }
    }

    public void setZoom(float value) {
        camera.getCameraControl().setZoomRatio(value);
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
        if (cameraControl == null) return;
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

        createPreview();
        createImageCapture();
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
        if (autoFlash) {
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

    public Consumer<VideoRecordEvent> captureListener = videoRecordEvent -> {

    };

    public void captureVideo() {

        options = new FileOutputOptions.Builder(new File(context.getFilesDir(), System.currentTimeMillis() + ".mp4")).build();


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        }
        pendingRecording = recorder.prepareRecording(context, options).withAudioEnabled();


        recording = pendingRecording.start(
                ContextCompat.getMainExecutor(context),
                captureListener
        );


    }

    public void stopVideo(VideoFile videoFile) {

        stopVideo();

        try {
            Thread.sleep(200);
        } catch (Exception ignore) {
        }
        if (recordingPaths.size() > 1) {
            Looper looper = Looper.getMainLooper();
            ExecutorService ex = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(looper);
            String path = context.getFilesDir() + "/" + System.currentTimeMillis() + ".mp4";

            ex.execute(() -> {

                joinVideos(path);
                handler.post(() -> {
                    videoFile.getVideo(path);
                    recordingPaths.clear();
                });
            });
        } else {

            videoFile.getVideo(options.getFile().getAbsolutePath());
        }


    }

    private void joinVideos(String path) {
        try {
            int length = recordingPaths.size();
            Movie[] inMovies = new Movie[length];

            for (int i = 0; i < length; i++) {
                inMovies[i] = MovieCreator.build(recordingPaths.get(i));
            }


            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();

            for (Movie m : inMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                    if (t.getHandler().equals("vide")) {
                        videoTracks.add(t);
                    }
                }
            }

            Movie result = new Movie();

            if (audioTracks.size() > 0) {
                result.addTrack(new AppendTrack(audioTracks
                        .toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                result.addTrack(new AppendTrack(videoTracks
                        .toArray(new Track[videoTracks.size()])));
            }

            BasicContainer out = (BasicContainer) new DefaultMp4Builder().build(result);

            @SuppressWarnings("resource")
            FileChannel fc = new RandomAccessFile(path, "rw").getChannel();
            out.writeContainer(fc);
            fc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void stopVideo() {
        recording.stop();
        recording.close();

        recording = null;
        pendingRecording = null;
        File f = options.getFile();
        String path = f.getAbsolutePath();
        f.setReadable(true, false);
        f.setExecutable(true, false);
        f.setWritable(true, false);
        recordingPaths.add(path);
        Log.i("Adding the path to recording", path);
    }

    public void pauseVideo() {
        recording.pause();
    }

    public void resumeVideo() {
        recording.resume();
    }

    public void closeVideo() {
        recording.close();
    }

    private void bindBackCameraVideo() {
        if (recorder != null) {
            stopVideo();
        }
        provider.unbindAll();


        recorder = new Recorder.Builder()
                .setExecutor(ContextCompat.getMainExecutor(context))
                .setQualitySelector(qualitySelector)
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        try {
            camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture);
        } catch (Exception e) {
            Log.e("Exception", e.toString());
        }

    }

    private void bindFrontCameraVideo() {
        if (recorder != null) {
            stopVideo();
        }
        provider.unbindAll();

        recorder = new Recorder.Builder()
                .setExecutor(ContextCompat.getMainExecutor(context))

                .setQualitySelector(qualitySelector)
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        try {
            if (provider.hasCamera(cameraSelector)) {
                camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture);

                cameraInfo = camera.getCameraInfo();
                cameraControl = camera.getCameraControl();

            }

        } catch (CameraInfoUnavailableException exp) {
            Log.i("Binding front camera exception", exp.toString());
            bindBackCameraVideo();
        }
    }

    private void bindBackCamera() {
        //need to check if the call is made in recording mode
        //this means that the camera is change between front/back

        provider.unbindAll();
        cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        ImageAnalysis ia = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        ia.setAnalyzer(ContextCompat.getMainExecutor(context), new FaceCountourProcessor(overlay));
        camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture,ia);
        cameraInfo = camera.getCameraInfo();


        cameraControl = camera.getCameraControl();

    }

    private void bindFrontCamera() {

        provider.unbindAll();
        cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        try {
            if (provider.hasCamera(cameraSelector)) {
                ImageAnalysis ia = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                ia.setAnalyzer(ContextCompat.getMainExecutor(context), new FaceCountourProcessor(overlay));
                camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture,ia);
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

    public void bindCameraVideo(boolean backCamera) {
        if (backCamera) {
            bindBackCameraVideo();
        } else {
            bindFrontCameraVideo();
        }
    }

    public void setTakePictureDelay(int delay) {
        if (delay <= 0) {
            takePictureDelay = 250;
            return;
        }

        takePictureDelay = delay;

    }

    public interface VideoFile {
        void getVideo(String path);
    }


}


