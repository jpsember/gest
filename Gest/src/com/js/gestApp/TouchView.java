package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import com.js.gest.GesturePanel;
import com.js.gest.StrokeSet;
import com.js.android.MyTouchListener;
import com.js.basic.Rect;

import static com.js.basic.Tools.*;

/**
 * View for inputting and rendering user touch sequences
 * 
 * @deprecated
 */
public class TouchView extends View implements GesturePanel.Listener {

  public TouchView(Context context, GesturePanel.Listener listener,
      MyTouchListener touchListener) {
    super(context);
    doNothing();
    // setBackgroundColor(Color.BLUE);
    // mListener = listener;
    // mRenderer = new StrokeRenderer();
    // if (touchListener == null)
    // throw new IllegalArgumentException();
    // touchListener.setView(this);
    // mEventFilter = new GestureEventFilter();
    // mEventFilter.setViewMode(GestureEventFilter.MODE_SHAREDVIEW);
    // mEventFilter.prependTo(touchListener);
    // mEventFilter.setListener(this);
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
  private GesturePanel.Listener mListener;
  private StrokeRenderer mRenderer;
}
