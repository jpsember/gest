package com.js.gest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.js.basic.Freezable;
import com.js.basic.Point;
import static com.js.basic.Tools.*;

/**
 * A collection of Strokes, which ultimately will be recognized as a touch
 * gesture
 */
public class StrokeSet extends Freezable.Mutable implements Iterable<Stroke> {

	/**
	 * Add a point to a stroke within the set. Construct a stroke for this pointer
	 * id, if none currently exists
	 * 
	 * @param eventTime
	 *          time, in seconds, associated with point
	 * @param pointerId
	 *          id of pointer (i.e. finger) generating point
	 * @param pt
	 *          location of point
	 */
	public void addPoint(float eventTime, int pointerId, Point pt) {
		mutate();
		if (isEmpty())
			mInitialEventTime = eventTime;
		Stroke s = strokeForId(pointerId);
		s.addPoint(eventTime - mInitialEventTime, pt);
	}

	public static StrokeSet buildFromStrokes(List<Stroke> strokes) {
		StrokeSet s = new StrokeSet();
		for (Stroke stroke : strokes) {
			s.mStrokes.add(stroke);
		}
		s.freeze();
		return s;
	}

	/**
	 * Stop adding points to stroke corresponding to a pointer id, so that if a
	 * subsequent point is generated for this pointer id, it will be stored within
	 * a fresh stroke
	 */
	public void stopStroke(int pointerId) {
		mutate();
		mStrokeIdToIndexMap.remove(pointerId);
	}

	/**
	 * Determine if any strokes are active (i.e., not yet stopped)
	 */
	public boolean areStrokesActive() {
		if (isFrozen())
			return false;
		return !mStrokeIdToIndexMap.isEmpty();
	}

	@Override
	public void freeze() {
		if (isFrozen())
			return;
		if (areStrokesActive())
			throw new IllegalStateException();
		// Throw out unnecessary resources
		mStrokeIdToIndexMap = null;
		// Calculate bounds, now that frozen
		mBounds = calculateBounds();
		if (mBounds == null)
			throw new IllegalStateException("set has no points");
		for (Stroke s : mStrokes)
			s.freeze();
		super.freeze();
	}

	private Stroke strokeForId(int pointerId) {
		Integer strokeIndex = mStrokeIdToIndexMap.get(pointerId);
		if (strokeIndex == null) {
			strokeIndex = mStrokes.size();
			mStrokes.add(new Stroke());
			mStrokeIdToIndexMap.put(pointerId, strokeIndex);
		}
		return mStrokes.get(strokeIndex);
	}

	private boolean isEmpty() {
		return mStrokes.isEmpty();
	}

	public String toJSON(String name) throws JSONException {
		return StrokeSetCollectionParser.strokeSetToJSON(this, name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("StrokeSet\n");
		for (int strokeIndex = 0; strokeIndex < mStrokes.size(); strokeIndex++) {
			Stroke stroke = mStrokes.get(strokeIndex);

			// Display which current id corresponds to this stroke, or '-' if none
			String strokeIdString = "-";
			for (int mapStrokeId : mStrokeIdToIndexMap.keySet()) {
				int mapStrokeIndex = mStrokeIdToIndexMap.get(mapStrokeId);
				if (mapStrokeIndex == strokeIndex) {
					strokeIdString = "" + mapStrokeId;
					break;
				}
			}
			sb.append(" id:" + strokeIdString + " #:" + strokeIndex + " ");
			for (int i = 0; i < stroke.size(); i++) {
				StrokePoint pt = stroke.get(i);
				sb.append(d((int) pt.getPoint().x, 4));
				if (i > 16) {
					sb.append("...");
					break;
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public Iterator<Stroke> iterator() {
		return mStrokes.iterator();
	}

	/**
	 * Get the number of strokes in this set
	 */
	public int size() {
		return mStrokes.size();
	}

	public int length() {
		if (isEmpty())
			throw new IllegalStateException();
		return mStrokes.get(0).size();
	}

	public Stroke get(int index) {
		return mStrokes.get(index);
	}

	@Override
	public Freezable getMutableCopy() {
		assertFrozen();
		StrokeSet s = new StrokeSet();
		for (Stroke st : mStrokes) {
			s.mStrokes.add(mutableCopyOf(st));
		}
		return s;
	}

	public Rect getBounds() {
		assertFrozen();
		return mBounds;
	}

	private Rect calculateBounds() {
		Rect r = null;
		for (Stroke s : mStrokes) {
			for (StrokePoint spt : s) {
				Point pt = spt.getPoint();
				if (r == null)
					r = new Rect(pt, pt);
				r.include(pt);
			}
		}
		return r;
	}

	private ArrayList<Stroke> mStrokes = new ArrayList();
	private Rect mBounds;

	// Only used when mutable
	private float mInitialEventTime;
	private Map<Integer, Integer> mStrokeIdToIndexMap = new HashMap();
}
