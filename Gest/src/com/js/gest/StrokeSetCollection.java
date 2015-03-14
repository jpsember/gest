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
  public Map<String, StrokeSetEntry> map() {
    return mEntriesMap;
  }

  /**
   * Convenience method for map().get()
   */
  StrokeSetEntry getSetEntry(String name) {
    return map().get(name);
  }

  /**
   * Get named StrokeSet, if it exists
   */
  public StrokeSet getStrokeSet(String name) {
    StrokeSetEntry entry = getSetEntry(name);
    if (entry == null)
      return null;
    return entry.strokeSet();
  }

  public int strokeLength() {
    return mStrokeLength;
  }

  public void put(String name, StrokeSet set, String aliasName) {
    if (set == null)
      throw new IllegalArgumentException();

    // If the library is currently empty, set its length to the length of this
    // set's strokes
    if (mEntriesMap.isEmpty()) {
      mStrokeLength = set.length();
    }

    StrokeSetEntry entry = new StrokeSetEntry(name);
    entry.setStrokeSet(set);
    entry.setAliasName(aliasName);
    mEntriesMap.put(name, entry);
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
      StrokeSetEntry entry = mEntriesMap.get(setName);
      StrokeSet set = entry.strokeSet();
      if (set.size() != inputSet.size())
        continue;
      StrokeSetMatcher m = new StrokeSetMatcher(set, inputSet, param);
      Match match = new Match(entry, m.similarity());
      results.add(match);
      // If second entry is an alias of the first, throw it out; there's no need
      // to keep it, since it shouldn't affect a match decision
      if (results.size() >= 2) {
        Iterator<Match> iter = results.iterator();
        Match m1 = iter.next();
        Match m2 = iter.next();
        if (m1.setEntry().aliasName() == m2.setEntry().aliasName()) {
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
    public Match(StrokeSetEntry set, float cost) {
      mStrokeSetEntry = set;
      mCost = cost;
    }

    public float cost() {
      return mCost;
    }

    StrokeSetEntry setEntry() {
      return mStrokeSetEntry;
    }

    /**
     * Get StrokeSet
     */
    public StrokeSet strokeSet() {
      return setEntry().strokeSet();
    }

    @Override
    public int compareTo(Object another) {
      Match m = (Match) another;
      int diff = (int) Math.signum(this.cost() - m.cost());
      if (diff == 0) {
        diff = String.CASE_INSENSITIVE_ORDER.compare(this.setEntry().name(), m
            .setEntry().name());
        if (diff != 0) {
          warning("comparisons matched exactly, this is unlikely: "
              + this.setEntry().name()
              + " / "
              + m.setEntry().name()
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
      sb.append(setEntry().name());
      if (setEntry().hasAlias())
        sb.append(" --> " + setEntry().aliasName());
      return sb.toString();
    }

    private StrokeSetEntry mStrokeSetEntry;
    private float mCost;

  }

  public StrokeSetCollection buildWithStrokeLength(int strokeLength) {
    StrokeSetCollection library = new StrokeSetCollection();
    for (String name : map().keySet()) {
      StrokeSetEntry entry = map().get(name);
      StrokeSet set2 = entry.strokeSet().normalize(strokeLength);
      library.put(name, set2, entry.aliasName());
    }
    return library;
  }

  private Map<String, StrokeSetEntry> mEntriesMap = new HashMap();
  private int mStrokeLength;

}
