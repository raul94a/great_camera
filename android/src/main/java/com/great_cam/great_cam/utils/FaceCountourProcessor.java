package com.great_cam.great_cam.utils;

import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceCountourProcessor extends BaseImageAnalyzer<List<Face>>{
    public FaceDetectorOptions realTimeOpts =
            new FaceDetectorOptions.Builder()
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();

    public FaceDetector detector = FaceDetection.getClient(realTimeOpts);

    FaceCountourProcessor(GraphicOverlay overlay) {
        super(overlay);
    }

    @Override
    protected Task<List<Face>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(List<Face> results, GraphicOverlay graphicOverlay, Rect rect) {
        graphicOverlay.clear();
        for(Face f : results){
            FaceContourGraphic graph = new FaceContourGraphic(graphicOverlay, f, rect);
            graphicOverlay.add(graph);
        }

    }

    @Override
    protected void onFailure(Exception e) {
        Log.e("EXCEPTION", e.toString());
    }

    public void stop(){
        detector.close();;
    }
}
