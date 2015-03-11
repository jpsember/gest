package com.js.gestApp;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import com.js.android.UITools;
import com.js.basic.Point;
import com.js.gest.StrokeSet;

/**
 * View for inputting and rendering user touch sequences
 */
public class TouchView extends UITools.OurBaseView {

	public interface Listener {
		void startTouchSequence();

		void processTouchSet(StrokeSet mTouchStrokeSet);
	}

	/**
	 * Set 'coarse mode' status. Off by default, if enabled, it generates fewer
	 * points per second (for test purposes) by ignoring certain TouchEvents
	 */
	public void setCoarseFlag(boolean f) {
		mCoarseMode = f;
	}

	public TouchView(Context context, Listener listener) {
		super(context);
		mListener = listener;
		mRenderer = new StrokeRenderer();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int actionMasked = event.getActionMasked();
		if (actionMasked == MotionEvent.ACTION_DOWN) {
			mStartEventTimeMillis = event.getEventTime();
			mListener.startTouchSequence();
			mTouchStrokeSet = new StrokeSet();
			mDisplayStrokeSet = null;
		}

		if (mCoarseMode) {
			if (actionMasked == MotionEvent.ACTION_MOVE) {
				mSkipCount++;
				if (mSkipCount == 4) {
					mSkipCount = 0;
				} else
					return false;
			} else {
				mSkipCount = 0;
			}
		}

		float eventTime = ((event.getEventTime() - mStartEventTimeMillis) / 1000.0f);

		int activeId = event.getPointerId(event.getActionIndex());
		MotionEvent.PointerCoords mCoord = new MotionEvent.PointerCoords();
		for (int i = 0; i < event.getPointerCount(); i++) {
			int ptrId = event.getPointerId(i);
			event.getPointerCoords(i, mCoord);
			Point pt = new Point(mCoord.x, mCoord.y);
			pt.y = getHeight() - mCoord.y;
			mTouchStrokeSet.addPoint(eventTime, ptrId, pt);
		}

		if (actionMasked == MotionEvent.ACTION_UP
				|| actionMasked == MotionEvent.ACTION_POINTER_UP) {
			mTouchStrokeSet.stopStroke(activeId);
			if (!mTouchStrokeSet.areStrokesActive()) {
				mTouchStrokeSet.freeze();
				mListener.processTouchSet(mTouchStrokeSet);
			}
		}

		if (actionMasked == MotionEvent.ACTION_UP) {
			mStartEventTimeMillis = null;
		}

		// Invalidate the view so it is redrawn with the updated stroke set
		invalidate();

		if (event.getAction() == MotionEvent.ACTION_UP && mAlwaysFalse) {
			return performClick();
		}
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

	// Stroke set from user touch event
	private StrokeSet mTouchStrokeSet;
	private StrokeSet mDisplayStrokeSet;
	private Listener mListener;
	private StrokeRenderer mRenderer;
	private boolean mAlwaysFalse;
	private int mSkipCount;
	private Long mStartEventTimeMillis;
	private boolean mCoarseMode;
}
