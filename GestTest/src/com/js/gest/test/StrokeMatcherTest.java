package com.js.gest.test;

import org.json.JSONArray;
import org.json.JSONException;

import com.js.basic.Point;
import com.js.gest.MatcherParameters;
import com.js.gest.Stroke;
import com.js.gest.StrokeMatcher;
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

    StrokeMatcher m = new StrokeMatcher();
    m.setArguments(stroke1, stroke2, null);
    assertEqualsFloat(expectedSimilarity, m.cost());
  }

  public void testParallelLines() throws JSONException {
    float exp = 16384.0f;
    match(str1, str2, exp, null);
    match(str2, str1, exp, null);
  }

  public void testAFirst() throws JSONException {
    // We expect the path to advance along A until it reaches B's starting
    // point,
    // then advance together
    String str1 = "[0, 0,0,  1, 16,0, 2,32,0, 3,48,0, 4,64,0, 5,80,0, 6,96,0]";
    String str2 = "[0, 80,0, 1, 80,0, 2,80,0, 3,80,0, 4,80,0, 5,80,0, 6,96,0]";

    // Verify that symmetric matching yields same result
    match(str2, str1, 1462.85717f, null);
  }

  public void testRoughMatch() throws JSONException {
    String str1 = "[0,0,1,  1,23,40,  2,47,80,  3,99,169,  4,151,239, 5,161,207, 6,208,98,  7,255,13]";
    String str2 = "[0,0,93, 1,15,142, 2,72,176, 3,103,134, 4,162,154, 5,213,175, 6,242,134, 7,255,76]";

    match(str1, str2, 3747.0625f, null);
  }

}
