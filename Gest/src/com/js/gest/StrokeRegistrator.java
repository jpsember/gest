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

	public static final Rect sStandardRect = new Rect(0, 0, STANDARD_WIDTH,
			STANDARD_WIDTH * STANDARD_ASPECT_RATIO);

	/**
	 * Construct version of StrokeSet that has been fit within a rectangle,
	 * preserving the aspect ratio
	 * 
	 * @param set
	 *          StrokeSet to fit
	 * @param destinationRect
	 *          rectangle to fit within, or null to use standard rectangle
	 * @return StrokeSet fitted to destinationRect
	 */
	public static StrokeSet fitToRect(StrokeSet set, Rect destinationRect) {
		Rect origBounds = bounds(set);
		if (destinationRect == null)
			destinationRect = sStandardRect;
		Matrix transform = MyMath.calcRectFitRectTransform(origBounds,
				destinationRect);
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
