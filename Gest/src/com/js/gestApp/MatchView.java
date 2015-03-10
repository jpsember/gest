package com.js.gestApp;

import static com.js.basic.Tools.*;

import android.content.Context;
import android.graphics.Canvas;

import com.js.android.UITools;
import com.js.gest.StrokeRegistrator;
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
			mRenderer.drawRect(StrokeRegistrator.sStandardRect);

			{
				unimp("display score in text view");
				// int TEXT_SIZE = 35;
				// mPaintFill.setTextSize(TEXT_SIZE);
				//
				// canvas.drawText("Match: " + Math.round(mDisplayedSimilarity), r.x,
				// r.endY() + TEXT_SIZE * 1.2f, mPaintFill);
			}
		}
		mRenderer.stopRender();
	}

	public void setStrokeSet(StrokeSet set, float score) {
		if (set != mRegisteredSet) {
			mRegisteredSet = set;
			mDisplayedSimilarity = score;
			invalidate();
		}
	}

	private StrokeRenderer mRenderer;
	private StrokeSet mRegisteredSet;
	/* private */float mDisplayedSimilarity;
}
