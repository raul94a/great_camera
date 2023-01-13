package com.great_cam.great_cam.utils;


import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


abstract public class BaseImageAnalyzer<T> implements ImageAnalysis.Analyzer {

    public GraphicOverlay graphicOverlay;

    BaseImageAnalyzer(GraphicOverlay overlay) {
        graphicOverlay = overlay;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        @SuppressLint("UnsafeOptInUsageError") Image img = image.getImage();
        InputImage inputImage = InputImage.fromMediaImage(img, image.getImageInfo().getRotationDegrees());
        detectInImage(inputImage).addOnSuccessListener(results -> {
            onSuccess(results, graphicOverlay, image.getCropRect());

        }).addOnCompleteListener(task -> {
            ExecutorService ex = Executors.newSingleThreadExecutor();

            ex.execute(() -> {
                ThreadSleeper.sleep(750);
                image.close();
            });
        });

    }

    abstract protected Task<T> detectInImage(InputImage image);

    protected abstract void onSuccess(
            T results, GraphicOverlay graphicOverlay, Rect rect
    );

    protected abstract void onFailure(Exception e);
}
