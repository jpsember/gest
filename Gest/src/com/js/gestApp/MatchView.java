package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;

import com.js.android.UITools;
import com.js.gest.StrokeSet;

public class MatchView extends UITools.OurBaseView {
	public MatchView(Context context) {
		super(context);
		mRenderer = new StrokeRenderer();
	}

	@Override
	public void onDraw(Canvas canvas) {
		mRenderer.startRender(canvas);

		if (mRegisteredSet != null) {
			mRenderer.drawStrokeSet(mRegisteredSet, true, 0);
			mRenderer.drawRect(StrokeSet.sStandardRect);
		}
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
