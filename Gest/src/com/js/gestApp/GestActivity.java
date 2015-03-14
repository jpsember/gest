package com.js.gestApp;

import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONException;

import com.js.android.MyActivity;
import com.js.android.UITools;
import com.js.basic.Files;
import com.js.gest.MatcherParameters;
import com.js.gest.StrokeSet;
import com.js.gest.StrokeSetCollection;
import com.js.gest.StrokeSetCollection.Match;
import com.js.gest.StrokeSetEntry;
import com.js.gest.GestureEventFilter;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import static com.js.basic.Tools.*;

public class GestActivity extends MyActivity implements
    GestureEventFilter.Listener {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // To address issue #6:
    getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    prepareGestureLibrary();
    setContentView(buildContentView());
  }

  @Override
  public void strokeSetExtended(StrokeSet set) {
    if (set == null)
      return;
    if (!set.isFrozen())
      return;
    mTouchStrokeSet = set;

    StrokeSet smoothedSet = set;
    if (mSmoothingCheckBox.isChecked()) {
      StrokeSmoother s = new StrokeSmoother(set);
      set = s.getSmoothedSet();
      smoothedSet = set;
    }
    smoothedSet = smoothedSet.fitToRect(null);

    mNormalizedStrokeSet = smoothedSet.normalize();
    StrokeSet displayedSet = mNormalizedStrokeSet.fitToRect(mTouchStrokeSet
        .getBounds());
    mTouchView.setDisplayStrokeSet(displayedSet);

    performMatch();
  }

  @Override
  public void processGesture(String gestureName) {
    pr("recognized gesture " + gestureName);
  }

  /**
   * Set parameters to make a view stretch to fit available space (according to
   * weight) in the appropriate dimension
   */
  private void setStretch(LinearLayout container, LayoutParams p, float weight) {
    if (container.getOrientation() == LinearLayout.HORIZONTAL) {
      p.width = 1;
      p.height = LayoutParams.MATCH_PARENT;
    } else {
      p.width = LayoutParams.MATCH_PARENT;
      p.height = 1;
    }
    p.weight = weight;
  }

  private void buildConsole(LinearLayout container) {
    TextView tv = new TextView(this);
    tv.setBackgroundColor(Color.LTGRAY);
    tv.setTypeface(Typeface.MONOSPACE);
    tv.setTextSize(18);
    tv.setPadding(10, 10, 10, 10);
    tv.setHeight(1);
    mConsole = tv;

    LinearLayout.LayoutParams p = UITools.layoutParams(container);
    setStretch(container, p, 1.5f);
    container.addView(mConsole, p);
  }

  private void buildMatchView(LinearLayout container) {
    mMatchView = new MatchView(this);
    LinearLayout.LayoutParams p = UITools.layoutParams(container);
    setStretch(container, p, 1f);
    container.addView(mMatchView, p);
  }

  private boolean mLandscape;

  private View buildUpperViews() {
    mLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    LinearLayout upperView = UITools.linearLayout(this, !mLandscape);

    LinearLayout auxContainer = UITools.linearLayout(this, mLandscape);
    buildMatchView(auxContainer);
    buildConsole(auxContainer);

    LinearLayout.LayoutParams p = UITools.layoutParams(upperView);
    setStretch(upperView, p, 1f);
    upperView.addView(auxContainer, p);

    mTouchView = new TouchView(this, this);
    mTouchView.setGestureSet(mFilterGestureLibrary);

    p = UITools.layoutParams(upperView);
    setStretch(upperView, p, 2f);
    upperView.addView(mTouchView, p);
    return upperView;
  }

  private void setConsoleText(String text) {
    if (mConsole == null)
      return;
    if (text == null)
      text = "";
    mConsole.setText(text);
  }

  private void addButton(String label, OnClickListener listener) {
    Button b = new Button(this);
    b.setText(label);
    mControlView.addView(b, UITools.layoutParams(mControlView));
    if (listener != null)
      b.setOnClickListener(listener);
  }

  private CheckBox addCheckBox(String label, OnClickListener listener) {
    CheckBox checkBox = new CheckBox(this);
    checkBox.setText(label);
    mControlView.addView(checkBox, UITools.layoutParams(mControlView));
    if (listener != null)
      checkBox.setOnClickListener(listener);
    return checkBox;
  }

  private void buildControlView() {
    LinearLayout ctrlView = UITools.linearLayout(this, false);
    mControlView = ctrlView;

    addButton("Save", new OnClickListener() {
      @Override
      public void onClick(View v) {
        String name = mNameWidget.getText().toString().trim();
        if (name.isEmpty())
          return;
        if (mNormalizedStrokeSet == null)
          return;
        addGestureToLibrary(name, mNormalizedStrokeSet);
        String json = dumpStrokeSet(mNormalizedStrokeSet, name);
        clearRegisteredSet();
        setConsoleText("Storing:\n\n" + json);
        mNameWidget.setText("");
      }
    });
    addButton("ZeroDist", new OnClickListener() {
      @Override
      public void onClick(View v) {
        mZeroDistIndex++;
        pr("zero dist threshold now " + d(calcZeroDistValue()));
        strokeSetExtended(mTouchStrokeSet);
      }
    });

    EditText name = new EditText(this);
    name.setSingleLine();
    name.setTypeface(Typeface.MONOSPACE);
    name.setTextSize(24);
    name.setCursorVisible(false); // Until issue #6 is dealt with
    LinearLayout.LayoutParams p = UITools.layoutParams(ctrlView);
    p.weight = 1;
    ctrlView.addView(name, p);
    mNameWidget = name;

    mSmoothingCheckBox = addCheckBox("Smoothing", new OnClickListener() {
      @Override
      public void onClick(View v) {
        strokeSetExtended(mTouchStrokeSet);
      }
    });

    mMultiLengthCheckBox = addCheckBox("Multilength", new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!((CheckBox) v).isChecked()) {
          mMultiLengthLibrary = null;
        }
      }
    });
  }

  private String dumpStrokeSet(StrokeSet originalSet, String name) {
    String json = null;
    StrokeSet set = originalSet.normalize(12);
    try {
      json = set.toJSON(name);
      pr("\n" + json);
    } catch (JSONException e) {
      die(e);
    }
    return json;
  }

  // Length of strokes normalized for small version within multilength library
  private static final int SMALL_STROKE_SET_LENGTH = 10;

  private void addGestureToLibrary(String name, StrokeSet set) {
    mGestureLibrary.add(name, set);
    if (mMultiLengthLibrary != null) {
      generateMultiLengthSets(name, set, mMultiLengthLibrary);
    }
  }

  private View buildContentView() {
    LinearLayout contentView = UITools.linearLayout(this, true);

    LinearLayout.LayoutParams p = UITools.layoutParams(contentView);
    p.weight = 1.0f;
    contentView.addView(buildUpperViews(), p);

    buildControlView();
    p = UITools.layoutParams(contentView);

    contentView.addView(mControlView, p);
    return contentView;
  }

  private void clearRegisteredSet() {
    mNormalizedStrokeSet = null;
    mMatchView.setStrokeSet(null);
    setConsoleText(null);
  }

  private float calcZeroDistValue() {
    return sZeroDistValues[mZeroDistIndex % sZeroDistValues.length];
  }

  private static final float[] sZeroDistValues = { .01f, .02f, .04f, .06f,
      .08f, .12f, .22f, 0f };

  private String performMatchWithLibrary(StrokeSet sourceSet,
      StrokeSetCollection library) {

    MatcherParameters p = new MatcherParameters();
    p.setZeroDistanceThreshold(calcZeroDistValue() * StrokeSet.STANDARD_WIDTH);

    ArrayList<StrokeSetCollection.Match> matches = new ArrayList();
    Match match = library.findMatch(sourceSet, matches, p);
    if (match == null) {
      return "No match found";
    }

    boolean goodFit = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < matches.size(); i++) {
      Match m = matches.get(i);
      if (i == 0) {
        if (matches.size() < 2)
          goodFit = true;
        else {
          // If second best match is an alias to ours, or its cost is
          // substantially more, set good fit prefix
          Match m2 = matches.get(i + 1);
          if (m2.setEntry().aliasName() == m.setEntry().aliasName()
              || m.cost() * 1.6f < m2.cost())
            goodFit = true;
        }
      }
      sb.append((i == 0 && goodFit) ? "***" : "   ");
      sb.append(m);
      sb.append("\n");
    }
    if (goodFit) {
      StrokeSetEntry ent = match.setEntry();
      mMatchView.setStrokeSet(ent.strokeSet(sourceSet.length()));
    } else
      mMatchView.setStrokeSet(null);

    return sb.toString();
  }

  private void generateMultiLengthSets(String name, StrokeSet originalSet,
      StrokeSetCollection destination) {
    mMultiLengthLibrary.add(name, originalSet);
    // Generate a lower resolution version
    StrokeSet set2 = originalSet.normalize(SMALL_STROKE_SET_LENGTH);
    mMultiLengthLibrary.add(name, set2);
  }

  private void performMatch() {
    if (mMultiLengthCheckBox.isChecked()) {
      if (mMultiLengthLibrary == null) {
        mMultiLengthLibrary = new StrokeSetCollection();
        for (String name : mGestureLibrary.map().keySet()) {
          StrokeSetEntry ent = mGestureLibrary.get(name);
          StrokeSet set = ent.strokeSet();
          generateMultiLengthSets(name, set, mMultiLengthLibrary);
        }
      }
    }

    if (mMultiLengthLibrary == null) {
      String result = performMatchWithLibrary(mNormalizedStrokeSet,
          mGestureLibrary);
      setConsoleText(result);
    } else {
      int[] lengths = { 0, SMALL_STROKE_SET_LENGTH };
      StringBuilder sb = new StringBuilder();
      for (int length : lengths) {
        StrokeSet source = mNormalizedStrokeSet;
        if (source.length() != length)
          source = source.normalize(length);
        String result = performMatchWithLibrary(source, mMultiLengthLibrary);
        sb.append("Length " + source.length() + ":\n");
        sb.append(result);
        sb.append("\n");
      }
      setConsoleText(sb.toString());
    }
  }

  private StrokeSetCollection readGesturesFromFile(String filename) {
    StrokeSetCollection c = null;
    try {
      InputStream stream = getClass().getResourceAsStream(filename);
      String json = Files.readString(stream);
      c = StrokeSetCollection.parseJSON(json);
    } catch (Exception e) {
      die(e);
    }
    return c;
  }

  private void prepareGestureLibrary() {
    mGestureLibrary = readGesturesFromFile("basic_gestures.json");
    mFilterGestureLibrary = readGesturesFromFile("small_gesture_set.json");
  }

  private StrokeSetCollection mGestureLibrary;
  private StrokeSetCollection mMultiLengthLibrary;
  // The gesture set to be used by the filter (for test purposes)
  private StrokeSetCollection mFilterGestureLibrary;
  private TouchView mTouchView;
  // Stroke set from user touch event
  private StrokeSet mTouchStrokeSet;
  // Stroke set after registering / smoothing / normalizing
  private StrokeSet mNormalizedStrokeSet;
  private MatchView mMatchView;
  private TextView mConsole;
  private LinearLayout mControlView;
  private EditText mNameWidget;
  private CheckBox mSmoothingCheckBox;
  private CheckBox mMultiLengthCheckBox;
  private int mZeroDistIndex;

}
