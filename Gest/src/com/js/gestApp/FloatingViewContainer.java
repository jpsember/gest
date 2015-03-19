package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.js.android.MyTouchListener;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.gest.GestureEventFilter;
import com.js.gest.GestureSet;
import static com.js.basic.Tools.*;

public class FloatingViewContainer extends View {

  public FloatingViewContainer(Context context, GestureSet gestures,
      GestureEventFilter.Listener listener, MyTouchListener touchListener) {
    super(context);
    doNothing();
    setBackgroundColor(0xFFe0e0e0);

    touchListener.setView(this);

    mEventFilter = new GestureEventFilter();
    mEventFilter.setViewMode(GestureEventFilter.MODE_FLOATINGVIEW);
    mEventFilter.prependTo(touchListener);
    mEventFilter.setListener(listener);
    mEventFilter.setGestures(gestures);

  }

  @Override
  public void onDraw(Canvas canvas) {
    onDrawAux(canvas);
    mEventFilter.draw(canvas);
  }

  private void onDrawAux(Canvas canvas) {
    if (mPaintOutline == null) {
      Paint p = new Paint();
      p.setColor(0xff408040);
      p.setStrokeWidth(1.2f);
      p.setStyle(Paint.Style.STROKE);
      mPaintOutline = p;

      p = new Paint();
      p.setColor(0xff408040);
      p.setStrokeWidth(3.4f);
      mPaintFill = p;
    }

    // Draw a little square bouncing back and forth
    {
      Point pt1 = new Point(10, 10);
      Point pt2 = new Point(200, 200);
      mFrame++;
      int duration = 20;
      int x = mFrame % (2 * duration);
      float t = x / (float) duration;
      if (t >= 1)
        t = 2 - t;
      Point pti = MyMath.interpolateBetween(pt1, pt2, t);
      Rect r = new Rect(pti.x, pti.y, 20, 20);
      canvas.drawRect(r.toAndroid(), mPaintFill);
    }
  }

  private GestureEventFilter mEventFilter;
  private int mFrame;
  private Paint mPaintOutline, mPaintFill;
}
