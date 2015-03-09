package com.js.gest;

public class StrokeSetEntry {

	public StrokeSet strokeSet() {
		return mStrokeSet;
	}

	public String name() {
		return mName;
	}

	/**
	 * Get the stroke set this one is an alias of; returns 'this' if none
	 */
	public StrokeSetEntry alias() {
		if (mAlias == null)
			return this;
		return mAlias;
	}

	public boolean hasAlias() {
		return mAlias != null;
	}

	StrokeSetEntry(String name) {
		mName = name;
	}

	void setStrokeSet(StrokeSet strokeSet) {
		mStrokeSet = strokeSet;
	}

	void setAlias(StrokeSetEntry alias) {
		mAlias = alias;
	}

	private StrokeSetEntry mAlias;
	private String mName;
	private StrokeSet mStrokeSet;
}
