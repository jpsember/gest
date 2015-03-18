package com.js.gest;

import com.js.basic.Point;
import com.js.basic.Rect;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import static com.js.basic.Tools.*;

public class GesturePanel {

  private static final float PADDING = 16.0f;
  private static final float MINIMIZED_HEIGHT = 32;

  /**
   * Constructor
   * 
   * @param container
   *          the View to contain the panel
   */
  public GesturePanel(View container) {
    mContainer = container;
    if (false)
      pr("");
  }

  /**
   * Draw the panel to a canvas; should be called by a View's onDraw() method
   */
  public void draw(Canvas canvas) {
    Rect r = getBounds();
    Paint paint = new Paint();
    paint.setColor(0x40808080);
    paint.setStrokeWidth(1.2f);

    fillRoundedRect(canvas, r, 16.0f, paint);
  }

  /**
   * Determine if the panel contains a point
   */
  public boolean containsPoint(Point point) {
    Rect r = getBounds();
    return r.contains(point);
  }

  private void fillRoundedRect(Canvas canvas, Rect rect, float radius,
      Paint paint) {
    Path path = new Path();
    path.moveTo(rect.x + radius, rect.y);
    path.lineTo(rect.endX() - radius, rect.y);
    path.quadTo(rect.endX(), rect.y, rect.endX(), rect.y + radius);
    path.lineTo(rect.endX(), rect.endY() - radius);
    path.quadTo(rect.endX(), rect.endY(), rect.endX() - radius, rect.endY());
    path.lineTo(rect.x + radius, rect.endY());
    path.quadTo(rect.x, rect.endY(), rect.x, rect.endY() - radius);
    path.lineTo(rect.x, rect.y + radius);
    path.quadTo(rect.x, rect.y, rect.x + radius, rect.y);
    canvas.drawPath(path, paint);
  }

  private Rect getBounds() {
    if (mViewBoundsNormal == null) {
      View view = mContainer;

      float minWidth = 340;
      float minHeight = 240;

      float width = Math.min(view.getWidth(), minWidth);
      float height = Math.min(view.getHeight(), minHeight);

      Rect rect = new Rect(view.getWidth() - width, view.getHeight() - height,
          width, height);

      rect.inset(PADDING, PADDING);
      mViewBoundsNormal = rect;

      mViewBoundsMinimized = new Rect(rect.x, rect.endY() - MINIMIZED_HEIGHT,
          rect.width, MINIMIZED_HEIGHT);
    }
    return isMinimized() ? mViewBoundsMinimized : mViewBoundsNormal;
  }

  public boolean isMinimized() {
    return mMinimized;
  }

  public void setMinimized(boolean state) {
    if (mMinimized == state)
      return;

    mMinimized = state;
    mContainer.invalidate();
  }

  private Rect mViewBoundsNormal;
  private Rect mViewBoundsMinimized;
  private View mContainer;
  private boolean mMinimized;

}
