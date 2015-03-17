package com.js.gestApp;

import static com.js.basic.Tools.pr;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.js.android.MyTouchListener;
import com.js.android.UITools;
import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.basic.Rect;
import com.js.gest.GestureEventFilter;
import com.js.gest.StrokeSet;
import static com.js.basic.Tools.*;

public class ExperimentView extends UITools.OurBaseView implements
    GestureEventFilter.Listener {

  public ExperimentView(Context context) {
    super(context);
    setBackgroundColor(0xFFc0c0a0);

    MyTouchListener touchListener = buildTouchListener();

    mEventFilter = new GestureEventFilter();
    mEventFilter.prependTo(touchListener);
    mEventFilter.setListener(this);
  }

  private MyTouchListener buildTouchListener() {
    MyTouchListener touchListener = new MyTouchListener() {
      // We don't need to include onTouchEvent(), but we do so to
      // verify that we still get non-gesture-related motions
      @Override
      public boolean onTouch(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_MOVE)
          pr("ExperimentView ignoring non-gesture event " + UITools.dump(event));
        invalidate();
        return false;
      }
    };
    touchListener.setView(this);
    return touchListener;
  }

  @Override
  public void onDraw(Canvas canvas) {
    onDrawAux(canvas);
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
      Rect r = new Rect(pti.x, pti.y, pti.x + 20, pti.y + 20);
      canvas.drawRect(r.toAndroid(), mPaintFill);
    }
  }

  @Override
  public void strokeSetExtended(StrokeSet strokeSet) {
  }

  @Override
  public void processGesture(String gestureName) {
    unimp("process gesture: " + gestureName);
  }

  private GestureEventFilter mEventFilter;
  private int mFrame;
  private Paint mPaintOutline, mPaintFill;
}
