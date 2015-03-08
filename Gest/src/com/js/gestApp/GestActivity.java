package com.js.gestApp;

import com.js.android.MyActivity;
import com.js.basic.Point;
import com.js.gest.Rect;
import com.js.gest.Stroke;
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
				mStrokeSet = new StrokeSet();
				mRegisteredSet = null;
				mSmoothedSet = null;
				mSimulateCoarse ^= true;
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

			boolean printFlag = (actionMasked == MotionEvent.ACTION_DOWN
					|| actionMasked == MotionEvent.ACTION_UP
					|| actionMasked == MotionEvent.ACTION_POINTER_DOWN
					|| actionMasked == MotionEvent.ACTION_POINTER_UP || event
					.getEventTime() - mPrevPrintedTime > 200);

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
				mStrokeSet.addPoint(event.getEventTime() / 1000.0f, ptrId, pt);
				sb.append("" + ptrId + d((int) pt.x, 4) + d((int) pt.y, 4));
			}

			if (printFlag) {
				pr(sb);
				mPrevPrintedTime = event.getEventTime();
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
			Rect fitRect = StrokeRegistrator.sStandardRect;
			set = StrokeRegistrator.fitToRect(set, fitRect);

			StrokeSmoother s = new StrokeSmoother(set);
			set = s.getSmoothedSet();
			mRegisteredSet = set;

			// Set smoothed set to registered version, scaled up to original's
			// bounding rect, then translated a bit so it's just above the
			// original
			Rect originalBounds = StrokeRegistrator.bounds(mStrokeSet);
			originalBounds.translate(0, -60);

			mSmoothedSet = StrokeRegistrator
					.fitToRect(mRegisteredSet, originalBounds);
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
				if (mSmoothedSet != null)
					drawStrokeSet(mSmoothedSet, false, 0.3f);

				if (mRegisteredSet != null) {
					set = mRegisteredSet;
					mCanvas.translate(20, 20);
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
			float scaleFactor = small ? 0.3f : 1.0f;

			for (Stroke s : mStrokeSet) {
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
		}

		private void drawLine(Point p1, Point p2, Paint paint) {
			mCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
		}

		private Paint mPaintFill;
		private Paint mPaintOutline;
		private Canvas mCanvas;
		private boolean mAlwaysFalse;
		private long mPrevPrintedTime;
		private StrokeSet mStrokeSet;
		private StrokeSet mRegisteredSet;
		private StrokeSet mSmoothedSet;
		private int mSkipCount;
	}

	private OurView mView;

}
