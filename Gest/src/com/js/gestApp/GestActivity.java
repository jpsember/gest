package com.js.gestApp;

import com.js.android.MyActivity;
import com.js.basic.Point;
import com.js.gest.StrokeSet;

import android.content.Context;
import android.graphics.Color;
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
			
			if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_POINTER_UP)
				mStrokeSet.stopStroke(activeId);
			
			if (printFlag) {
				pr(sb);
				mPrevPrintedTime = event.getEventTime();
			}
			if (actionMasked == MotionEvent.ACTION_UP)
				pr("");

			if (event.getAction() == MotionEvent.ACTION_UP && mAlwaysFalse) {
				return performClick();
			}
			return true;
		}

		@Override
		public boolean performClick() {
			return super.performClick();
		}

		private boolean mAlwaysFalse;
		private long mPrevPrintedTime;
		private StrokeSet mStrokeSet;
	}

	private OurView mView;

}
