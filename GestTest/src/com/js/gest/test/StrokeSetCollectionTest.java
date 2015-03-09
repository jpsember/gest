package com.js.gest.test;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;

import com.js.basic.Files;
import com.js.gest.StrokeSetCollection;
import com.js.gest.StrokeSetEntry;
import com.js.testUtils.MyTestCase;

public class StrokeSetCollectionTest extends MyTestCase {

	private String readJSON(String filename) {
		try {
			InputStream stream = getClass().getResourceAsStream(filename);
			String json = Files.readString(stream);
			return json;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void testJSON() throws JSONException {
		String json = readJSON("strokes.txt");
		StrokeSetCollection c = StrokeSetCollection.parseJSON(json);
		assertNotNull(c.get("a"));
		assertNull(c.get("b"));
		assertNotNull(c.get("undo"));
		assertNotNull(c.get("twofingerswipe"));
	}

	public void testJSONAliases() throws JSONException {
		String json = readJSON("strokes2.txt");

		StrokeSetCollection c = StrokeSetCollection.parseJSON(json);
		assertNotNull(c.get("undo"));
		assertNotNull(c.get("undo2"));

		StrokeSetEntry e1 = c.get("undo");
		StrokeSetEntry e2 = c.get("undo2");
		assertFalse(e1.hasAlias());
		assertTrue(e2.hasAlias());
	}

	public void testJSONBadAliases() throws JSONException {
		String json = readJSON("bad_aliases.txt");

		try {
			StrokeSetCollection.parseJSON(json);
			fail();
		} catch (JSONException e) {
			assertTrue(e.getMessage().contains("problem with alias"));
		}
	}

}
