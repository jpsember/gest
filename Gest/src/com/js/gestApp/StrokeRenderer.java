package com.js.gestApp;

import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.gest.Stroke;
import com.js.gest.StrokeSet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Encapsulates rendering tasks for strokes
 */
public class StrokeRenderer {

  public StrokeRenderer() {
    Paint p = new Paint();
    mPaintOutline = p;
    p.setColor(Color.WHITE);
    p.setStrokeWidth(1.2f);
    p.setStyle(Paint.Style.STROKE);

    p = new Paint();
    mPaintFill = p;
    p.setColor(Color.WHITE);
    p.setStrokeWidth(3.4f);
  }

  public void startRender(Canvas c) {
    mCanvas = c;
  }

  public void stopRender() {
    mCanvas = null;
  }

  public void drawStrokeSet(StrokeSet set, Rect bounds, boolean detailed) {
    for (Stroke s : set) {
      drawStroke(s, bounds, detailed);
    }
  }

  private void drawStroke(Stroke s, Rect bounds, boolean detailed) {
    float px = 0, py = 0;
    for (int i = 0; i < s.size(); i++) {
      Point point = s.getPoint(i);
      float x = point.x;
      float y = point.y;
      y = bounds.endY() - (y - bounds.y);
      if (i != 0)
        mCanvas.drawLine(px, py, x, y, mPaintFill);
      px = x;
      py = y;
      if (detailed) {
        mCanvas.drawCircle(px, py, 8, mPaintOutline);
      }
    }
  }

  private Paint mPaintFill;
  private Paint mPaintOutline;
  private Canvas mCanvas;

}
