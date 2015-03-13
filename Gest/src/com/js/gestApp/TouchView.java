package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import com.js.android.UITools;
import com.js.gest.GestureEventFilter;
import com.js.gest.StrokeSet;
import static com.js.basic.Tools.*;

/**
 * View for inputting and rendering user touch sequences
 */
public class TouchView extends UITools.OurBaseView implements
		GestureEventFilter.Listener {

	public TouchView(Context context, GestureEventFilter.Listener listener) {
		super(context);
		mListener = listener;
		mRenderer = new StrokeRenderer();

		mEventFilter = new GestureEventFilter();
		mEventFilter.attachToView(this, this);
	}

	// We don't need to include onTouchEvent() and performClick(), but we do so to
	// verify that we still get non-gesture-related motions
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionMasked() != MotionEvent.ACTION_MOVE)
			pr("TouchView ignoring non-gesture event " + UITools.dump(event));
		if (mAlwaysFalse)
			return performClick();
		return true;
	}

	@Override
	public boolean performClick() {
		return super.performClick();
	}

	@Override
	public void onDraw(Canvas canvas) {
		mRenderer.startRender(canvas);
		if (mTouchStrokeSet != null) {
			StrokeSet set = mTouchStrokeSet;
			if (mDisplayStrokeSet != null)
				set = mDisplayStrokeSet;
			mRenderer.drawStrokeSet(set, false, 1.0f);
		}
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
		if (strokeSet.length() == 1) {
			mDisplayStrokeSet = null;
		}
		invalidate();
	}

	@Override
	public void strokeSetCompleted(StrokeSet strokeSet) {
		mListener.strokeSetCompleted(strokeSet);
	}

	// Stroke set from user touch event
	private StrokeSet mTouchStrokeSet;
	private StrokeSet mDisplayStrokeSet;
	private GestureEventFilter.Listener mListener;
	private StrokeRenderer mRenderer;
	private boolean mAlwaysFalse;
	private GestureEventFilter mEventFilter;
}
