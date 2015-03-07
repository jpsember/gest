package com.js.gest.test;

import org.json.JSONException;
import static com.js.basic.Tools.*;

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
		if (false) {
			// For verifying JSON appearance
			try {
				pr("built stroke " + s.toJSON());
			} catch (JSONException e) {
				die(e);
			}
		}
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
		String json1 = stroke1.toJSON();
		Stroke stroke2 = Stroke.parseJSON(json1);
		assertEquals(stroke2.length(), stroke1.length());
		String json2 = stroke2.toJSON();
		assertEquals(json1, json2);
	}

}
