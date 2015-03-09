package com.js.gest;

import static com.js.basic.Tools.*;

class Cell {

	public Cell(int aIndex, int bIndex) {
		mIndexA = aIndex;
		mIndexB = bIndex;
		mCost = 0;
	}

	public int distanceAlongAxis() {
		return mIndexA + mIndexB;
	}

	public float distanceFromAxis() {
		return Math.abs(distanceAlongAxis() / 2.0f - mIndexA);
	}

	public void setCost(float cost) {
		mCost = cost;
	}

	public float cost() {
		return mCost;
	}

	public void setPrevCell(Cell cell) {
		mPrevCell = cell;
	}

	public Cell getPrevCell() {
		return mPrevCell;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Cell ");
		sb.append(d(mIndexA));
		sb.append(d(mIndexB));
		sb.append(d(cost()));
		return sb.toString();
	}

	private int mIndexA;
	private int mIndexB;
	private float mCost;
	private Cell mPrevCell;

}
