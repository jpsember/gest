package com.js.gest;

import static com.js.basic.Tools.*;

public class StrokeSetMatcher {

	public StrokeSetMatcher(StrokeSet a, StrokeSet b) {
		mStrokeA = frozen(a);
		mStrokeB = frozen(b);
		if (mStrokeA.size() != mStrokeB.size())
			throw new IllegalArgumentException(
					"Different number of strokes in each set");
		setDistanceThreshold(.01f);
	}

	public void setDistanceThreshold(float f) {
		if (matched())
			throw new IllegalStateException();
		mDistanceThreshold = f;
	}

	public float similarity() {
		if (mSimilarity == null) {
			float totalCost = 0;
			for (int i = 0; i < mStrokeA.size(); i++) {
				Stroke sa = mStrokeA.get(i);
				Stroke sb = mStrokeB.get(i);
				StrokeMatcher m = new StrokeMatcher(sa, sb);
				m.setDistanceThreshold(mDistanceThreshold);
				float cost = m.similarity();
				totalCost += cost;
			}
			mSimilarity = totalCost;
		}
		return mSimilarity;
	}

	private boolean matched() {
		return mSimilarity != null;
	}

	private float mDistanceThreshold;
	private StrokeSet mStrokeA;
	private StrokeSet mStrokeB;
	private Float mSimilarity;

}
