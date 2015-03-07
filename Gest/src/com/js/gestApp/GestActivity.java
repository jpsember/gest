package com.js.gestApp;

import com.js.android.MyActivity;
import com.js.basic.IPoint;

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

	private OurView mView;

	private static class OurView extends View {

		public OurView(Context context) {
			super(context);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {

			int actionMasked = event.getActionMasked();
			if (actionMasked == MotionEvent.ACTION_DOWN) {
				pr("\nTouchEvent");
			}

			int activeId = -1;
			StringBuilder sb = null;
			if (actionMasked == MotionEvent.ACTION_DOWN
					|| actionMasked == MotionEvent.ACTION_UP
					|| actionMasked == MotionEvent.ACTION_POINTER_DOWN
					|| actionMasked == MotionEvent.ACTION_POINTER_UP
					|| (System.currentTimeMillis() - mPrevTime > 200)) {
				activeId = event.getPointerId(event.getActionIndex());
				sb = new StringBuilder(" action=" + actionMasked);
				mPrevTime = System.currentTimeMillis();
			}

			if (sb != null) {
				MotionEvent.PointerCoords mCoord = new MotionEvent.PointerCoords();
				for (int i = 0; i < event.getPointerCount(); i++) {
					int ptrId = event.getPointerId(i);
					event.getPointerCoords(i, mCoord);
					sb.append("     ");
					if (activeId == ptrId)
						sb.append("*");
					else
						sb.append(":");
					IPoint pt = new IPoint(mCoord.x, mCoord.y);
					sb.append("" + ptrId + d(pt.x, 4) + d(pt.y, 4));
				}
			}
			if (sb != null)
				pr(sb);
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
		private long mPrevTime;

	}

}
