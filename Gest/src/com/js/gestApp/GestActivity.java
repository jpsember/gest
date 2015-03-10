package com.js.gestApp;

import com.js.android.MyActivity;
import com.js.android.UITools;
import com.js.basic.Point;
import com.js.gest.Rect;
import com.js.gest.Stroke;
import com.js.gest.StrokeNormalizer;
import com.js.gest.StrokeRegistrator;
import com.js.gest.StrokeSet;
import com.js.gest.StrokeSetMatcher;
import com.js.gest.StrokeSmoother;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import static com.js.basic.Tools.*;

public class GestActivity extends MyActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(buildContentView());
	}

	private class OurView extends UITools.OurBaseView {

		// If enabled, generates fewer points for test purposes (useful for
		// debugging the smoothing algorithm)
		private boolean mSimulateCoarse = false;

		public OurView(Context context) {
			super(context);

			Paint p = new Paint();
			mPaintOutline = p;
			p.setColor(Color.WHITE);
			p.setStrokeWidth(1.2f);
			p.setStyle(Paint.Style.STROKE);

			p = new Paint();
			mPaintFill = p;
			p.setColor(Color.WHITE);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {

			int actionMasked = event.getActionMasked();
			if (actionMasked == MotionEvent.ACTION_DOWN) {
				mStartEventTimeMillis = event.getEventTime();
				mTouchStrokeSet = new StrokeSet();
				mRegisteredSet = null;
				mDisplayStrokeSet = null;
				mSimulateCoarse ^= true;
				if (false) {
					warning("always coarse");
					mSimulateCoarse = true;
				}
				if (false) {
					warning("always fine");
					mSimulateCoarse = false;
				}
			}

			if (mSimulateCoarse) {
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
				mTouchStrokeSet.addPoint(eventTime, ptrId, pt);
			}

			if (actionMasked == MotionEvent.ACTION_UP
					|| actionMasked == MotionEvent.ACTION_POINTER_UP) {
				mTouchStrokeSet.stopStroke(activeId);
				if (mTouchStrokeSet.isComplete()) {
					if (mTouchStrokeSet.isMutable()) {
						mTouchStrokeSet.freeze();
						processTouchSet();
					}
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
			mCanvas = canvas;

			if (mTouchStrokeSet != null) {

				StrokeSet set = mTouchStrokeSet;

				if (mDisplayStrokeSet != null)
					set = mDisplayStrokeSet;
				drawStrokeSet(set, false, 1.0f);

				if (mRegisteredSet != null) {
					set = mRegisteredSet;
					drawStrokeSet(set, true, 0);
					Rect r = StrokeRegistrator.sStandardRect;
					for (int i = 0; i < 4; i++)
						drawLine(r.corner(i), r.corner((i + 1) % 4), mPaintOutline);
					if (mDisplayedSimilarity != null) {
						int TEXT_SIZE = 35;
						mPaintFill.setTextSize(TEXT_SIZE);

						canvas.drawText("Match: " + mDisplayedSimilarity.intValue(), r.x,
								r.endY() + TEXT_SIZE * 1.2f, mPaintFill);
					}
				}
			}
			mCanvas = null;
		}

		private void drawStrokeSet(StrokeSet mStrokeSet, boolean small,
				float circleScale) {
			for (Stroke s : mStrokeSet) {
				drawStroke(s, small, circleScale);
			}
		}

		private void drawStroke(Stroke s, boolean small, float circleScale) {
			float scaleFactor = small ? 0.3f : 1.0f;

			Point prevPoint = null;
			for (int i = 0; i < s.length(); i++) {
				Point point = s.get(i).getPoint();
				if (prevPoint != null) {
					drawLine(prevPoint, point, mPaintFill);
				}
				if (!small)
					mCanvas.drawCircle(point.x, point.y, 8 * scaleFactor * circleScale,
							mPaintOutline);
				prevPoint = point;
			}

		}

		private void drawLine(Point p1, Point p2, Paint paint) {
			mCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
		}

		private Paint mPaintFill;
		private Paint mPaintOutline;
		private Canvas mCanvas;
		private boolean mAlwaysFalse;
		private int mSkipCount;
		private Long mStartEventTimeMillis;
	}

	private View buildUpperViews() {
		LinearLayout horzLayout = UITools.linearLayout(this, false);

		View mAuxView = new UITools.OurBaseView(this);
		mAuxView.setBackgroundColor(Color.GREEN);

		LinearLayout.LayoutParams p = UITools.layoutParams(horzLayout);
		p.weight = .3f;
		horzLayout.addView(mAuxView, p);

		mView = new OurView(this);
		mView.setBackgroundColor(Color.BLUE);
		p = UITools.layoutParams(horzLayout);
		p.weight = .7f;
		horzLayout.addView(mView, p);
		return horzLayout;
	}

	private LinearLayout buildControlView() {
		LinearLayout ctrlView = UITools.linearLayout(this, false);
		ctrlView.setBackgroundColor(Color.RED);
		return ctrlView;
	}

	private View buildContentView() {
		LinearLayout contentView = UITools.linearLayout(this, true);

		LinearLayout.LayoutParams p = UITools.layoutParams(contentView);
		p.weight = 1.0f;
		contentView.addView(buildUpperViews(), p);

		LinearLayout ctrlView = buildControlView();
		p = UITools.layoutParams(contentView);
		if (false) {
			Button b = new Button(this);
			b.setText("Hey");
			ctrlView.addView(b, UITools.layoutParams(ctrlView));
		}

		contentView.addView(ctrlView, p);
		return contentView;
	}

	private void clearRegisteredSet() {
		mMatchStrokeSet = null;
		mRegisteredSet = null;
		mDisplayedSimilarity = null;
	}

	private void processTouchSet() {

		StrokeSet set = mTouchStrokeSet;
		if (set.iterator().next().length() < 3) {
			clearRegisteredSet();
			mTouchStrokeSet = null;
			return;
		}

		boolean withSmoothing = true;
		boolean withNormalizing = true;

		StrokeSet smoothedSet = set;
		if (withSmoothing) {
			StrokeSmoother s = new StrokeSmoother(set);
			set = s.getSmoothedSet();
			smoothedSet = set;
		}
		Rect fitRect = StrokeRegistrator.sStandardRect;
		smoothedSet = StrokeRegistrator.fitToRect(smoothedSet, fitRect);

		StrokeSet normalizedSet = smoothedSet;
		if (withNormalizing) {
			StrokeNormalizer n = new StrokeNormalizer(normalizedSet);
			normalizedSet = n.getNormalizedSet();
		}
		mRegisteredSet = normalizedSet;
		Rect originalBounds = StrokeRegistrator.bounds(mTouchStrokeSet);
		mDisplayStrokeSet = StrokeRegistrator.fitToRect(mRegisteredSet,
				originalBounds);

		performMatch();
	}

	private void performMatch() {
		if (mMatchStrokeSet == null) {
			mMatchStrokeSet = mRegisteredSet;
			return;
		}
		if (mMatchStrokeSet.size() != mRegisteredSet.size()) {
			clearRegisteredSet();
			return;
		}

		StrokeSetMatcher m = new StrokeSetMatcher(mMatchStrokeSet, mRegisteredSet);
		m.similarity();

		mDisplayedSimilarity = m.similarity();

		if (false) {
			float ff[] = { 0, .01f, .02f, .05f, .06f, .07f, .08f, .1f };
			for (float factor : ff) {
				m = new StrokeSetMatcher(mMatchStrokeSet, mRegisteredSet);
				m.setDistanceThreshold(factor);
				pr("Factor " + d(factor) + " similiarity: " + d(m.similarity()));
			}
		}
	}

	private OurView mView;
	// Stroke set from user touch event
	private StrokeSet mTouchStrokeSet;
	// If not null, set to display in touch view instead of mTouchStrokeSet
	private StrokeSet mDisplayStrokeSet;
	// Normalized stroke set (suitable for comparison)
	private StrokeSet mRegisteredSet;
	private Float mDisplayedSimilarity;
	private StrokeSet mMatchStrokeSet;

}
