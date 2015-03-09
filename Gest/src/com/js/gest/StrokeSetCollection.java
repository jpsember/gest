package com.js.gest;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

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

	private Map<String, StrokeSetEntry> mEntriesMap = new HashMap();

}
