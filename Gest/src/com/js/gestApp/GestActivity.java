package com.js.gestApp;

import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONException;

import com.js.android.MyActivity;
import com.js.android.UITools;
import com.js.basic.Files;
import com.js.gest.Rect;
import com.js.gest.StrokeNormalizer;
import com.js.gest.StrokeRegistrator;
import com.js.gest.StrokeSet;
import com.js.gest.StrokeSetCollection;
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
		mRegisteredSet = null;
		mView.setDisplayStrokeSet(null);
	}

	@Override
	public void processTouchSet(StrokeSet set) {

		mTouchStrokeSet = set;

		boolean withSmoothing = true;
		boolean withNormalizing = true;

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
		mRegisteredSet = normalizedSet;
		Rect originalBounds = StrokeRegistrator.bounds(mTouchStrokeSet);
		StrokeSet mDisplayStrokeSet = StrokeRegistrator.fitToRect(mRegisteredSet,
				originalBounds);
		mView.setDisplayStrokeSet(mDisplayStrokeSet);

		performMatch();
	}

	private View buildUpperViews() {
		LinearLayout horzLayout = UITools.linearLayout(this, false);

		LinearLayout layout2 = UITools.linearLayout(this, true);
		{
			mMatchView = new MatchView(this);
			mMatchView.setBackgroundColor(Color.BLUE);
			LinearLayout.LayoutParams p = UITools.layoutParams(layout2);
			p.width = (int) StrokeRegistrator.sStandardRect.width;
			p.height = (int) StrokeRegistrator.sStandardRect.height;
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
			tv.setTypeface(Typeface.MONOSPACE);
			tv.setTextSize(24);
			mConsole = tv;

			LinearLayout.LayoutParams p = UITools.layoutParams(layout2);
			p.setMargins(10, 10, 10, 10);
			p.weight = 1;
			layout2.addView(mConsole, p);
		}

		LinearLayout.LayoutParams p = UITools.layoutParams(horzLayout);
		p.weight = .2f;
		horzLayout.addView(layout2, p);

		mView = new TouchView(this, this);

		mView.setBackgroundColor(Color.BLUE);
		p = UITools.layoutParams(horzLayout);
		p.weight = .7f;
		horzLayout.addView(mView, p);
		return horzLayout;
	}

	private void setConsoleText(String text) {
		if (mConsole == null)
			return;
		if (text == null)
			text = "";
		mConsole.setText(text);
	}

	private void addButton(String label, final Runnable handler) {
		Button b = new Button(this);
		b.setText(label);
		mControlView.addView(b, UITools.layoutParams(mControlView));
		if (handler != null)
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					handler.run();
				}
			});
	}

	private void buildControlView() {
		LinearLayout ctrlView = UITools.linearLayout(this, false);
		mControlView = ctrlView;

		addButton("Save", new Runnable() {
			@Override
			public void run() {
				String name = mNameWidget.getText().toString().trim();
				if (name.isEmpty())
					return;
				if (mRegisteredSet == null)
					return;

				mGestureLibrary.set(name, mRegisteredSet);
				setConsoleText("saving set as name '" + name + "'");
				mNameWidget.setText("");
				dumpStrokeSet(mRegisteredSet, name);
				clearRegisteredSet();
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
	}

	private void dumpStrokeSet(StrokeSet set, String name) {
		StrokeNormalizer n = new StrokeNormalizer(set);
		n.setDesiredStrokeSize(8);
		StrokeSet set2 = n.getNormalizedSet();
		try {
			String s = set2.toJSON(name);
			pr("\n"+s);
		} catch (JSONException e) {
			die(e);
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
		mRegisteredSet = null;
		mMatchView.setStrokeSet(null);
		setConsoleText(null);
	}

	private void performMatch() {
		ArrayList<StrokeSetCollection.Match> matches = new ArrayList();
		StrokeSetCollection.Match match = mGestureLibrary.findMatch(mRegisteredSet,
				matches);
		if (match == null) {
			setConsoleText("No match found");
			return;
		}

		StrokeSetEntry ent = match.setEntry();
		mMatchView.setStrokeSet(ent.strokeSet());
		StringBuilder sb = new StringBuilder();
		for (StrokeSetCollection.Match m : matches) {
			sb.append(m);
			sb.append("\n");
		}
		setConsoleText(sb.toString());
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
	private TouchView mView;
	// Stroke set from user touch event
	private StrokeSet mTouchStrokeSet;
	// Normalized stroke set (suitable for comparison)
	private StrokeSet mRegisteredSet;
	private MatchView mMatchView;
	private TextView mConsole;
	private LinearLayout mControlView;
	private EditText mNameWidget;
}
