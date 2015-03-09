package com.js.gest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.basic.JSONTools;

class StrokeSetCollectionParser {

	private static final String KEY_NAME = "name";
	private static final String KEY_ALIAS = "alias";
	private static final String KEY_SOURCE = "source";
	private static final String KEY_STROKES = "strokes";

	public void parse(String script, StrokeSetCollection collection)
			throws JSONException {

		mStrokeSetCollection = collection;
		JSONArray array = JSONTools.parseArray(script);
		// Pass 1: read all of the entries into our map
		populateMapFromArray(array);

		// Pass 2: process all entries which contain strokes (instead of
		// references to a source)
		processStrokes();

		// Process all entries which contain references to strokes
		processStrokeReferences();

		processAliases();
	}

	private void populateMapFromArray(JSONArray array) throws JSONException {
		mNamedSets = new HashMap();
		for (int i = 0; i < array.length(); i++) {
			JSONObject map = array.getJSONObject(i);
			String name = getName(map);
			if (mNamedSets.containsKey(name))
				throw new JSONException("Duplicate name: " + name);
			verifyLegality(map);
			ParseEntry parseEntry = new ParseEntry(name, map);
			mNamedSets.put(name, parseEntry);
			mStrokeSetCollection.map().put(name, parseEntry.strokeSetEntry());
		}
	}

	private void verifyLegality(JSONObject map) throws JSONException {
		if (!(map.has(KEY_STROKES) ^ map.has(KEY_SOURCE)))
			throw new JSONException(getName(map) + " must have exactly one of "
					+ KEY_STROKES + " or " + KEY_SOURCE);
	}

	private void processStrokes() throws JSONException {
		for (String name : mNamedSets.keySet()) {
			ParseEntry entry = mNamedSets.get(name);
			JSONArray strokes = entry.map().optJSONArray(KEY_STROKES);
			if (strokes == null)
				continue;
			StrokeSet strokeSet = parseStrokeSet(name, strokes);
			entry.strokeSetEntry().setStrokeSet(strokeSet);
		}
	}

	private void processStrokeReferences() throws JSONException {
		for (String name : mNamedSets.keySet()) {
			ParseEntry parseEntry = mNamedSets.get(name);
			StrokeSetEntry strokeSetEntry = parseEntry.strokeSetEntry();

			JSONArray sourceList = parseEntry.map().optJSONArray(KEY_SOURCE);
			if (sourceList == null)
				continue;

			if (sourceList.length() == 0)
				throw new JSONException("no source name found: " + name);
			String sourceName = sourceList.getString(0);

			ParseEntry sourceEntry = mNamedSets.get(sourceName);
			if (sourceEntry == null)
				throw new JSONException("No set found: " + sourceName);
			StrokeSet sourceSet = sourceEntry.strokeSetEntry().strokeSet();
			if (sourceSet == null)
				throw new JSONException("No strokes found for: " + sourceName);

			Set<String> options = new HashSet();
			for (int i = 1; i < sourceList.length(); i++)
				options.add(sourceList.getString(i));

			StrokeSet strokeSet = modifyExistingStrokeSet(sourceSet, options);
			strokeSetEntry.setStrokeSet(strokeSet);
		}
	}

	private void processAliases() throws JSONException {
		for (String name : mNamedSets.keySet()) {
			ParseEntry entry = mNamedSets.get(name);
			String aliasName = entry.map().optString(KEY_ALIAS);
			if (aliasName.isEmpty())
				continue;
			ParseEntry targetEntry = mNamedSets.get(aliasName);
			if (targetEntry == null || targetEntry.map().has(KEY_ALIAS))
				throw new JSONException("problem with alias: " + name);
			entry.strokeSetEntry().setAlias(targetEntry.strokeSetEntry());
		}
	}

	private static StrokeSet parseStrokeSet(String name, JSONArray array)
			throws JSONException {

		List<Stroke> strokes = new ArrayList();
		if (array.length() == 0)
			throw new JSONException("no strokes defined for " + name);
		for (int i = 0; i < array.length(); i++) {
			JSONArray setEntry = array.getJSONArray(i);
			Stroke stroke = Stroke.parseJSONArray(setEntry);
			strokes.add(stroke);
		}
		StrokeSet set = StrokeSet.buildFromStrokes(strokes);
		set = normalizeStrokeSet(set);
		return set;
	}

	private static StrokeSet normalizeStrokeSet(StrokeSet set) {
		final boolean withSmoothing = true;
		final boolean withNormalizing = true;
		StrokeSet smoothedSet = set;
		if (withSmoothing) {
			StrokeSmoother s = new StrokeSmoother(set);
			set = s.getSmoothedSet();
			smoothedSet = set;
		}
		Rect fitRect = StrokeRegistrator.sStandardRect;
		smoothedSet = StrokeRegistrator.fitToRect(smoothedSet, fitRect);

		StrokeSet normalizedSet = smoothedSet;
		if (withNormalizing) {
			StrokeNormalizer n = new StrokeNormalizer(normalizedSet);
			normalizedSet = n.getNormalizedSet();
		}
		return normalizedSet;
	}

	private StrokeSet modifyExistingStrokeSet(StrokeSet sourceSet,
			Set<String> options) {
		throw new UnsupportedOperationException();
	}

	private static String getName(JSONObject strokeSetMap) throws JSONException {
		return strokeSetMap.getString(KEY_NAME);
	}

	private Map<String, ParseEntry> mNamedSets;
	private StrokeSetCollection mStrokeSetCollection;

	private static class ParseEntry {
		public ParseEntry(String name, JSONObject jsonMap) {
			mEntry = new StrokeSetEntry(name);
			mJSONMap = jsonMap;
		}

		public JSONObject map() {
			return mJSONMap;
		}

		public StrokeSetEntry strokeSetEntry() {
			return mEntry;
		}

		private StrokeSetEntry mEntry;
		private JSONObject mJSONMap;
	}

}
