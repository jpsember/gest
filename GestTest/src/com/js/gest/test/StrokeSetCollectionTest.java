package com.js.gest.test;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;

import com.js.basic.Files;
import com.js.gest.GestureSet;
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
    GestureSet c = GestureSet.parseJSON(json);
    assertNotNull(c.get("a"));
    assertNull(c.get("b"));
    assertNotNull(c.get("undo"));
    assertNotNull(c.get("twofingerswipe"));
  }

  public void testJSONBadAliases() throws JSONException {
    String json = readJSON("bad_aliases.txt");

    try {
      GestureSet.parseJSON(json);
      fail();
    } catch (JSONException e) {
      assertTrue(e.getMessage().contains("alias references unknown entry"));
    }
  }

  public void testJSONSource() throws JSONException {
    String json = readJSON("strokes3.txt");
    GestureSet.parseJSON(json);
  }
}
