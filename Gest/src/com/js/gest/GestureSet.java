package com.js.gest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONException;

import com.js.basic.Files;

import static com.js.basic.Tools.*;

public class GestureSet {

  public static GestureSet parseJSON(String script) throws JSONException {
    GestureSetParser p = new GestureSetParser();
    GestureSet collection = new GestureSet();
    p.parse(script, collection);
    return collection;
  }

  public static GestureSet readFromClassResource(Class klass, String filename)
      throws IOException, JSONException {
    InputStream stream = klass.getResourceAsStream(filename);
    String json = Files.readString(stream);
    return GestureSet.parseJSON(json);
  }

  /**
   * Get the map containing the gestures
   */
  private Map<String, StrokeSet> map() {
    return mEntriesMap;
  }

  /**
   * Get gesture
   * 
   * @param name
   *          name of gesture to look for
   * @return gesture, or null
   */
  public StrokeSet get(String name) {
    return map().get(name);
  }

  /**
   * Get the length of the strokes used in this set's gestures
   */
  public int strokeLength() {
    return mStrokeLength;
  }

  public void add(StrokeSet set) {
    set = frozen(set);
    set.assertNamed();
    // If the library is currently empty, set its length to the length of this
    // set's strokes
    if (mEntriesMap.isEmpty()) {
      mStrokeLength = set.length();
    }
    mEntriesMap.put(set.name(), set);
  }

  public void setTraceStatus(boolean trace) {
    mTrace = trace;
  }

  /**
   * Find a match for a gesture, if possible
   * 
   * @param inputSet
   *          gesture to examine
   * @param param
   *          parameters for match, or null to use defaults
   * @param resultsList
   *          optional list of highest candidate gestures; if not null, they
   *          will be returned in this list
   * @return match found, or null
   */
  public Match findMatch(StrokeSet inputSet, MatcherParameters param,
      List<Match> resultsList) {
    if (mTrace)
      pr("GestureSet findMatch");
    if (param == null)
      param = MatcherParameters.DEFAULT;
    if (resultsList != null)
      resultsList.clear();
    TreeSet<Match> results = new TreeSet();
    StrokeSetMatcher m = new StrokeSetMatcher();
    float maximumCost = StrokeMatcher.INFINITE_COST;
    for (String setName : mEntriesMap.keySet()) {
      StrokeSet set = mEntriesMap.get(setName);
      if (set.size() != inputSet.size())
        continue;
      m.setArguments(set, inputSet, param);
      m.setMaximumCost(maximumCost);
      Match match = new Match(set, m.normalizedCost(m.cost()));
      results.add(match);

      // Update the cutoff value to be some small multiple of the smallest (raw)
      // cost yet seen.
      // Scale the raw cost by the number of strokes since the cost of the set
      // is the sum of the costs of the individual strokes.
      float newLimit = m.cost() / inputSet.size();
      newLimit *= param.maximumCostRatio();
      maximumCost = Math.min(newLimit, maximumCost);
      if (mTrace)
        pr(" gesture: " + d(setName, "15p") + " cost:" + dumpCost(m.cost())
            + " max:" + dumpCost(maximumCost) + " cells %:"
            + d((int) (100 * m.strokeMatcher().cellsExaminedRatio())));

      // If second entry is an alias of the first, throw it out; there's no
      // need
      // to keep it, since it shouldn't affect a match decision
      if (results.size() >= 2) {
        Iterator<Match> iter = results.iterator();
        Match m1 = iter.next();
        Match m2 = iter.next();
        if (m1.strokeSet().aliasName() == m2.strokeSet().aliasName()) {
          results.remove(m2);
        }
      }

      // Throw out all but top three
      while (results.size() > 3)
        results.pollLast();
    }
    if (results.isEmpty())
      return null;

    if (resultsList != null) {
      resultsList.addAll(results);
    }

    return results.first();
  }

  /**
   * Utility method for converting a (raw) cost value to a string
   */
  private static String dumpCost(float cost) {
    if (cost >= StrokeMatcher.INFINITE_COST)
      return " ********";
    return d(((int) cost), 8);
  }

  public static class Match implements Comparable {
    Match(StrokeSet set, float cost) {
      set.assertNamed();
      mStrokeSet = set;
      mCost = cost;
    }

    public float cost() {
      return mCost;
    }

    /**
     * Get StrokeSet
     */
    public StrokeSet strokeSet() {
      return mStrokeSet;
    }

    @Override
    public int compareTo(Object another) {
      Match m = (Match) another;
      int diff = (int) Math.signum(this.cost() - m.cost());
      if (diff == 0) {
        diff = String.CASE_INSENSITIVE_ORDER.compare(this.strokeSet().name(), m
            .strokeSet().name());
      }
      return diff;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(d(cost()).substring(4));
      sb.append(' ');
      sb.append(strokeSet().name());
      if (strokeSet().hasAlias())
        sb.append(" --> " + strokeSet().aliasName());
      return sb.toString();
    }

    private StrokeSet mStrokeSet;
    private float mCost;

  }

  public GestureSet buildWithStrokeLength(int strokeLength) {
    GestureSet library = new GestureSet();
    for (String name : map().keySet()) {
      StrokeSet entry = map().get(name);
      StrokeSet set2 = entry.normalize(strokeLength);
      library.add(set2);
    }
    return library;
  }

  private Map<String, StrokeSet> mEntriesMap = new HashMap();
  private int mStrokeLength;
  private boolean mTrace;

}
