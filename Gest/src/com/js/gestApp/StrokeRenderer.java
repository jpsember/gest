package com.js.gestApp;

import com.js.basic.Point;
import com.js.gest.Rect;
import com.js.gest.Stroke;
import com.js.gest.StrokeSet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Encapsulates rendering tasks for strokes
 */
public class StrokeRenderer {

	public StrokeRenderer() {
		Paint p = new Paint();
		mPaintOutline = p;
		p.setColor(Color.WHITE);
		p.setStrokeWidth(1.2f);
		p.setStyle(Paint.Style.STROKE);

		p = new Paint();
		mPaintFill = p;
		p.setColor(Color.WHITE);
	}

	public void startRender(Canvas c) {
		mCanvas = c;
	}

	public void stopRender() {
		mCanvas = null;
	}

	public void drawStrokeSet(StrokeSet mStrokeSet, boolean small,
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

	public void drawRect(Rect r) {
		for (int i = 0; i < 4; i++)
			drawLine(r.corner(i), r.corner((i + 1) % 4), mPaintOutline);
	}

	private Paint mPaintFill;
	private Paint mPaintOutline;
	private Canvas mCanvas;

}
