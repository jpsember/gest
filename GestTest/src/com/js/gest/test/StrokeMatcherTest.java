package com.js.gest.test;

import org.json.JSONArray;
import org.json.JSONException;

import com.js.basic.Point;
import com.js.gest.MatcherParameters;
import com.js.gest.Stroke;
import com.js.gest.StrokeMatcher;
import com.js.gest.StrokeSet;
import com.js.testUtils.*;

public class StrokeMatcherTest extends MyTestCase {

	private Stroke buildStroke(String script) throws JSONException {
		JSONArray a = new JSONArray(script);
		return Stroke.parseJSONArray(a);
	}

	public void testEmptyStroke() {
		Stroke s = new Stroke();
		assertTrue(s.isEmpty());
	}

	public void testNonEmptyStroke() {
		Stroke s = new Stroke();
		s.addPoint(5, new Point(20, 30));
		assertFalse(s.isEmpty());
	}

	private static final String str1 = "[0, 0,0, 1, 100,0]";
	private static final String str2 = "[0, 0,128, 1,100,128]";

	public void testPerfectMatch() throws JSONException {
		match(str1, str1, 0, null);
	}

	private void match(String str1, String str2, float expectedSimilarity,
			MatcherParameters p) throws JSONException {
		Stroke stroke1 = buildStroke(str1);
		Stroke stroke2 = buildStroke(str2);
		StrokeMatcher m = new StrokeMatcher(stroke1, stroke2, null);
		float f = m.similarity();
		assertEqualsFloat(expectedSimilarity, f);
	}

	public void testParallelLines() throws JSONException {
		// The best path is to advance equally along both, and they're 128 apart,
		// which is 1/2 the scale factor
		match(str1, str2, 0.5f, null);
		match(str2, str1, 0.5f, null);

	}

	public void testAFirst() throws JSONException {
		// We expect the path to advance along A until it reaches B's starting
		// point,
		// then advance together
		String str1 = "[0, 0,0,  1, 16,0, 2,32,0, 3,48,0, 4,64,0, 5,80,0, 6,96,0]";
		String str2 = "[0, 80,0, 1, 80,0, 2,80,0, 3,80,0, 4,80,0, 5,80,0, 6,96,0]";

		float unscaledCost = 80 * 80 + 64 * 64 + 48 * 48 + 32 * 32 + 16 * 16;
		float scaledCost = unscaledCost / 13; // total columns
		scaledCost = (float) Math.sqrt(scaledCost);
		scaledCost /= StrokeSet.STANDARD_WIDTH;
		match(str1, str2, scaledCost, null);

		// Verify that symmetric matching yields same result
		match(str2, str1, scaledCost, null);
	}

}
