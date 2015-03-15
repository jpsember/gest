package com.js.gest;

import static com.js.basic.Tools.*;

import com.js.basic.MyMath;
import com.js.basic.Point;
import com.js.gest.Stroke.DataPoint;

/**
 * Determines how closely two strokes match
 * 
 * Public for test purposes
 * 
 * Consider making this package visibility only, and testing at higher level
 */
public class StrokeMatcher {

  /**
   * A value representing 'infinite' cost. It should not be so large that it
   * can't be safely doubled or tripled without overflowing
   */
  public static final float INFINITE_COST = Float.MAX_VALUE / 100;

  /**
   * Prepare matcher for new pair of strokes. Also resets cost cutoff
   * 
   * @param a
   * @param b
   * @param parameters
   */
  public void setArguments(Stroke a, Stroke b, MatcherParameters parameters) {
    mStrokeA = frozen(a);
    mStrokeB = frozen(b);
    if (mStrokeA.size() != mStrokeB.size())
      throw new IllegalArgumentException("stroke lengths mismatch");
    if (parameters == null)
      parameters = MatcherParameters.DEFAULT;
    mParameters = parameters;
    prepareTable();
    setMaximumCost(INFINITE_COST);
    mCostCalculated = false;
  }

  /**
   * Set upper bound on the cost. The algorithm will exit early if it determines
   * the cost will exceed this bound
   */
  public void setMaximumCost(float maximumCost) {
    mMaximumCost = maximumCost;
  }

  /**
   * Determine the cost, or distance, between the two strokes. This is a 'raw'
   * value, i.e., without normalizing by the stroke length or the size of the
   * stroke's bounding rectangle
   */
  public float cost() {
    if (!mCostCalculated) {
      if (mStrokeA == null)
        throw new IllegalStateException();
      calculateSimilarity();
    }
    return mRawCost;
  }

  /**
   * Determine a normalized cost value. This is a 'raw' cost, scaled so it is
   * independent of the length of the strokes, or their bounding rectangle
   * 
   * @param rawCost
   */
  public float normalizedCost(float rawCost) {
    if (mStrokeA == null)
      throw new IllegalStateException();
    // Divide by the path length, to get approximately the average cost per data
    // point
    float c = rawCost / mTableSize;
    // Take square root, since costs at each point were based on squared
    // distances
    c = (float) Math.sqrt(c);
    // Scale by the width of the standard rectangle
    c /= StrokeSet.STANDARD_WIDTH;
    return c;
  }

  /**
   * For diagnostic / test purposes, calculate the ratio of actual cells
   * examined to the potential total cells examined by this matcher
   */
  public float cellsExaminedRatio() {
    if (mTotalCellCount == 0)
      return 0;
    return ((float) mActualCellsExamined) / mTotalCellCount;
  }

  private void prepareTable() {
    mTableSize = mStrokeA.size();
    int tableCells = mTableSize * mTableSize;
    if (mTable == null || mTable.length != tableCells) {
      mTable = new float[tableCells];
    }
  }

  private int cellIndex(int a, int b) {
    return a + b * mTableSize;
  }

  private void storeCost(int a, int b, float cost) {
    mTable[cellIndex(a, b)] = cost;
  }

  /**
   * Perform matching algorithm
   * 
   * We perform dynamic programming. Conceptually we construct a square table of
   * cells, where the x axis represents the cursor within stroke A, and the y
   * axis the cursor within stroke B. Thus x and y range from 0...n-1, where n
   * is the length of the stroke.
   * 
   * The initial state is with x=y=0 (the bottom left corner), and the final
   * state is x=y=n-1 (the top right corner).
   * 
   * Each cell (x,y) in the table stores the lowest cost leading to that cell,
   * where possible moves are from (x-1,y), (x-1,y-1), or (x,y-1).
   */
  private void calculateSimilarity() {
    mTotalCellCount += mTable.length;

    float startCost = comparePoints(0, 0);
    storeCost(0, 0, startCost);

    mCostCalculated = true;
    mRawCost = INFINITE_COST;

    int tableSize = mTableSize;
    for (int x = 1; x < tableSize; x++) {
      float minCost = INFINITE_COST;
      for (int j = 0; j <= x; j++) {
        minCost = Math.min(minCost, processCell(x - j, j));
      }
      if (minCost >= mMaximumCost) {
        return;
      }
    }
    for (int y = 1; y < tableSize; y++) {
      float minCost = INFINITE_COST;
      for (int j = y; j < tableSize; j++) {
        minCost = Math.min(minCost, processCell(tableSize - 1 + y - j, j));
      }
      if (minCost >= mMaximumCost) {
        return;
      }
    }
    mRawCost = mTable[mTable.length - 1];
  }

  private float processCell(int a, int b) {
    int abIndex = cellIndex(a, b);
    float bestCost;
    float abCost = comparePoints(a, b);
    if (a > 0) {
      bestCost = mTable[abIndex - 1] + abCost;
      if (b > 0) {
        float prevCost = mTable[abIndex - mTableSize] + abCost;
        if (bestCost > prevCost)
          bestCost = prevCost;
        // Multiply cost by root(2), since we're moving diagonally
        prevCost = mTable[abIndex - mTableSize - 1] + abCost * 1.41421356237f;
        if (bestCost > prevCost)
          bestCost = prevCost;
      }
    } else {
      bestCost = mTable[abIndex - mTableSize] + abCost;
    }
    mTable[abIndex] = bestCost;
    return bestCost;
  }

  private float comparePoints(int aIndex, int bIndex) {
    DataPoint elemA = mStrokeA.get(aIndex);
    DataPoint elemB = mStrokeB.get(bIndex);
    Point posA = elemA.getPoint();
    Point posB = elemB.getPoint();
    mActualCellsExamined++;
    float dist;
    dist = MyMath.squaredDistanceBetween(posA, posB);
    if (dist < mParameters.zeroDistanceThreshold()
        * mParameters.zeroDistanceThreshold())
      dist = 0;
    return dist;
  }

  private int mTableSize;
  private float[] mTable;
  private Stroke mStrokeA;
  private Stroke mStrokeB;
  private boolean mCostCalculated;
  private MatcherParameters mParameters;
  private float mRawCost;
  private float mMaximumCost;
  private int mActualCellsExamined;
  private int mTotalCellCount;
}
