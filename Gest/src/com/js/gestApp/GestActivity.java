package com.js.gestApp;

import java.util.ArrayList;

import com.js.android.MyActivity;
import com.js.basic.Point;
import com.js.gest.Cell;
import com.js.gest.StrokeMatcher;
import com.js.gest.Rect;
import com.js.gest.Stroke;
import com.js.gest.StrokeNormalizer;
import com.js.gest.StrokeRegistrator;
import com.js.gest.StrokeSet;
import com.js.gest.StrokeSmoother;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import static com.js.basic.Tools.*;

public class GestActivity extends MyActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(prepareView());
	}

	private View prepareView() {
		mView = new OurView(this);
		mView.setBackgroundColor(Color.BLUE);
		return mView;
	}

	private static class OurView extends View {

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
				pr("\nTouchEvent");
				mStartEventTimeMillis = event.getEventTime();
				mStrokeSet = new StrokeSet();
				mRegisteredSet = null;
				mAlgorithmSet1 = null;
				mAlgorithmSet2 = null;
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

			boolean printFlag = (//
			actionMasked == MotionEvent.ACTION_DOWN
					|| actionMasked == MotionEvent.ACTION_UP
					|| actionMasked == MotionEvent.ACTION_POINTER_DOWN
					|| actionMasked == MotionEvent.ACTION_POINTER_UP //
			|| eventTime - mPrevPrintedTime > 0.2f);

			int activeId = event.getPointerId(event.getActionIndex());
			StringBuilder sb = new StringBuilder(" action=" + actionMasked);
			MotionEvent.PointerCoords mCoord = new MotionEvent.PointerCoords();
			for (int i = 0; i < event.getPointerCount(); i++) {
				int ptrId = event.getPointerId(i);
				event.getPointerCoords(i, mCoord);
				sb.append("     ");
				if (activeId == ptrId)
					sb.append("*");
				else
					sb.append(":");
				Point pt = new Point(mCoord.x, mCoord.y);
				mStrokeSet.addPoint(eventTime, ptrId, pt);
				sb.append("" + ptrId + d((int) pt.x, 4) + d((int) pt.y, 4));
				sb.append(" t:" + d(eventTime));
			}

			if (printFlag) {
				pr(sb);
				mPrevPrintedTime = eventTime;
			}

			if (actionMasked == MotionEvent.ACTION_UP
					|| actionMasked == MotionEvent.ACTION_POINTER_UP) {
				mStrokeSet.stopStroke(activeId);
				if (mStrokeSet.isComplete()) {
					if (mStrokeSet.isMutable()) {
						mStrokeSet.freeze();
						constructRegisteredSet();
					}
				}
			}

			if (actionMasked == MotionEvent.ACTION_UP) {
				pr(mStrokeSet);
				pr("");
				mStartEventTimeMillis = null;
			}

			// Invalidate the view so it is redrawn with the updated stroke set
			invalidate();

			if (event.getAction() == MotionEvent.ACTION_UP && mAlwaysFalse) {
				return performClick();
			}
			return true;
		}

		private void constructRegisteredSet() {

			StrokeSet set = mStrokeSet;
			if (set.iterator().next().length() < 3) {
				mMatchStroke = null;
				mStrokeSet = null;
				mRegisteredSet = null;
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

			// Set algorithm set #1 and #2 to smoothed and normalized sets
			// respectively, scaled up to original's
			// bounding rect, then translated a bit so it's just above the
			// original
			Rect originalBounds = StrokeRegistrator.bounds(mStrokeSet);
			float displacement = this.getWidth()
					/ (withSmoothing && withNormalizing ? 4 : 3);

			if (withSmoothing) {
				originalBounds.translate(displacement, 0);
				mAlgorithmSet1 = StrokeRegistrator.fitToRect(smoothedSet,
						originalBounds);
			}
			if (withNormalizing) {
				originalBounds.translate(displacement, 0);
				mAlgorithmSet2 = StrokeRegistrator.fitToRect(normalizedSet,
						originalBounds);
			}

			performMatch();
		}

		@Override
		public boolean performClick() {
			return super.performClick();
		}

		@Override
		public void onDraw(Canvas canvas) {
			mCanvas = canvas;

			if (mStrokeSet != null) {
				StrokeSet set = mStrokeSet;
				drawStrokeSet(set, false, 1.0f);
				if (mAlgorithmSet1 != null)
					drawStrokeSet(mAlgorithmSet1, false, 0.3f);
				if (mAlgorithmSet2 != null)
					drawStrokeSet(mAlgorithmSet2, false, 0.3f);

				mCanvas.translate(20, 20);

				if (mMatchStroke != null) {
					mPaintFill.setColor(Color.LTGRAY);
					drawStroke(mMatchStroke, true, 0);
					mPaintFill.setColor(Color.WHITE);
				}

				if (mRegisteredSet != null) {
					set = mRegisteredSet;
					drawStrokeSet(set, true, 0);
					Rect r = StrokeRegistrator.sStandardRect;
					for (int i = 0; i < 4; i++)
						drawLine(r.corner(i), r.corner((i + 1) % 4), mPaintOutline);
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

		private void performMatch() {
			Stroke s = mRegisteredSet.iterator().next();
			if (mMatchStroke == null) {
				mMatchStroke = s;
				return;
			}

			StrokeMatcher m = new StrokeMatcher(mMatchStroke, s);
			m.similarity();

			ArrayList<Cell> path = m.optimalPath();
			float prevCost = 0;
			for (Cell c : path) {
				float diff = c.cost() - prevCost;
				pr(" " + c + " " + d(diff));
				prevCost = c.cost();
			}
			pr("Match similarity: " + d(m.similarity()));

			float ff[] = { 0, .01f, .02f, .05f, .06f, .07f, .08f, .1f };
			for (float factor : ff) {
				m = new StrokeMatcher(mMatchStroke, s);
				m.setDistanceThreshold(factor);
				pr("Factor " + d(factor) + " similiarity: " + d(m.similarity()));
			}
		}

		private Paint mPaintFill;
		private Paint mPaintOutline;
		private Canvas mCanvas;
		private boolean mAlwaysFalse;
		private float mPrevPrintedTime;
		private StrokeSet mStrokeSet;
		private StrokeSet mRegisteredSet;
		private StrokeSet mAlgorithmSet1;
		private StrokeSet mAlgorithmSet2;
		private int mSkipCount;
		private Long mStartEventTimeMillis;
		private Stroke mMatchStroke;
	}

	private OurView mView;

}
