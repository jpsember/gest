package com.js.gest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	 * @param pointerId
	 * @param pt
	 */
	public void addPoint(float eventTime, int pointerId, Point pt) {
		mutate();
		if (isEmpty())
			mInitialEventTime = eventTime;
		Stroke s = strokeForId(pointerId);
		s.addPoint(eventTime - mInitialEventTime, pt);
	}

	/**
	 * Stop adding points to stroke corresponding to a pointer id, so that if a
	 * subsequent point is generated for this pointer id, it will be stored within
	 * a fresh stroke
	 * 
	 * @param pointerId
	 */
	public void stopStroke(int pointerId) {
		mutate();
		mStrokeIdToIndexMap.remove(pointerId);
	}

	/**
	 * Determine if all strokes in the set have been completed
	 */
	public boolean isComplete() {
		return !isEmpty() && mStrokeIdToIndexMap.isEmpty();
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
			for (int i = 0; i < stroke.length(); i++) {
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

	@Override
	public Freezable getMutableCopy() {
		if (!isComplete()) throw new IllegalStateException();
		StrokeSet s = new StrokeSet();
		for (Stroke st : mStrokes) {
			s.mStrokes.add(mutableCopyOf(st));
		}
		return s;
	}

	private float mInitialEventTime;
	private Map<Integer, Integer> mStrokeIdToIndexMap = new HashMap();
	private ArrayList<Stroke> mStrokes = new ArrayList();
}
