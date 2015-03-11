package com.js.gest.test;

import org.json.JSONArray;
import org.json.JSONException;

import com.js.basic.Point;
import com.js.gest.MyMath;
import com.js.gest.Stroke;
import com.js.testUtils.*;

public class StrokeTest extends MyTestCase {

	private Stroke buildStroke() {
		Stroke s = new Stroke();
		Point pt = new Point(50, 50);
		float dir = 0;
		float time = 30;
		for (int i = 0; i < 20; i++) {
			dir += ((random().nextFloat() * random().nextFloat()) - .5f)
					* MyMath.M_DEG * 30;
			pt = MyMath.pointOnCircle(pt, dir, 10);
			s.addPoint(time, pt);
			time += random().nextFloat() * .05f + 0.01f;
		}
		s.freeze();
		return s;
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

	public void testJSON() throws JSONException {
		Stroke stroke1 = buildStroke();
		String json1 = stroke1.toJSONArray().toString();
		JSONArray array1 = new JSONArray(json1);
		Stroke stroke2 = Stroke.parseJSONArray(array1);
		assertEquals(stroke2.size(), stroke1.size());
		String json2 = stroke2.toJSONArray().toString();
		assertEquals(json1, json2);
	}

}
