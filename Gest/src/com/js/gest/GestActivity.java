package com.js.gest;

import com.js.android.MyActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

public class GestActivity extends MyActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = prepareView();
		setContentView(view);
	}

	private View prepareView() {
		View view = new View(this);
		view.setBackgroundColor(Color.BLUE);
		return view;
	}
}
