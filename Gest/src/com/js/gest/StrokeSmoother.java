package com.js.gest;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.List;

import com.js.basic.Point;

public class StrokeSmoother {

	public StrokeSmoother(StrokeSet set) {
		mSet = set;
	}

	public StrokeSet getSmoothedSet() {
		if (mSmoothed == null) {
			List<Stroke> smoothedList = new ArrayList();
			for (Stroke s : mSet) {
				Stroke smoothed = smoothStroke(s);
				smoothedList.add(smoothed);
			}
			mSmoothed = StrokeSet.buildFromStrokes(smoothedList);
		}
		return mSmoothed;
	}

	private Point calcStrokeVelocity(Stroke stroke, int index) {
		if (index == 0 || index == stroke.length() - 1)
			return Point.ZERO;

		StrokePoint e0 = stroke.get(index - 1);
		StrokePoint e1 = stroke.get(index);
		StrokePoint e2 = stroke.get(index + 1);

		float ta = e1.getTime() - e0.getTime();
		float tb = e2.getTime() - e1.getTime();

		Point va = new Point((e1.getPoint().x - e0.getPoint().x) / ta,
				(e1.getPoint().y - e0.getPoint().y) / ta);
		Point vb = new Point((e2.getPoint().x - e1.getPoint().x) / tb,
				(e2.getPoint().y - e1.getPoint().y) / tb);

		/*
		 * If the next segment has moved a great distance, we'll be predicting a
		 * near-instantaneous velocity which can cause initial sharp movements in
		 * the opposite direction. To remedy this, if the larger velocity is much
		 * greater than the smaller, scale it back so that their difference is not
		 * too great (especially as the smaller velocity nears zero).
		 */

		float ma = va.magnitude();
		float mb = vb.magnitude();
		if (ma > mb) {
			Point tmp = va;
			va = vb;
			vb = tmp;
			float tmp2 = ma;
			ma = mb;
			mb = tmp2;
		}

		if (mb > 0) {
			float s = ma / mb;
			float THRESHOLD = 0.25f;
			if (s < THRESHOLD) {
				// Scale the larger velocity so it approaches the smaller
				s = s / THRESHOLD;
				vb = new Point(vb.x * s, vb.y * s);
			}
		}
		return new Point((va.x + vb.x) / 2, (va.y + vb.y) / 2);
	}

	private Stroke smoothStroke(Stroke sOrig) {
		Stroke sNew = new Stroke();
		StrokePoint prev = null;
		Point velocityPrev = null;
		for (int strokeIndex = 0; strokeIndex < sOrig.length(); strokeIndex++) {
			StrokePoint pt = sOrig.get(strokeIndex);
			Point velocity = calcStrokeVelocity(sOrig, strokeIndex);

			if (prev == null) {
				sNew.addPoint(pt.getTime(), pt.getPoint());

			} else {

				float tDiff = pt.getTime() - prev.getTime();
				Point p1 = prev.getPoint();
				Point p2 = pt.getPoint();

				Point d1 = new Point(velocityPrev.x * tDiff, velocityPrev.y * tDiff);
				Point d2 = new Point(velocity.x * tDiff, velocity.y * tDiff);

				// Determine how many steps to interpolate. We should oversample
				// somewhat, so we induce a nice curve on the points, which can then be
				// downsampled by the normalization process.

				int kNumSteps = Math.max(2, (int) (tDiff * 120));

				float t = 0.0f;
				float tStep = 1.0f / kNumSteps;

				int totalSteps = kNumSteps;

				for (int i = 0; i < totalSteps; i++) {
					t += tStep;

					float t2 = t * t;
					float t3 = t2 * t;

					float m1 = (2 * t3 - 3 * t2 + 1);
					float m2 = (-2 * t3 + 3 * t2);
					float m3 = (t3 - 2 * t2 + t);
					float m4 = (t3 - t2);

					float x = m1 * p1.x + m2 * p2.x + m3 * d1.x + m4 * d2.x;
					float y = m1 * p1.y + m2 * p2.y + m3 * d1.y + m4 * d2.y;

					float time = t * pt.getTime() + (1 - t) * prev.getTime();

					Point curr = new Point(x, y);
					sNew.addPoint(time, curr);
				}

			}
			velocityPrev = velocity;
			prev = pt;

		}
		return sNew;
	}

	private StrokeSet mSet;
	private StrokeSet mSmoothed;
}
