package com.js.gest;

import android.graphics.Matrix;

import com.js.basic.Point;
import static com.js.basic.Tools.*;

public class StrokeRegistrator {

	public static Rect bounds(StrokeSet set) {
		Rect r = null;
		for (Stroke s : set) {
			for (StrokePoint spt : s) {
				Point pt = spt.getPoint();
				if (r == null)
					r = new Rect(pt, pt);
				r.include(pt);
			}
		}
		return r;
	}

	private static final float STANDARD_WIDTH = 255.0f;
	private static final float STANDARD_ASPECT_RATIO = 1.0f;
	private static final Rect sStandardRect = new Rect(0, 0, STANDARD_WIDTH,
			STANDARD_WIDTH * STANDARD_ASPECT_RATIO);

	public static Matrix transformToFitStandard(Rect rect) {
		return MyMath.calcRectFitRectTransform(rect, sStandardRect);
	}

	public static StrokeSet fitToStandardRect(StrokeSet set) {
		Rect origBounds = bounds(set);
		Matrix transform = transformToFitStandard(origBounds);
		StrokeSet standardizedSet = mutableCopyOf(set);
		for (Stroke stroke : standardizedSet) {
			for (StrokePoint strokePoint : stroke) {
				strokePoint.getPoint().apply(transform);
			}
		}
		standardizedSet.freeze();
		return standardizedSet;
	}

}
