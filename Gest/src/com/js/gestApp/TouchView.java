package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;

import com.js.gest.StrokeSet;
import com.js.basic.Point;
import com.js.basic.Rect;

import static com.js.basic.Tools.*;

/**
 * View for inputting and rendering user touch sequences (in a more expressive
 * manner than the GesturePanel)
 */
public class TouchView extends View {

  public static interface Listener {
    /**
     * Called when user has entered a complete StrokeSet
     */
    void receivedStrokeSet(StrokeSet set);
  }

  public TouchView(Context context, Listener listener) {
    super(context);
    doNothing();
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
    mListener = listener;
  }

  private void onDrawAux() {
    if (mTouchStrokeSet != null) {
      StrokeSet set = mTouchStrokeSet;
      Rect r = new Rect(0, 0, getWidth(), getHeight());
      if (mDisplayStrokeSet != null)
        set = mDisplayStrokeSet;
      mRenderer.drawStrokeSet(set, r, true);
    }
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

    if (actionMasked != MotionEvent.ACTION_DOWN
        && actionMasked != MotionEvent.ACTION_POINTER_DOWN
        && actionMasked != MotionEvent.ACTION_MOVE
        && actionMasked != MotionEvent.ACTION_POINTER_UP
        && actionMasked != MotionEvent.ACTION_UP)
      return;

    if (actionMasked == MotionEvent.ACTION_DOWN) {
      mStartEventTimeMillis = event.getEventTime();
      mTouchStrokeSet = new StrokeSet();
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
        mListener.receivedStrokeSet(mTouchStrokeSet);
      }
    }
    invalidate();
  }

  // Stroke set from user touch event
  private StrokeSet mTouchStrokeSet;
  private long mStartEventTimeMillis;
  private boolean mReceivingGesture;
  private Listener mListener;
  private StrokeSet mDisplayStrokeSet;
  private StrokeRenderer mRenderer;
}