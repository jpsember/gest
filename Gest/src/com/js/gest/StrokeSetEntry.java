package com.js.gest;

public class StrokeSetEntry {

	public StrokeSet strokeSet() {
		return mStrokeSet;
	}

	public String name() {
		return mName;
	}

	/**
	 * Get the name of the stroke set this one is an alias of; returns our name if
	 * we are not an alias
	 */
	public String aliasName() {
		return mAliasName;
	}

	public boolean hasAlias() {
		return mAliasName != mName;
	}

	StrokeSetEntry(String name) {
		mName = name;
	}

	void setStrokeSet(StrokeSet strokeSet) {
		mStrokeSet = strokeSet;
	}

	void setAlias(StrokeSetEntry strokeSetEntry) {
		mAliasName = strokeSetEntry.aliasName();
	}

	private String mAliasName;
	private String mName;
	private StrokeSet mStrokeSet;
}
