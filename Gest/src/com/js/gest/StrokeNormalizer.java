package com.js.gest;

import java.util.ArrayList;
import java.util.List;

import static com.js.basic.Tools.*;

public class StrokeNormalizer {

	/**
	 * Construct a normalizer for a particular stroke set
	 * 
	 * @param strokeSet
	 */
	public StrokeNormalizer(StrokeSet strokeSet) {
		mSet = strokeSet;
		mDesiredStrokeSize = 30;
	}

	public void setDesiredStrokeSize(int size) {
		if (mNormalized != null)
			throw new IllegalStateException();
		mDesiredStrokeSize = size;
	}

	/**
	 * Perform normalization (if not already done)
	 * 
	 * @return normalized stroke set
	 */
	public StrokeSet getNormalizedSet() {
		if (mNormalized == null) {
			List<Stroke> normalizedList = new ArrayList();
			for (Stroke s : mSet) {
				Stroke normalized = normalizeStroke(s);
				normalizedList.add(normalized);
			}
			mNormalized = StrokeSet.buildFromStrokes(normalizedList);
		}
		return mNormalized;
	}

	private void splitStrokeAtFeaturePoints(Stroke origStroke) {
		unimp("splitStrokeAtFeaturePoints: determine which points are feature points");
		mSplitStrokeList = new ArrayList();
		mSplitStrokeList.add(origStroke);
	}

	private void calculateInterpolatedPointsCount() {
		int featurePointCount = mSplitStrokeList.size() + 1;
		int interpPointCount = mDesiredStrokeSize - featurePointCount;
		if (interpPointCount < 0)
			throw new IllegalArgumentException("Too many feature points: "
					+ featurePointCount);

		mInterpPointsRemaining = interpPointCount;
		float t = 0;
		for (Stroke s : mSplitStrokeList) {
			t += s.totalTime();
		}
		mInterpPolylineTimeRemaining = t;
	}

	/**
	 * Construct a normalized version of a stroke
	 * 
	 * @param origStroke
	 * @return normalized stroke
	 */
	private Stroke normalizeStroke(Stroke origStroke) {
		splitStrokeAtFeaturePoints(origStroke);
		calculateInterpolatedPointsCount();

		pr("# strokes in list: " + mSplitStrokeList.size()
				+ "\ninterpPointsRemaining: " + mInterpPointsRemaining
				+ "\ntimeRemaining: " + d(mInterpPolylineTimeRemaining));

		unimp("normalizeStroke returning orig stroke");
		return origStroke;
	}

	private StrokeSet mSet;
	private StrokeSet mNormalized;
	private int mDesiredStrokeSize;
	private ArrayList<Stroke> mSplitStrokeList;
	private float mInterpPolylineTimeRemaining;
	private int mInterpPointsRemaining;
}
