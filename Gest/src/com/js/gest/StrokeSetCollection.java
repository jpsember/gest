package com.js.gest;

import java.util.HashMap;
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
	public StrokeSetEntry get(String name) {
		return map().get(name);
	}

	public void set(String name, StrokeSet set) {
		warning("have alias refer to name, not actual set");
		if (set == null)
			throw new IllegalArgumentException();
		StrokeSetEntry entry = new StrokeSetEntry(name);
		entry.setStrokeSet(set);
		mEntriesMap.put(name, entry);
	}

	public Match findMatch(StrokeSet inputSet) {

		pr("\nfindMatch");
		TreeSet<Match> results = new TreeSet();
		for (String setName : mEntriesMap.keySet()) {
			StrokeSetEntry entry = mEntriesMap.get(setName);
			StrokeSet set2 = entry.strokeSet();
			if (set2.size() != inputSet.size())
				continue;

			StrokeSetMatcher m = new StrokeSetMatcher(set2, inputSet);
			Match match = new Match(entry, m.similarity());
			results.add(match);
			pr("  match= " + match);
			// Throw out all but top three
			while (results.size() > 3)
				results.pollLast();
		}
		if (results.isEmpty())
			return null;
		return results.first();
	}

	public static class Match implements Comparable {
		public Match(StrokeSetEntry set, float score) {
			mStrokeSetEntry = set;
			mScore = score;
		}

		public float score() {
			return mScore;
		}

		public StrokeSetEntry set() {
			return mStrokeSetEntry;
		}

		@Override
		public int compareTo(Object another) {
			Match m = (Match) another;
			int diff = (int) Math.signum(this.score() - m.score());
			if (diff == 0) {
				diff = String.CASE_INSENSITIVE_ORDER.compare(this.set().name(), m.set()
						.name());
			}
			return diff;
		}

		@Override
		public String toString() {
			return "Match '" + set().name() + "' score=" + Math.round(score());
		}

		private StrokeSetEntry mStrokeSetEntry;
		private float mScore;

	}

	private Map<String, StrokeSetEntry> mEntriesMap = new HashMap();
}
