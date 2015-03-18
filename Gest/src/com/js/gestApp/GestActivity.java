package com.js.gestApp;

import java.util.ArrayList;

import org.json.JSONException;

import com.js.android.MyActivity;
import com.js.android.UITools;
import com.js.gest.MatcherParameters;
import com.js.gest.StrokeSet;
import com.js.gest.GestureSet;
import com.js.gest.GestureSet.Match;
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

  private View buildUpperViews() {
    boolean landscapeFlag;
    landscapeFlag = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    LinearLayout upperView = UITools.linearLayout(this, !landscapeFlag);

    LinearLayout auxContainer = UITools.linearLayout(this, landscapeFlag);
    buildMatchView(auxContainer);
    buildConsole(auxContainer);

    LinearLayout.LayoutParams p = UITools.layoutParams(upperView);
    setStretch(upperView, p, 1f);
    upperView.addView(auxContainer, p);

    mTouchView = new TouchView(this, this);

    p = UITools.layoutParams(upperView);
    setStretch(upperView, p, 2f);
    upperView.addView(mTouchView, p);

    p = UITools.layoutParams(upperView);
    setStretch(upperView, p, 2.5f);
    mExperimentView = new ExperimentView(this);
    upperView.addView(mExperimentView, p);

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

  private CheckBox addCheckBox(LinearLayout container, String label,
      OnClickListener listener) {
    CheckBox checkBox = new CheckBox(this);
    checkBox.setText(label);
    container.addView(checkBox, UITools.layoutParams(container));
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
        StrokeSet set = mutableCopyOf(mNormalizedStrokeSet);
        set.setName(name);
        set.freeze();
        addGestureToLibrary(set);
        String json = dumpStrokeSet(set);
        clearRegisteredSet();
        setConsoleText("Storing:\n\n" + json);
        mNameWidget.setText("");
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

    LinearLayout optionsPanel = UITools.linearLayout(this, true);
    ctrlView.addView(optionsPanel, UITools.layoutParams(ctrlView));

    mSmoothingCheckBox = addCheckBox(optionsPanel, "Smoothing",
        new OnClickListener() {
          @Override
          public void onClick(View v) {
            strokeSetExtended(mTouchStrokeSet);
          }
        });

    mMultiLengthCheckBox = addCheckBox(optionsPanel, "Multilength",
        new OnClickListener() {
          @Override
          public void onClick(View v) {
            if (!((CheckBox) v).isChecked()) {
              mLowResolutionLibrary = null;
            }
          }
        });
  }

  private String dumpStrokeSet(StrokeSet set) {
    String json = null;
    set = set.normalize(12);
    try {
      json = set.toJSON();
      pr("\n" + json);
    } catch (JSONException e) {
      die(e);
    }
    return json;
  }

  // Length of strokes normalized for small version within multilength library
  private static final int SMALL_STROKE_SET_LENGTH = 10;

  private void addGestureToLibrary(StrokeSet set) {
    set.assertFrozen();
    mGestureLibrary.add(set);
    mLowResolutionLibrary = null;
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

  private String performMatchWithLibrary(StrokeSet sourceSet, GestureSet library) {

    library.setTraceStatus(true);

    MatcherParameters p = new MatcherParameters();

    ArrayList<GestureSet.Match> matches = new ArrayList();
    Match match = library.findMatch(sourceSet, p, matches);
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
          // If second best match's cost is
          // substantially more, set good fit prefix
          Match m2 = matches.get(i + 1);
          if (m.cost() * 1.6f < m2.cost())
            goodFit = true;
        }
      }
      sb.append((i == 0 && goodFit) ? "***" : "   ");
      sb.append(m);
      sb.append("\n");
    }
    if (goodFit) {
      mMatchView.setStrokeSet(match.strokeSet());
    } else
      mMatchView.setStrokeSet(null);

    return sb.toString();
  }

  private void performMatch() {
    if (mMultiLengthCheckBox.isChecked()) {
      if (mLowResolutionLibrary == null) {
        mLowResolutionLibrary = mGestureLibrary
            .buildWithStrokeLength(SMALL_STROKE_SET_LENGTH);
      }
    }

    StringBuilder sb = new StringBuilder();
    for (int pass = 0; pass < 2; pass++) {
      if (pass == 1 && !mMultiLengthCheckBox.isChecked())
        continue;
      if (pass == 1)
        sb.append("\n");
      GestureSet library = (pass == 0) ? mGestureLibrary
          : mLowResolutionLibrary;
      sb.append("Length " + library.strokeLength() + ":\n");
      StrokeSet source = mNormalizedStrokeSet;
      source = source.normalize(library.strokeLength());
      String result = performMatchWithLibrary(source, library);
      sb.append(result);
    }
    setConsoleText(sb.toString());
  }

  private void prepareGestureLibrary() {
    try {
      mGestureLibrary = GestureSet.readFromClassResource(getClass(),
          "basic_gestures.json");
    } catch (Exception e) {
      die(e);
    }
  }

  private GestureSet mGestureLibrary;
  private GestureSet mLowResolutionLibrary;
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
  private ExperimentView mExperimentView;
}
