package com.js.gest;

import static com.js.basic.Tools.*;

import com.js.basic.Freezable;

public class MatcherParameters extends Freezable.Mutable {

  private static final int FLAG_RANDOM_TEST_ORDER = (1 << 0);

  public static final MatcherParameters DEFAULT = frozen(new MatcherParameters());

  public MatcherParameters() {
    setMaximumCostRatio(1.3f);
    setWindowSize(Math
        .round(StrokeNormalizer.DEFAULT_DESIRED_STROKE_LENGTH * .20f));
    setRandomTestOrder(true);
    setMaxResults(3);
    // setSkewMax(.2f, 1);
    // setAlignmentAngle(MyMath.M_DEG * 20, 1);
  }

  /**
   * Get the maximum cost ratio. The matcher multiples this value by the
   * previous lowest cost gesture recognized, to use as an upper bound on
   * subsequent matching attempts
   */
  public float maximumCostRatio() {
    return mMaximumCostRatio;
  }

  public void setMaximumCostRatio(float ratio) {
    mutate();
    mMaximumCostRatio = ratio;
  }

  public void setWindowSize(int windowSize) {
    mutate();
    mWindowSize = windowSize;
  }

  public int windowSize() {
    return mWindowSize;
  }

  public void setMaxResults(int maxResults) {
    mutate();
    mMaxResults = maxResults;
  }

  public int maxResults() {
    return mMaxResults;
  }

  public void setAlignmentAngle(float angle, int numberOfSteps) {
    mutate();
    mAlignmentAngle = angle;
    mAlignmentAngleSteps = numberOfSteps;
  }

  public float alignmentAngle() {
    return mAlignmentAngle;
  }

  public boolean hasRotateOption() {
    return mAlignmentAngle != 0;
  }

  public boolean hasSkewOption() {
    return mSkewXMax != 0;
  }

  public int alignmentAngleSteps() {
    return mAlignmentAngleSteps;
  }

  public void setSkewMax(float skewXMax, int numberOfSteps) {
    mutate();
    mSkewXMax = skewXMax;
    mSkewSteps = numberOfSteps;
  }

  public float skewXMax() {
    return mSkewXMax;
  }

  public int skewSteps() {
    return mSkewSteps;
  }

  @Override
  public Freezable getMutableCopy() {
    MatcherParameters m = new MatcherParameters();
    m.mFlags = mFlags;
    m.mRandomSeed = mRandomSeed;
    m.setMaximumCostRatio(maximumCostRatio());
    m.setWindowSize(windowSize());
    m.setAlignmentAngle(alignmentAngle(), alignmentAngleSteps());
    m.setSkewMax(skewXMax(), skewSteps());
    m.setMaxResults(maxResults());
    return m;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MatcherParameters");
    sb.append("\n max cost ratio: " + d(maximumCostRatio()));
    sb.append("\n    window size: " + d(windowSize()));
    return sb.toString();
  }

  public void setRandomTestOrder(boolean state) {
    setFlag(FLAG_RANDOM_TEST_ORDER, state);
  }

  public boolean hasRandomTestOrder() {
    return hasFlag(FLAG_RANDOM_TEST_ORDER);
  }

  public void setRandomSeed(int seed) {
    mutate();
    mRandomSeed = seed;
  }

  public int randomSeed() {
    return mRandomSeed;
  }

  private void setFlag(int flag, boolean state) {
    mutate();
    if (!state)
      mFlags &= ~flag;
    else
      mFlags |= flag;
  }

  private boolean hasFlag(int flag) {
    return 0 != (mFlags & flag);
  }

  private int mWindowSize;
  private float mMaximumCostRatio;
  private float mAlignmentAngle;
  private int mAlignmentAngleSteps;
  private float mSkewXMax;
  private int mSkewSteps;
  private int mFlags;
  private int mMaxResults;
  private int mRandomSeed;
}
