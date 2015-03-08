package com.js.gest;

import static com.js.basic.Tools.*;

/**
 * Determines how closely two strokes match
 */
public class StrokeMatcher {

	public StrokeMatcher(Stroke a, Stroke b) {
		mStrokeA = frozen(a);
		mStrokeB = frozen(b);
	}

	public float similarity() {
		if (mSimilarity == null)
			calculateSimilarity();
		return mSimilarity;
	}

	private void calculateSimilarity() {
		if (mStrokeA.length() != mStrokeB.length())
			throw new IllegalArgumentException("stroke lengths mismatch");
		unimp("calculateSimilarity");
		mSimilarity = 0.0f;
	}

	private Stroke mStrokeA;
	private Stroke mStrokeB;
	private Float mSimilarity;
}
