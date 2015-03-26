package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.js.gest.Stroke;
import com.js.gest.StrokeSet;
import com.js.basic.Point;
import com.js.basic.Rect;

import static com.js.basic.Tools.*;

/**
 * View for inputting and rendering user touch sequences (in a more expressive
 * manner than the GesturePanel)
 */
public class TouchView extends View {

  public TouchView(Context context, Listener listener) {
    super(context);
    doNothing();
    mListener = listener;
    setBackgroundColor(Color.BLUE);
    mRenderer = new StrokeRenderer();
    setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        // Avoid Eclipse warnings:
        if (alwaysFalse())
          view.performClick();
        if (!mReceivingGesture
            && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
          mReceivingGesture = true;
        }
        if (mReceivingGesture) {
          processGestureEvent(event);
          if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            mReceivingGesture = false;
          }
        }
        return true;
      }
    });
  }

  /**
   * Render the display stroke set, or if it's null, the touch stroke set; if
   * both are null, do nothing
   */
  private void onDrawAux() {
    StrokeSet set = mDisplayStrokeSet;
    if (set == null)
      set = mTouchStrokeSet;
    if (set == null)
      return;
    Rect r = new Rect(0, 0, getWidth(), getHeight());
    mRenderer.drawStrokeSet(set, r, true);
  }

  @Override
  public void onDraw(Canvas canvas) {
    mRenderer.startRender(canvas);
    onDrawAux();
    mRenderer.stopRender();
  }

  public void setDisplayStrokeSet(StrokeSet set) {
    if (set != mDisplayStrokeSet) {
      if (set != null) {
        if (StrokeSet.SHOW_FEATURE_POINTS) {
          set = set.normalize();
          set = set.determineFeaturePoints();
          set.freeze();
        }

        // Scale to fit the TouchView
        Rect rect = new Rect(0, 0, getWidth(), getHeight());
        float inset = rect.minDim() * .2f;
        rect.inset(inset, inset);
        set = set.fitToRect(rect);
      }
      mDisplayStrokeSet = set;
      invalidate();
    }
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }

  /**
   * Transform a stroke point from its coordinate system (origin bottom left) to
   * Android's (origin top left) by flipping the y coordinate
   */
  private Point flipVertically(Point pt) {
    return new Point(pt.x, getHeight() - pt.y);
  }

  private void processGestureEvent(MotionEvent event) {
    int actionMasked = event.getActionMasked();

    unimp("deal with issue #17 here");

    if (actionMasked == MotionEvent.ACTION_DOWN) {
      mStartEventTimeMillis = event.getEventTime();
      mTouchStrokeSet = new StrokeSet();
      // Clear any old display stroke set, so we render the touch one instead
      setDisplayStrokeSet(null);
    }

    float eventTime = ((event.getEventTime() - mStartEventTimeMillis) / 1000.0f);

    int activeId = event.getPointerId(event.getActionIndex());
    MotionEvent.PointerCoords mCoord = new MotionEvent.PointerCoords();
    for (int i = 0; i < event.getPointerCount(); i++) {
      int ptrId = event.getPointerId(i);
      event.getPointerCoords(i, mCoord);
      Point pt = flipVertically(new Point(mCoord.x, mCoord.y));
      mTouchStrokeSet.addPoint(eventTime, ptrId, pt);
    }

    if (actionMasked == MotionEvent.ACTION_UP
        || actionMasked == MotionEvent.ACTION_POINTER_UP) {
      mTouchStrokeSet.stopStroke(activeId);
      if (!mTouchStrokeSet.areStrokesActive()) {
        mTouchStrokeSet.freeze();
        mListener.processStrokeSet(mTouchStrokeSet);
      }
    }
    invalidate();
  }

  private static class StrokeRenderer {

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
        if (StrokeSet.SHOW_FEATURE_POINTS) {
          if (s.get(i).isFeaturePoint()) {
            mCanvas.drawCircle(px, py, 8, mPaintOutline);
          }
        } else {
          if (detailed) {
            mCanvas.drawCircle(px, py, 8, mPaintOutline);
          }
        }
      }
    }

    private Paint mPaintFill;
    private Paint mPaintOutline;
    private Canvas mCanvas;

  }

  public interface Listener {
    void processStrokeSet(StrokeSet set);
  }

  // Stroke set from user touch event
  private StrokeSet mTouchStrokeSet;
  private long mStartEventTimeMillis;
  private boolean mReceivingGesture;
  private StrokeSet mDisplayStrokeSet;
  private StrokeRenderer mRenderer;
  private Listener mListener;
}