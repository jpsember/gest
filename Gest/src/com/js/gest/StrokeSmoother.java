package com.js.gest;
import static com.js.basic.Tools.*;


public class StrokeSmoother {

	public StrokeSmoother(StrokeSet set) {
		mSet = set;
	}

	public StrokeSet perform() {
		if (mSmoothed == null) {
			mSmoothed = mSet;
			unimp("smooth");
		}
		return mSmoothed;
	}

	private StrokeSet mSet;
	private StrokeSet mSmoothed;
}
