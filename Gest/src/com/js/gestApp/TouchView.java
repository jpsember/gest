package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;

import com.js.android.UITools;
import com.js.gest.GestureEventFilter;
import com.js.gest.StrokeSet;
import com.js.gest.GestureSet;
import com.js.android.MyTouchListener;
import com.js.basic.Rect;

import static com.js.basic.Tools.*;

/**
 * View for inputting and rendering user touch sequences
 */
public class TouchView extends UITools.OurBaseView implements
    GestureEventFilter.Listener {

  public TouchView(Context context, GestureEventFilter.Listener listener) {
    super(context);
    setBackgroundColor(Color.BLUE);
    mListener = listener;
    mRenderer = new StrokeRenderer();

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
          pr("TouchView ignoring non-gesture event " + UITools.dump(event));
        return false;
      }
    };
    touchListener.setView(this);
    return touchListener;
  }

  public void setGestureSet(GestureSet gestures) {
    mEventFilter.setGestures(gestures);
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
      mDisplayStrokeSet = set;
      invalidate();
    }
  }

  @Override
  public void strokeSetExtended(StrokeSet strokeSet) {
    mTouchStrokeSet = strokeSet;
    // If this stroke set has just started, clear any old displayed version
    if (strokeSet.length() == 1) {
      mDisplayStrokeSet = null;
    }
    mListener.strokeSetExtended(strokeSet);
    invalidate();
  }

  @Override
  public void processGesture(String gestureName) {
    mListener.processGesture(gestureName);
  }

  // Stroke set from user touch event
  private StrokeSet mTouchStrokeSet;
  private StrokeSet mDisplayStrokeSet;
  private GestureEventFilter.Listener mListener;
  private StrokeRenderer mRenderer;
  private GestureEventFilter mEventFilter;
}
