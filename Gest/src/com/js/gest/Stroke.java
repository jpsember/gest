package com.js.gest;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.basic.JSONTools;
import com.js.basic.Point;
import com.js.basic.Freezable;

import android.graphics.Matrix;
import static com.js.basic.Tools.*;

/**
 * A Stroke is a sequence of StrokePoints, with strictly increasing times.
 * 
 * It represents a user's touch/drag/release action, for a single finger.
 */
public class Stroke extends Freezable.Mutable implements Iterable<StrokePoint> {

	public Stroke() {
		mPoints = new ArrayList();
		mStartTime = -1;
	}

	public void clear() {
		mutate();
		mPoints.clear();
		mStartTime = -1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(nameOf(this));
		sb.append("Stroke\n");
		int index = 0;
		for (StrokePoint pt : mPoints) {
			sb.append(d(index));
			sb.append(" ");
			sb.append(pt);
			sb.append('\n');
			index++;
		}

		return sb.toString();
	}

	public int length() {
		return mPoints.size();
	}

	public StrokePoint get(int index) {
		return mPoints.get(index);
	}

	public StrokePoint last() {
		return com.js.basic.Tools.last(mPoints);
	}

	public StrokePoint pop() {
		mutate();
		return com.js.basic.Tools.pop(mPoints);
	}

	public boolean isEmpty() {
		return mPoints.isEmpty();
	}

	public void addPoint(StrokePoint point) {
		addPoint(point.getTime(), point.getPoint());
	}

	public void addPoint(float time, Point location) {
		mutate();
		if (isEmpty()) {
			mStartTime = time;
		} else {
			StrokePoint elem = last();
			float lastTime = elem.getTime() + mStartTime;
			float elapsedTime = time - lastTime;
			// Make sure time is strictly increasing
			if (elapsedTime <= 0) {
				time = lastTime + 0.001f;
			}
		}
		StrokePoint pt = new StrokePoint(time - mStartTime, location);
		mPoints.add(pt);
	}

	/**
	 * Construct stroke as a portion of this one
	 * 
	 * @param start
	 *          index of first point to appear in fragment
	 * @param end
	 *          one plus index of last point to appear in fragment
	 */
	public Stroke constructFragment(int start, int end) {
		Stroke fragment = new Stroke();
		for (int i = start; i < end; i++)
			fragment.addPoint(get(i));
		return fragment;
	}

	private static final float FLOAT_TIME_SCALE = 120.0f;

	@Deprecated
	public String toJSON() throws JSONException {
		return toJSONArray().toString();
	}

	public JSONArray toJSONArray() throws JSONException {
		JSONArray a = new JSONArray();
		for (StrokePoint pt : mPoints) {
			a.put((int) (pt.getTime() * FLOAT_TIME_SCALE));
			a.put((int) pt.getPoint().x);
			a.put((int) pt.getPoint().y);
		}
		return a;
	}

	public static Stroke parseJSONArray(JSONArray array) throws JSONException {
		Stroke s = new Stroke();

		if (array.length() % 3 != 0)
			throw new JSONException("malformed stroke array");
		int nPoints = array.length() / 3;

		for (int i = 0; i < nPoints; i++) {
			int j = i * 3;
			float time = array.getInt(j + 0) / FLOAT_TIME_SCALE;
			float x = array.getInt(j + 1);
			float y = array.getInt(j + 2);
			s.addPoint(time, new Point(x, y));
		}
		return s;
	}

	@Deprecated
	public static Stroke parseJSON(String script) throws JSONException {
	 final String JSON_KEY_POINTS = "pts";
	 	JSONObject map = JSONTools.parseMap(script);
		if (!map.has(JSON_KEY_POINTS)) {
			throw new JSONException(JSON_KEY_POINTS + " key missing");
		}
		return parseJSONArray(map.getJSONArray(JSON_KEY_POINTS));
	}

	/**
	 * Transform all points on path, returning a new path
	 */
	public Stroke transformBy(Matrix matrix) {
		Stroke p = new Stroke();
		for (StrokePoint pt : mPoints) {
			Point pos = new Point(pt.getPoint());
			pos.apply(matrix);
			p.addPoint(pt.getTime(), pos);
		}
		return p;
	}

	/**
	 * Apply translation to points, returning a new path
	 */
	public Stroke translateBy(Point point) {
		Matrix m = null;
		return transformBy(m);
	}

	public Iterator<StrokePoint> iterator() {
		return mPoints.iterator();
	}

	@Override
	public Freezable getMutableCopy() {
		Stroke s = new Stroke();
		s.mStartTime = mStartTime;
		for (StrokePoint pt : mPoints) {
			s.mPoints.add(new StrokePoint(pt));
		}
		return s;
	}

	public float totalTime() {
		float time = 0;
		if (!mPoints.isEmpty())
			time = last().getTime();
		return time;
	}

	private ArrayList<StrokePoint> mPoints;
	private float mStartTime;

}
