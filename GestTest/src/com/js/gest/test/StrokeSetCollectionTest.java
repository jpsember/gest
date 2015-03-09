package com.js.gest.test;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;

import com.js.basic.Files;
import com.js.gest.StrokeSetCollection;
import com.js.testUtils.MyTestCase;

public class StrokeSetCollectionTest extends MyTestCase {

	public void testJSON() throws JSONException, IOException {

		InputStream stream = getClass().getResourceAsStream("strokes.txt");
		String json1 = Files.readString(stream);

		StrokeSetCollection c = StrokeSetCollection.parseJSON(json1);
		assertNotNull(c.get("a"));
		assertNull(c.get("b"));
		assertNotNull(c.get("undo"));
		assertNotNull(c.get("twofingerswipe"));
	}

}
