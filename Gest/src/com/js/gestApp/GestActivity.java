package com.js.gestApp;

import com.js.android.MyActivity;
import com.js.basic.Point;
import com.js.gest.Rect;
import com.js.gest.Stroke;
import com.js.gest.StrokeRegistrator;
import com.js.gest.StrokeSet;

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

		public OurView(Context context) {
			super(context);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {

			int actionMasked = event.getActionMasked();
			if (actionMasked == MotionEvent.ACTION_DOWN) {
				pr("\nTouchEvent");
				mStrokeSet = new StrokeSet();
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
					|| actionMasked == MotionEvent.ACTION_POINTER_UP)
				mStrokeSet.stopStroke(activeId);

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

		@Override
		public boolean performClick() {
			return super.performClick();
		}

		@Override
		public void onDraw(Canvas canvas) {
			mCanvas = canvas;
			if (mStrokeSet != null) {
				drawStrokeSet(mStrokeSet, 1.0f);
				if (mStrokeSet.isComplete()) {
					StrokeSet s2 = StrokeRegistrator.fitToStandardRect(mStrokeSet);
					drawStrokeSet(s2, 0.5f);
				}
			}
			mCanvas = null;
		}

		private void drawStrokeSet(StrokeSet mStrokeSet, float scaleFactor) {
			Paint p = mPaint;
			p.setColor(Color.WHITE);
			p.setStrokeWidth(8 * scaleFactor);

			for (Stroke s : mStrokeSet) {
				Point prevPoint = null;
				for (int i = 0; i < s.length(); i++) {
					Point point = s.get(i).getPoint();
					if (prevPoint != null) {
						drawLine(prevPoint, point);
					}
					prevPoint = point;
				}
			}

			p.setStrokeWidth(3 * scaleFactor);
			Rect r = StrokeRegistrator.bounds(mStrokeSet);
			for (int i = 0; i < 4; i++)
				drawLine(r.corner(i), r.corner((i + 1) % 4));

		}

		private void drawLine(Point p1, Point p2) {
			mCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, mPaint);
		}

		private Paint mPaint = new Paint();
		private Canvas mCanvas;
		private boolean mAlwaysFalse;
		private long mPrevPrintedTime;
		private StrokeSet mStrokeSet;
	}

	private OurView mView;

}
