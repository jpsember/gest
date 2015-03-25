package com.js.gestApp;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.js.android.MyActivity;

import static com.js.android.UITools.*;
import com.js.gest.MatcherParameters;
import com.js.gest.StrokeSet;
import com.js.gest.GestureSet;
import com.js.gest.GestureSet.Match;
import com.js.gest.GesturePanel;

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
import static com.js.basic.Tools.*;

public class GestActivity extends MyActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // To address issue #6:
    getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    prepareGestureLibrary();
    setContentView(buildContentView());
  }

  private void buildConsole(LinearLayout container) {
    TextView tv = new TextView(this);
    tv.setBackgroundColor(Color.LTGRAY);
    tv.setTypeface(Typeface.MONOSPACE);
    tv.setTextSize(18);
    tv.setPadding(10, 10, 10, 10);
    tv.setHeight(1);
    mConsole = tv;

    container.addView(mConsole, layoutParams(container, 1.5f));
  }

  private View buildPrimaryViews() {
    boolean landscapeFlag = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    LinearLayout mainContainer = linearLayout(this, !landscapeFlag);
    LinearLayout auxContainer = linearLayout(this, landscapeFlag);

    buildConsole(auxContainer);

    mGesturePanel = new GesturePanel(this);
    mGesturePanel.setListener(new GesturePanel.Listener() {
      @Override
      public void processGesture(String gestureName) {
      }

      @Override
      public void processStrokeSet(StrokeSet set) {
        // Have the TouchView display this stroke set
        mTouchView.setDisplayStrokeSet(set);

        if (mAddSamplesCheckBox.isChecked()) {
          mSamples.add(set);
          return;
        }

        // Perform a match operation with this stroke set, and display the
        // results in the console view.
        // Scale the stroke set to fit the standard rectangle
        set = set.fitToRect(null);
        performMatch(set);
      }
    });
    mGesturePanel.setGestures(mGestureLibrary);

    mainContainer.addView(auxContainer, layoutParams(mainContainer, 1));
    mTouchView = new TouchView(this, new TouchView.Listener() {
      @Override
      public void processStrokeSet(StrokeSet set) {
        // When we receive a stroke set from the Touch view, send it to the
        // GesturePanel; our listener above will cause it to be displayed in the
        // TouchView again (after scaling & normalizing), then will attempt a
        // match
        mGesturePanel.setEnteredStrokeSet(set);
      }
    });
    mainContainer.addView(mTouchView, layoutParams(mainContainer, 2f));

    LinearLayout pair = null;
    pair = linearLayout(this, true);
    mainContainer.addView(pair, layoutParams(mainContainer, 1.5f));

    LinearLayout.LayoutParams p = layoutParams(pair, 1f);
    pair.addView(mGesturePanel, p);

    return mainContainer;
  }

  private void setConsoleText(String text) {
    if (mConsole == null)
      return;
    if (text == null)
      text = "";
    mConsole.setText(text);
  }

  private void addButton(LinearLayout container, String label,
      OnClickListener listener) {
    Button b = new Button(this);
    b.setText(label);
    container.addView(b, layoutParams(container, 0));
    if (listener != null)
      b.setOnClickListener(listener);
  }

  private CheckBox addCheckBox(LinearLayout container, String label,
      OnClickListener listener) {
    CheckBox checkBox = new CheckBox(this);
    checkBox.setText(label);
    container.addView(checkBox, layoutParams(container, 0));
    if (listener != null)
      checkBox.setOnClickListener(listener);
    return checkBox;
  }

  private LinearLayout buildControlView() {
    LinearLayout ctrlView = linearLayout(this, false);

    addButton(ctrlView, "Save", new OnClickListener() {
      @Override
      public void onClick(View v) {
        String name = mNameWidget.getText().toString().trim();
        if (name.isEmpty())
          return;
        if (mAddSamplesCheckBox.isChecked()) {
          dumpSamples(name);
          return;
        }

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
    ctrlView.addView(name, layoutParams(ctrlView, 1f));
    mNameWidget = name;

    addButton(ctrlView, "Run", new OnClickListener() {
      @Override
      public void onClick(View v) {
        runSamplesExperiment();
      }
    });
    LinearLayout optionsPanel = linearLayout(this, true);
    ctrlView.addView(optionsPanel, layoutParams(ctrlView, 0));
    mAddSamplesCheckBox = addCheckBox(optionsPanel, "Record",
        new OnClickListener() {
          @Override
          public void onClick(View v) {
            if (mAddSamplesCheckBox.isChecked()) {
              mSamples.clear();
            }
          }
        });
    addButton(optionsPanel, "Pop", new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mSamples.isEmpty())
          return;
        pop(mSamples);
      }
    });
    return ctrlView;
  }

  private String dumpStrokeSet(StrokeSet set) {
    String json = null;
    set = set.normalize(0);
    try {
      json = set.toJSON();
      pr(json);
    } catch (JSONException e) {
      die(e);
    }
    return json;
  }

  private void dumpSamples(String name) {
    for (StrokeSet s : mSamples) {
      s = mutable(s);
      s.setName(name);
      dumpStrokeSet(s);
    }
  }

  private void runSamplesExperiment() {
    unimp();
  }

  private void addGestureToLibrary(StrokeSet set) {
    set.assertFrozen();
    mGestureLibrary.add(set);
  }

  private View buildContentView() {
    LinearLayout contentView = linearLayout(this, true);
    contentView.addView(buildPrimaryViews(), layoutParams(contentView, 1));
    contentView.addView(buildControlView());
    return contentView;
  }

  private void clearRegisteredSet() {
    mNormalizedStrokeSet = null;
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
    return sb.toString();
  }

  private void performMatch(StrokeSet strokeSet) {
    StringBuilder sb = new StringBuilder();
    GestureSet library = mGestureLibrary;
    StrokeSet source = strokeSet;
    source = source.normalize(library.strokeLength());
    // Save this as the normalized stroke set, i.e., for saving
    mNormalizedStrokeSet = source;
    String result = performMatchWithLibrary(source, library);
    sb.append(result);
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
  private TouchView mTouchView;
  // Stroke set after scaling / normalizing
  private StrokeSet mNormalizedStrokeSet;
  private TextView mConsole;
  private EditText mNameWidget;
  private GesturePanel mGesturePanel;
  private CheckBox mAddSamplesCheckBox;
  private List<StrokeSet> mSamples = new ArrayList();
}
