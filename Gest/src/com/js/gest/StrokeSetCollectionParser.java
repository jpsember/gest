package com.js.gest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.js.basic.JSONTools;
import com.js.basic.Point;
import com.js.gest.Stroke.DataPoint;
import com.js.gestApp.StrokeSmoother;

import static com.js.basic.Tools.*;

class StrokeSetCollectionParser {

  /**
   * <pre>
   * 
   * Format of JSON file
   * 
   * gesture_set := [ entry* ]
   * 
   * Each entry is a map.  Each map contains exactly one of NAME or ALIAS,
   * and exactly one of STROKES or USES.
   * 
   * </pre>
   */
  private static final String KEY_NAME = "name";
  private static final String KEY_ALIAS = "alias";
  private static final String KEY_USES = "uses";
  private static final String KEY_STROKES = "strokes";

  public void parse(String script, StrokeSetCollection collection)
      throws JSONException {

    mStrokeSetCollection = collection;
    JSONArray array = JSONTools.parseArray(script);

    // Pass 1: read all of the entries into our map
    populateMapFromArray(array);

    // Pass 2: process all entries which contain actual strokes, instead of
    // referencing others
    processStrokes();

    // Process all entries which contain references to strokes
    processStrokeReferences();

    processAliases();
  }

  private static void quote(StringBuilder sb, String text) {
    sb.append('"');
    sb.append(text);
    sb.append('"');
  }

  public static String strokeSetToJSON(StrokeSet set, String name)
      throws JSONException {
    StringBuilder sb = new StringBuilder(",{");
    quote(sb, KEY_NAME);
    sb.append(':');
    quote(sb, name);
    sb.append(',');
    quote(sb, KEY_STROKES);
    sb.append(":[");
    for (int i = 0; i < set.size(); i++) {
      if (i != 0)
        sb.append(',');
      sb.append(set.get(i).toJSONArray());
    }
    sb.append("]}\n");
    return sb.toString();
  }

  private String generateNameForAlias(JSONObject map) throws JSONException {
    String originalName = map.optString(KEY_ALIAS);
    if (originalName.isEmpty())
      throw new JSONException("Entry has no name and is not an alias:\n" + map);
    String name = "_" + mUniquePrefixIndex + "_" + originalName;
    mUniquePrefixIndex++;
    return name;
  }

  /**
   * Perform preprocessing on a gesture entry, if appropriate
   * 
   * "alias":["name", options,....] =>
   * 
   * "alias":"name", "uses":["name", options, ...]
   * 
   * @throws JSONException
   * 
   */
  private void preprocessEntry(JSONObject map) throws JSONException {
    Object aliasObject = map.opt(KEY_ALIAS);
    if (aliasObject != null) {
      if (aliasObject instanceof JSONArray) {
        JSONArray array = (JSONArray) aliasObject;
        String originalName = array.getString(0);
        map.put(KEY_ALIAS, originalName);
        map.put(KEY_USES, array);
      }
    }
  }

  private void populateMapFromArray(JSONArray array) throws JSONException {

    mNamedSets = new HashMap();

    for (int i = 0; i < array.length(); i++) {
      JSONObject map = array.getJSONObject(i);

      preprocessEntry(map);

      // If it has a NAME entry, use it; otherwise, it must have an alias; take
      // the name of the alias and prepend a unique prefix, and store that as
      // the name
      String name = map.optString(KEY_NAME);
      if (name.isEmpty()) {
        name = generateNameForAlias(map);
      } else {
        if (map.has(KEY_ALIAS))
          throw new JSONException("Entry has both a name and an alias");
      }

      if (mNamedSets.containsKey(name))
        throw new JSONException("Duplicate name: " + name);
      ParseEntry parseEntry = new ParseEntry(name, map);
      mNamedSets.put(name, parseEntry);
      mStrokeSetCollection.map().put(name, parseEntry.strokeSetEntry());
    }
  }

  private void processStrokes() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry entry = mNamedSets.get(name);
      JSONArray strokes = entry.map().optJSONArray(KEY_STROKES);
      if (strokes == null)
        continue;
      StrokeSet strokeSet = parseStrokeSet(name, strokes);
      entry.strokeSetEntry().addStrokeSet(strokeSet);
    }
  }

  private void processStrokeReferences() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry parseEntry = mNamedSets.get(name);
      StrokeSetEntry strokeSetEntry = parseEntry.strokeSetEntry();

      Set<String> options = new HashSet();
      String usesName = null;

      JSONArray usesList = parseEntry.map().optJSONArray(KEY_USES);
      if (usesList == null)
        continue;
      if (usesList.length() == 0)
        throw new JSONException("no uses name found: " + name);
      usesName = usesList.getString(0);
      for (int i = 1; i < usesList.length(); i++)
        options.add(usesList.getString(i));

      ParseEntry usesEntry = mNamedSets.get(usesName);
      if (usesEntry == null)
        throw new JSONException("No set found: " + usesName);
      StrokeSet usesSet = usesEntry.strokeSetEntry().strokeSet();
      if (usesSet == null)
        throw new JSONException("No strokes found for: " + usesName);

      StrokeSet strokeSet = modifyExistingStrokeSet(name, usesSet, options);
      strokeSetEntry.addStrokeSet(strokeSet);
    }
  }

  private void processAliases() throws JSONException {
    for (String name : mNamedSets.keySet()) {
      ParseEntry entry = mNamedSets.get(name);
      String aliasName = entry.map().optString(KEY_ALIAS);
      if (aliasName.isEmpty())
        continue;
      ParseEntry targetEntry = mNamedSets.get(aliasName);
      if (targetEntry == null)
        throw new JSONException("alias references unknown entry: " + name);
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
    smoothedSet = smoothedSet.fitToRect(null);

    StrokeSet normalizedSet = smoothedSet;
    if (withNormalizing) {
      normalizedSet = StrokeNormalizer.normalize(normalizedSet);
    }
    return normalizedSet;
  }

  private static Set<String> sLegalOptions;

  /**
   * Modify an existing stroke set according to some options
   * 
   * Options can include:
   * 
   * 'reverse' : reverse the time sequence of the stroke points
   * 
   * 'fliphorz' : flip around y axis
   * 
   * 'flipvert' : flip around x axis
   * 
   */
  private StrokeSet modifyExistingStrokeSet(String setName, StrokeSet usesSet,
      Set<String> options) {
    if (sLegalOptions == null) {
      sLegalOptions = new HashSet();
      sLegalOptions.add("reverse");
      sLegalOptions.add("fliphorz");
      sLegalOptions.add("flipvert");
    }
    if (!sLegalOptions.containsAll(options))
      throw new IllegalArgumentException("illegal options for " + setName
          + ": " + d(options));

    boolean reverse = options.contains("reverse");
    boolean flipHorz = options.contains("fliphorz");
    boolean flipVert = options.contains("flipvert");

    List<Stroke> modifiedStrokes = new ArrayList();
    List<DataPoint> workList = new ArrayList();
    for (Stroke s : usesSet) {
      Stroke modifiedStroke = new Stroke();
      modifiedStrokes.add(modifiedStroke);
      workList.clear();
      float totalTime = s.totalTime();
      for (DataPoint spt : s) {
        float time = spt.getTime();
        Point pt = new Point(spt.getPoint());
        if (reverse)
          time = totalTime - time;
        if (flipHorz)
          pt.x = StrokeSet.sStandardRect.width - pt.x;
        if (flipVert)
          pt.y = StrokeSet.sStandardRect.height - pt.y;
        workList.add(new DataPoint(time, pt));
      }
      if (reverse)
        Collections.reverse(workList);
      for (DataPoint pt : workList)
        modifiedStroke.addPoint(pt);
    }
    return StrokeSet.buildFromStrokes(modifiedStrokes);
  }

  private Map<String, ParseEntry> mNamedSets;
  private StrokeSetCollection mStrokeSetCollection;
  private int mUniquePrefixIndex;

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
