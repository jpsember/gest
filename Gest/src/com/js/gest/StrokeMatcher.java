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

  public void setArguments(Stroke a, Stroke b, MatcherParameters parameters) {
    mSimilarityFound = false;
    mStrokeA = frozen(a);
    mStrokeB = frozen(b);
    if (mStrokeA.size() != mStrokeB.size())
      throw new IllegalArgumentException("stroke lengths mismatch");
    if (parameters == null)
      parameters = MatcherParameters.DEFAULT;
    mParameters = parameters;
    prepareTable();
  }

  public void setCostCutoff(float costCutoff) {
    if (costCutoff <= 0)
      costCutoff = Float.MAX_VALUE;
    mCostCutoff = costCutoff;
  }

  public float similarity() {
    if (!mSimilarityFound)
      calculateSimilarity();
    return mSimilarity;
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
    float startCost = comparePoints(0, 0);
    storeCost(0, 0, startCost);

    int tableSize = mTableSize;
    for (int x = 1; x < tableSize; x++) {
      for (int j = 0; j <= x; j++) {
        processCell(x - j, j);
      }
    }
    for (int y = 1; y < tableSize; y++) {
      for (int j = y; j < tableSize; j++) {
        processCell(tableSize - 1 + y - j, j);
      }
    }

    mSimilarity = cost();
    mSimilarityFound = true;
  }

  private void processCell(int a, int b) {
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
  }

  private float cost() {
    float c = mTable[mTable.length - 1];
    // Divide by the path length
    c /= mTableSize;
    c = (float) Math.sqrt(c);
    // Scale by the width of the standard rectangle
    c /= StrokeSet.STANDARD_WIDTH;
    return c;
  }

  private float comparePoints(int aIndex, int bIndex) {
    DataPoint elemA = mStrokeA.get(aIndex);
    DataPoint elemB = mStrokeB.get(bIndex);
    Point posA = elemA.getPoint();
    Point posB = elemB.getPoint();

    float dist;
    dist = MyMath.squaredDistanceBetween(posA, posB);
    if (dist < mParameters.zeroDistanceThreshold()
        * mParameters.zeroDistanceThreshold())
      dist = 0;
    return dist;
  }

  private float mCostCutoff = Float.MAX_VALUE;
  private int mTableSize;
  private float[] mTable;
  private Stroke mStrokeA;
  private Stroke mStrokeB;
  private boolean mSimilarityFound;
  private MatcherParameters mParameters;
  private float mSimilarity;
}
