package com.js.gest;

import java.util.ArrayList;
import java.util.List;

import com.js.basic.Point;

import static com.js.basic.Tools.*;

public class StrokeNormalizer {

	// Feature point detection is probably unnecessary; I can't produce
	// any examples (on my Samsung tablet) where I think it would make a
	// difference (although that may change if we allow strokes separated in time)
	private static final boolean DETECT_FEATURE_POINTS = false;

	private static final int DEFAULT_DESIRED_STROKE_LENGTH = 32;

	/**
	 * Construct a normalizer for a particular stroke set
	 * 
	 * @param strokeSet
	 */
	public StrokeNormalizer(StrokeSet strokeSet) {
		mSet = strokeSet;
		mDesiredStrokeSize = DEFAULT_DESIRED_STROKE_LENGTH;
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
		mFragmentList = new ArrayList();
		if (DETECT_FEATURE_POINTS) {
			if (false) {
				unimp("splitStrokeAtFeaturePoints: cutting in two");
				int q = origStroke.length() / 2;
				mFragmentList.add(origStroke.constructFragment(0, 1 + q));
				mFragmentList.add(origStroke.constructFragment(q, origStroke.length()));
				return;
			}
			throw new UnsupportedOperationException();
		}
		mFragmentList.add(origStroke);
	}

	private int calculateInterpolatedPointsCount() {
		int featurePointCount = mFragmentList.size() + 1;
		int interpPointCount = mDesiredStrokeSize - featurePointCount;
		if (interpPointCount < 0)
			throw new IllegalArgumentException("Too many feature points: "
					+ featurePointCount);
		return interpPointCount;
	}

	/**
	 * Construct a normalized version of a stroke
	 * 
	 * @param origStroke
	 * @return normalized stroke
	 */
	private Stroke normalizeStroke(Stroke originalStroke) {
		splitStrokeAtFeaturePoints(originalStroke);
		int interpPointsTotal = calculateInterpolatedPointsCount();
		int interpPointsGenerated = 0;
		float strokeTimeRemaining = originalStroke.totalTime();

		Stroke normalizedStroke = new Stroke();

		for (Stroke fragment : mFragmentList) {
			int cursor = 0;
			StrokePoint strokePoint = fragment.get(cursor);

			// Add fragment's start point, if it's the first fragment
			if (normalizedStroke.isEmpty())
				normalizedStroke.addPoint(strokePoint);

			// Determine number of interpolation points to distribute to this fragment
			float fragmentProportionOfTotalTime = fragment.totalTime()
					/ strokeTimeRemaining;
			int fragInterpPointTotal = Math.round(fragmentProportionOfTotalTime
					* (interpPointsTotal - interpPointsGenerated));
			interpPointsGenerated += fragInterpPointTotal;

			strokeTimeRemaining -= fragment.totalTime();

			// Determine time interval between generated points
			// (The fragment endpoints are NOT interpolated)
			float timeStepWithinFragment = fragment.totalTime()
					/ (1 + fragInterpPointTotal);
			float currentTime = strokePoint.getTime();
			float nextInterpolationTime = currentTime + timeStepWithinFragment;

			int fragInterpPointCount = 0;
			while (fragInterpPointCount < fragInterpPointTotal) {

				// Advance to next interpolation point, or next source element,
				// whichever is first
				StrokePoint nextStrokePoint = fragment.get(cursor + 1);

				if (nextInterpolationTime < nextStrokePoint.getTime()) {
					// generate a new interpolation point
					currentTime = nextInterpolationTime;
					float timeAlongCurrentEdge = currentTime - strokePoint.getTime();
					float currentEdgeTotalTime = nextStrokePoint.getTime()
							- strokePoint.getTime();
					if (currentEdgeTotalTime <= 0
							|| timeAlongCurrentEdge > currentEdgeTotalTime)
						throw new IllegalStateException("illegal values");
					float t = timeAlongCurrentEdge / currentEdgeTotalTime;
					Point position = MyMath.interpolateBetween(strokePoint.getPoint(),
							nextStrokePoint.getPoint(), t);
					normalizedStroke.addPoint(currentTime, position);
					nextInterpolationTime += timeStepWithinFragment;
					fragInterpPointCount++;
				} else {
					// Advance to next original point
					currentTime = nextStrokePoint.getTime();
					strokePoint = nextStrokePoint;
					cursor += 1;
				}
			}
			// Add fragment's end point
			normalizedStroke.addPoint(fragment.last());
		}
		return normalizedStroke;
	}

	private StrokeSet mSet;
	private StrokeSet mNormalized;
	private int mDesiredStrokeSize;
	private ArrayList<Stroke> mFragmentList;
}
