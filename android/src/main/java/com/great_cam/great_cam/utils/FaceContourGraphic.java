package com.great_cam.great_cam.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.great_cam.great_cam.utils.GraphicOverlay;

import java.util.List;

/** Graphic instance for rendering face contours graphic overlay view. */
public class FaceContourGraphic extends GraphicOverlay.Graphic {

  private static final float FACE_POSITION_RADIUS = 10.0f;
  private static final float ID_TEXT_SIZE = 70.0f;
  private static final float ID_Y_OFFSET = 80.0f;
  private static final float ID_X_OFFSET = -70.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;

  private static final int[] COLOR_CHOICES = {
    Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW
  };
  private static int currentColorIndex = 0;

  private final Paint facePositionPaint;
  private final Paint idPaint;
  private final Paint boxPaint;

  private volatile Face face;
  private Rect imageRect;

  public FaceContourGraphic(GraphicOverlay overlay, Face face, Rect imageRect) {
    super(overlay);
    this.face = face;
    this.imageRect = imageRect;
    currentColorIndex = Color.YELLOW;
    final int selectedColor = currentColorIndex;//COLOR_CHOICES[currentColorIndex];

    facePositionPaint = new Paint();
    facePositionPaint.setColor(selectedColor);

    idPaint = new Paint();
    idPaint.setColor(selectedColor);
    idPaint.setTextSize(ID_TEXT_SIZE);

    boxPaint = new Paint();
    boxPaint.setColor(selectedColor);
    boxPaint.setStyle(Paint.Style.STROKE);
    boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
  }

  /**
   * Updates the face instance from the detection of the most recent frame. Invalidates the relevant
   * portions of the overlay to trigger a redraw.
   */
  public void updateFace(Face face) {
    this.face = face;
    postInvalidate();
  }

  /** Draws the face annotations for position on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    RectF rect  = calculateRect(
            (float)imageRect.height(),
            (float)imageRect.width(),
            face.getBoundingBox()
    );
    Paint paint = new Paint();
    paint.setTextSize(55.0f);
    paint.setColor(Color.WHITE);

    canvas.drawRect(rect,boxPaint);
    canvas.drawText("ERES UN PEDAZO DE CIPOTE", canvas.getWidth() / 3.0f, canvas.getHeight() / 2.0f, paint);
  }
}
