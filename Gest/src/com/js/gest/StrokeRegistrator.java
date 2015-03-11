package com.js.gest;

import android.graphics.Matrix;

import static com.js.basic.Tools.*;

public class StrokeRegistrator {

	public static final int STANDARD_WIDTH = 256;
	private static final float STANDARD_ASPECT_RATIO = 1.0f;

	public static final Rect sStandardRect = new Rect(0, 0, STANDARD_WIDTH - 1,
			(STANDARD_WIDTH - 1) * STANDARD_ASPECT_RATIO);

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
		Rect origBounds = set.getBounds();
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
