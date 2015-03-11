package com.js.gest;

import static com.js.basic.Tools.*;

import java.util.ArrayList;
import java.util.Collections;

import com.js.basic.Point;

/**
 * Determines how closely two strokes match
 */
class StrokeMatcher {

	public StrokeMatcher(Stroke a, Stroke b) {
		mStrokeA = frozen(a);
		mStrokeB = frozen(b);
		setDistanceThreshold(.01f);
	}

	public void setDistanceThreshold(float factor) {
		if (matched())
			throw new IllegalStateException();
		mZeroThreshold = (float) Math.pow(StrokeSet.STANDARD_WIDTH * factor, 2);
	}

	private boolean matched() {
		return mSimilarity != null;
	}

	public float similarity() {
		if (mSimilarity == null)
			calculateSimilarity();
		return mSimilarity;
	}

	/**
	 * Construct the optimal path within the dynamic table
	 * 
	 * @return an array of cells leading from the bottom left to the top right
	 */
	public ArrayList<Cell> optimalPath() {
		if (!matched())
			throw new IllegalStateException();
		ArrayList<Cell> list = new ArrayList();

		Cell cell = mBestCell;
		while (cell != null) {
			list.add(cell);
			cell = cell.getPrevCell();
		}
		Collections.reverse(list);
		return list;
	}

	private void calculateSimilarity() {
		if (mStrokeA.size() != mStrokeB.size())
			throw new IllegalArgumentException("stroke lengths mismatch");
		prepare();

		for (int iter = 0; iter < mAxisLength; iter++) {
			int x = (iter + 1) / 2;
			int y = x;
			boolean parity = (iter & 1) == 1;
			if (!parity)
				x += 1;
			generateDynamicTableColumn(parity, x + mWindowSize, y - mWindowSize);
		}

		mBestCell = mColumn1[mWindowSize];

		mSimilarity = cost();
	}

	/**
	 * Generate another column in the dynamic programming table
	 * 
	 * @param parity
	 *          false for first column, true for second, etc
	 * @param aBottomCursor
	 *          path cursor for bottom row of the new column
	 * @param bBottomCursor
	 */
	private void generateDynamicTableColumn(boolean parity, int aBottomCursor,
			int bBottomCursor) {

		mColumn2 = buildColumn();
		int y_start;
		int y_end;
		if (!parity) {
			y_start = 1;
			y_end = mColumnSize - 1;
		} else {
			y_start = 1;
			y_end = mColumnSize - 1;
			//
			// Since we're rotating a square matrix by 45 degrees to sweep it using
			// vertical columns,
			// there's an asymmetry on odd-numbered columns, in that the 'center' cell
			// is actually below the
			// 45 degree line. For this reason, omit the lowest cell in these columns,
			// so we are examining
			// the same number of cells above and below the 45 degree line every time.
			//
			y_end -= 1;
		}
		for (int y = y_start; y <= y_end; y++) {
			int a_index = aBottomCursor - y;
			int b_index = bBottomCursor + y;
			if (!isLegalCell(a_index, b_index))
				continue;
			clearOptimalEdge();
			// Multiply by 2 here, since we count each distance twice when advancing
			// along both paths
			examineEdge(mColumn0[y], a_index, b_index, 2);
			examineEdge(mColumn1[y], a_index, b_index, 1);
			if (!parity) {
				examineEdge(mColumn1[y - 1], a_index, b_index, 1);
			} else {
				examineEdge(mColumn1[y + 1], a_index, b_index, 1);
			}
			storeBestEdgeIntoCell(y, a_index, b_index);
		}
		mColumn0 = mColumn1;
		mColumn1 = mColumn2;
	}

	private boolean isLegalCell(int aIndex, int bIndex) {
		return aIndex >= 0 && aIndex < pathLength() && bIndex >= 0
				&& bIndex < pathLength();
	}

	/**
	 * Initialize 'best edge' to undefined for candidates leading into a cell
	 */
	private void clearOptimalEdge() {
		mMinCost = 0;
		mMinPredecessor = null;
	}

	/**
	 * Examine an edge from a source to a destination cell, and store as the
	 * optimal edge if the resulting total cost at the destination is the minimum
	 * seen yet
	 * 
	 * @param sourceCell
	 *          source cell; if null, does nothing
	 * @param a
	 *          coefficients of destination cell
	 * @param b
	 * @param multiplier
	 *          amount to weight the cost of the edge; normally 1, but can be 2 if
	 *          edge represents advancement along both strokes
	 */
	private void examineEdge(Cell sourceCell, int a, int b, float multiplier) {
		if (sourceCell == null)
			return;
		float cost = comparePoints(a, b);
		cost = cost * multiplier + sourceCell.cost();

		float diff = mMinCost - cost;
		diff = mMinCost - cost;

		if (mMinPredecessor == null || diff > 0) {
			mMinCost = cost;
			mMinPredecessor = sourceCell;
		}
	}

	/**
	 * Store the optimal edge leading to this cell (does nothing if no optimal
	 * edge exists)
	 */
	private void storeBestEdgeIntoCell(int row, int a_index, int b_index) {
		if (mMinPredecessor == null)
			return;
		Cell cell = buildNewCell(a_index, b_index);
		cell.setCost(mMinCost);
		cell.setPrevCell(mMinPredecessor);
		mColumn2[row] = cell;
	}

	private float cost() {
		if (mBestCell == null)
			throw new IllegalStateException();
		return mBestCell.cost() / pathLength();
	}

	private int pathLength() {
		return mStrokeA.size();
	}

	private void prepare() {
		mAxisLength = 2 * (pathLength() - 1);
		mWindowSize = Math.round((float) Math.sqrt(mAxisLength) / 2);
		mWindowSize = Math.max(mWindowSize, 15);
		mWindowSize = Math.min(mWindowSize, mAxisLength);
		unimp("Experiment with window size");

		mColumnSize = 1 + 2 * mWindowSize;
		buildColumns();

		mBestCell = null;
	}

	private void buildColumns() {
		mColumn0 = buildColumn();
		mColumn1 = buildColumn();

		Cell cell_0 = buildNewCell(0, 0);

		float cost = comparePoints(0, 0);
		cell_0.setCost(cost);
		mColumn1[mWindowSize] = cell_0;
	}

	private float comparePoints(int aIndex, int bIndex) {
		StrokePoint elem_a = mStrokeA.get(aIndex);
		StrokePoint elem_b = mStrokeB.get(bIndex);
		Point pos_a = elem_a.getPoint();
		Point pos_b = elem_b.getPoint();

		float dist = MyMath.squaredDistanceBetween(pos_a, pos_b);
		if (dist < mZeroThreshold)
			dist = 0;
		return dist;
	}

	private Cell buildNewCell(int aIndex, int bIndex) {
		Cell cell = new Cell(aIndex, bIndex);
		return cell;
	}

	private Cell[] buildColumn() {
		return new Cell[mColumnSize];
	}

	private Stroke mStrokeA;
	private Stroke mStrokeB;
	private Float mSimilarity;
	// The maximum distance a path can stray from the central axis
	private int mWindowSize;
	// The maximum number of steps in a path taken to compare the two
	// strokes. Each step advances along at least one of the paths, possibly
	// both
	private int mAxisLength;
	// if distance between points is less than this, treated as zero
	private float mZeroThreshold;
	private int mColumnSize;
	private Cell[] mColumn0;
	private Cell[] mColumn1;
	private Cell[] mColumn2;
	private Cell mBestCell;
	private float mMinCost;
	private Cell mMinPredecessor;
}
