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
import com.js.gest.StrokeSmoother;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import static com.js.basic.Tools.*;

public class GestActivity extends MyActivity implements TouchView.Listener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// To address issue #6:
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		setContentView(buildContentView());

		prepareGestureLibrary();
	}

	@Override
	public void startTouchSequence() {
		mTouchStrokeSet = new StrokeSet();
		mNormalizedStrokeSet = null;
		mTouchView.setDisplayStrokeSet(null);
	}

	@Override
	public void processTouchSet(StrokeSet set) {
		if (set == null)
			return;

		if (!set.isFrozen())
			throw new IllegalArgumentException();
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

	private View buildUpperViews() {
		LinearLayout horzLayout = UITools.linearLayout(this, false);

		LinearLayout layout2 = UITools.linearLayout(this, true);
		{
			mMatchView = new MatchView(this);
			mMatchView.setBackgroundColor(Color.BLUE);
			LinearLayout.LayoutParams p = UITools.layoutParams(layout2);
			p.width = (int) StrokeSet.sStandardRect.width;
			p.height = (int) StrokeSet.sStandardRect.height;
			p.setMargins(10, 10, 10, 10);
			p.gravity = Gravity.CENTER;
			layout2.addView(mMatchView, p);
			View mAuxView = new UITools.OurBaseView(this);
			mAuxView.setBackgroundColor(Color.GRAY);
			p = UITools.layoutParams(layout2);
			p.weight = 0;
			layout2.addView(mAuxView, p);
		}

		{
			TextView tv = new TextView(this);
			tv.setBackgroundColor(Color.LTGRAY);
			tv.setTypeface(Typeface.MONOSPACE);
			tv.setTextSize(18);
			mConsole = tv;

			LinearLayout.LayoutParams p = UITools.layoutParams(layout2);
			p.setMargins(10, 10, 10, 10);
			p.weight = 1;
			layout2.addView(mConsole, p);
		}

		LinearLayout.LayoutParams p = UITools.layoutParams(horzLayout);
		p.weight = .2f;
		horzLayout.addView(layout2, p);

		mTouchView = new TouchView(this, this);

		mTouchView.setBackgroundColor(Color.BLUE);
		p = UITools.layoutParams(horzLayout);
		p.weight = .7f;
		horzLayout.addView(mTouchView, p);
		return horzLayout;
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
				setConsoleText("saving set as name '" + name + "'");
				mNameWidget.setText("");
				dumpStrokeSet(mNormalizedStrokeSet, name);
				clearRegisteredSet();
			}
		});
		addButton("ZeroDist", new OnClickListener() {
			@Override
			public void onClick(View v) {
				mZeroDistIndex++;
				pr("zero dist threshold now " + d(calcZeroDistValue()));
				processTouchSet(mTouchStrokeSet);
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
				processTouchSet(mTouchStrokeSet);
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
		mNonSquaredErrorsCheckBox = addCheckBox("RootError", new OnClickListener() {
			@Override
			public void onClick(View v) {
				processTouchSet(mTouchStrokeSet);
			}
		});

		addCheckBox("Coarse", new OnClickListener() {
			@Override
			public void onClick(View v) {
				mTouchView.setCoarseFlag(((CheckBox) v).isChecked());
			}
		});
	}

	private void dumpStrokeSet(StrokeSet originalSet, String name) {
		StrokeSet set = originalSet.normalize(12);
		try {
			String s = set.toJSON(name);
			pr("\n" + s);
		} catch (JSONException e) {
			die(e);
		}
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
		p.setSquaredErrorFlag(!mNonSquaredErrorsCheckBox.isChecked());
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

	private void prepareGestureLibrary() {

		try {
			InputStream stream = getClass()
					.getResourceAsStream("basic_gestures.json");
			String json = Files.readString(stream);
			mGestureLibrary = StrokeSetCollection.parseJSON(json);
		} catch (Exception e) {
			die(e);
		}
	}

	private StrokeSetCollection mGestureLibrary;
	private StrokeSetCollection mMultiLengthLibrary;
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
	private CheckBox mNonSquaredErrorsCheckBox;
	private int mZeroDistIndex;
}
