package com.js.gest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONException;

import static com.js.basic.Tools.*;

public class StrokeSetCollection {

  public static StrokeSetCollection parseJSON(String script)
      throws JSONException {

    StrokeSetCollectionParser p = new StrokeSetCollectionParser();
    StrokeSetCollection collection = new StrokeSetCollection();
    p.parse(script, collection);
    return collection;
  }

  /**
   * Get the map containing the entries
   */
  public Map<String, StrokeSet> map() {
    return mEntriesMap;
  }

  /**
   * Get named StrokeSet, if it exists
   */
  public StrokeSet getStrokeSet(String name) {
    return map().get(name);
  }

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

  public Match findMatch(StrokeSet inputSet, MatcherParameters param) {
    return findMatch(inputSet, null, param);
  }

  public Match findMatch(StrokeSet inputSet, List<Match> resultsList,
      MatcherParameters param) {
    if (resultsList != null)
      resultsList.clear();
    TreeSet<Match> results = new TreeSet();
    for (String setName : mEntriesMap.keySet()) {
      StrokeSet set = mEntriesMap.get(setName);
      if (set.size() != inputSet.size())
        continue;
      StrokeSetMatcher m = new StrokeSetMatcher(set, inputSet, param);
      Match match = new Match(set, m.similarity());
      results.add(match);
      // If second entry is an alias of the first, throw it out; there's no need
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
        if (diff != 0) {
          warning("comparisons matched exactly, this is unlikely: "
              + this.strokeSet().name()
              + " / "
              + m.strokeSet().name()
              + "\n and may be indicative of a spelling mistake in the 'source' options");
        }
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

  public StrokeSetCollection buildWithStrokeLength(int strokeLength) {
    StrokeSetCollection library = new StrokeSetCollection();
    for (String name : map().keySet()) {
      StrokeSet entry = map().get(name);
      StrokeSet set2 = entry.normalize(strokeLength);
      library.add(set2);
    }
    return library;
  }

  private Map<String, StrokeSet> mEntriesMap = new HashMap();
  private int mStrokeLength;

}
