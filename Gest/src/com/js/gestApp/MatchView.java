package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;

import com.js.android.UITools;
import com.js.basic.Rect;
import com.js.gest.StrokeSet;

public class MatchView extends UITools.OurBaseView {

  public MatchView(Context context) {
    super(context);
    setBackgroundColor(0xFF008000);
    mRenderer = new StrokeRenderer();
  }

  private void onDrawAux() {
    Rect r = new Rect(0, 0, getWidth(), getHeight());
    r.inset(10, 10);
    StrokeSet s = mRegisteredSet.fitToRect(r);
    mRenderer.drawStrokeSet(s, r, false);
  }

  @Override
  public void onDraw(Canvas canvas) {
    if (mRegisteredSet == null)
      return;

    mRenderer.startRender(canvas);
    onDrawAux();

    // sRect.setTo(0, 0, this.getWidth(), this.getHeight());
    // // Inset and center
    // sRect.inset(10, 10);
    // if (mRegisteredSet != null) {
    // mRenderer.drawStrokeSet(mRegisteredSet, true, 0);
    // mRenderer.drawRect(StrokeSet.sStandardRect);
    // }

    mRenderer.stopRender();
  }

  public void setStrokeSet(StrokeSet set) {
    if (set != mRegisteredSet) {
      mRegisteredSet = set;
      invalidate();
    }
  }

  private StrokeRenderer mRenderer;
  private StrokeSet mRegisteredSet;
}
