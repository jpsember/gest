package com.js.gest;

import com.js.basic.Point;
import com.js.basic.Rect;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class GesturePanel {

  public GesturePanel(View container) {
    mContainer = container;
  }

  public void draw(Canvas canvas) {
    Rect r = getFloatingViewBounds();
    Paint p = new Paint();
    p.setColor(0x40808080);
    p.setStrokeWidth(1.2f);
    canvas.drawRect(r.toAndroid(), p);
  }

  private Rect getFloatingViewBounds() {
    if (mFloatingViewBounds == null) {
      View view = mContainer;
      Rect r = new Rect(0, 0, view.getWidth(), view.getHeight());
      float size = Math.min(r.width, r.height) / 2;
      float ASPECT_RATIO = .7f;
      r = new Rect(r.endX() - size, r.endY() - size * ASPECT_RATIO, size, size
          * ASPECT_RATIO);
      float PADDING = 16;
      r.inset(PADDING, PADDING);
      mFloatingViewBounds = r;
    }
    return mFloatingViewBounds;
  }

  public boolean containsPoint(Point point) {
    Rect r = getFloatingViewBounds();
    return r.contains(point);
  }

  private Rect mFloatingViewBounds;
  private View mContainer;
}
