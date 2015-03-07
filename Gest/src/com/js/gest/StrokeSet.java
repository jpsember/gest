package com.js.gest;

import java.util.HashMap;
import java.util.Map;

import com.js.basic.Point;

/**
 * A collection of Strokes, which ultimately will be recognized as a touch
 * gesture
 */
public class StrokeSet {

	public void addPoint(float eventTime, int pointerId, Point pt) {
		if (mStrokeMap.isEmpty())
			mInitialEventTime = eventTime;
		Stroke s = strokeForId(pointerId);
		s.addPoint(eventTime - mInitialEventTime, pt);
	}

	private Stroke strokeForId(int pointerId) {
		Stroke s = mStrokeMap.get(pointerId);
		if (s == null) {
			s = new Stroke();
			mStrokeMap.put(pointerId, s);
		}
		return s;
	}

	private float mInitialEventTime;
	private Map<Integer, Stroke> mStrokeMap = new HashMap();
}
