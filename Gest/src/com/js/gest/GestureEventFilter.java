package com.js.gest;

import java.util.ArrayDeque;
import java.util.Queue;

import com.js.android.UITools;
import com.js.basic.Point;
import com.js.basic.Tools;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import static com.js.basic.Tools.*;

public class GestureEventFilter implements View.OnTouchListener {

	// User must move by a certain distance within this time in order to switch
	// from BUFFERING to RECORDING (instead of to FORWARDING)
	private static final int BUFFERING_TIME_MS = 100;
	// Minimum distance finger must move by end of buffering time to be
	// interpreted as a gesture
	private static final float MIN_GESTURE_DISTANCE = 13.0f;

	private static final int STATE_UNATTACHED = 0;
	private static final int STATE_DORMANT = 1;
	private static final int STATE_BUFFERING = 2;
	private static final int STATE_RECORDING = 3;
	private static final int STATE_FORWARDING = 4;
	private static final int STATE_STOPPED = 5;

	public GestureEventFilter() {
		mTraceActive = false;
	}

	public void attachToView(View view, Listener listener) {
		unimp("support de-attaching to prevent memory leaks");
		if (state() != STATE_UNATTACHED)
			throw new IllegalStateException();
		mView = view;
		mView.setOnTouchListener(this);
		mListener = listener;
		setState(STATE_DORMANT);
	}

	private void pr(Object message) {
		if (mTraceActive)
			Tools.pr(message);
	}

	private final static String[] sStateNames = { "UNATTACHED", "DORMANT",
			"BUFFERING", "RECORDING", "FORWARDING", "STOPPED" };

	private static String stateName(int state) {
		return sStateNames[state];
	}

	private int state() {
		return mState;
	}

	private void setState(int s) {
		pr("Set state from " + stateName(mState) + " to " + stateName(s));
		mState = s;
	}

	/**
	 * Push a copy of an event onto our queue for delayed processing
	 * 
	 * @param event
	 * @return the copy that was pushed a copy of the event
	 */
	private MotionEvent bufferEvent(MotionEvent event) {
		MotionEvent eventCopy = MotionEvent.obtain(event);
		mEventQueue.add(eventCopy);
		return eventCopy;
	}

	/**
	 * Send any previously buffered events to the view
	 */
	private void flushBufferedEvents() {
		// Set flag instructing our filter to pass the event through to the view's
		// original handler
		mPassingEventFlag = true;
		if (mEventQueue.size() > 1)
			pr("    flushing " + mEventQueue.size() + " buffered events");
		while (true) {
			MotionEvent event = mEventQueue.poll();
			if (event == null)
				break;
			mView.dispatchTouchEvent(event);
			event.recycle();
		}
		mPassingEventFlag = false;
	}

	/**
	 * Send any previously buffered events to the gesture recording logic
	 */
	private void flushGestureEvents() {
		if (mEventQueue.size() > 1)
			pr("    processing " + mEventQueue.size() + " gesture events");
		while (true) {
			MotionEvent event = mEventQueue.poll();
			if (event == null)
				break;
			processGestureEvent(event);
			event.recycle();
		}
	}

	private void processDormantState(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			// Post an event to switch to FORWARDING automatically in case user
			// doesn't trigger any further events for a while
			postForwardTestEvent();
			setState(STATE_BUFFERING);
			processBufferingState(event);
		} else {
			flushBufferedEvents();
		}
	}

	/**
	 * Post an event to switch to FORWARDING automatically in case user doesn't
	 * trigger any further events for a while, to give the state machine a chance
	 * to make the RECORDING vs FORWARDING decision
	 */
	private void postForwardTestEvent() {
		sHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				pr("Timer task fired, state= " + stateName(state()));
				if (state() == STATE_BUFFERING) {
					setState(STATE_FORWARDING);
					flushBufferedEvents();
				}
			}
		}, (long) (BUFFERING_TIME_MS * 1.5));
	}

	private void processBufferingState(MotionEvent event) {

		MotionEvent startEvent = mEventQueue.peek();
		// Has enough time elapsed since start event to make a decision about
		// whether it's a gesture event?
		long elapsed = 0;
		if (startEvent != null)
			elapsed = event.getEventTime() - startEvent.getEventTime();
		if (elapsed > BUFFERING_TIME_MS) {
			Point pt0 = new Point(startEvent.getRawX(), startEvent.getRawY());
			Point pt = new Point(event.getRawX(), event.getRawY());
			float distance = MyMath.distanceBetween(pt0, pt);
			pr("  ...distance=" + d(distance));
			unimp("use a distance that's proportional to the device density");
			if (distance > MIN_GESTURE_DISTANCE) {
				setState(STATE_RECORDING);
				processRecordingState(event);
			} else {
				setState(STATE_FORWARDING);
				processForwardingState(event);
			}
		} else {
			// pr("  ...not enough elapsed time, continuing to buffer");
			if (event.getActionMasked() == MotionEvent.ACTION_UP) {
				setState(STATE_FORWARDING);
				processForwardingState(event);
			} else {
				bufferEvent(event);
			}
		}
	}

	private void processRecordingState(MotionEvent event) {
		event = bufferEvent(event);
		flushGestureEvents();
		if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			setState(STATE_DORMANT);
		}
	}

	private void processForwardingState(MotionEvent event) {
		event = bufferEvent(event);
		flushBufferedEvents();
		if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			setState(STATE_DORMANT);
		}
	}

	private void processStoppedState(MotionEvent event) {
		bufferEvent(event);
		flushBufferedEvents();
	}

	private boolean onTouchAux(MotionEvent event) {
		switch (state()) {
		case STATE_DORMANT:
			processDormantState(event);
			break;
		case STATE_BUFFERING:
			processBufferingState(event);
			break;
		case STATE_RECORDING:
			processRecordingState(event);
			break;
		case STATE_FORWARDING:
			processForwardingState(event);
			break;
		case STATE_STOPPED:
			processStoppedState(event);
			break;
		}
		return true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getActionMasked() != MotionEvent.ACTION_MOVE)
			pr("onTouch: " + UITools.dump(event) + " state " + stateName(state()));

		// Eclipse is giving warnings if we don't appear to be calling
		// performClick() at some point in this method
		if (sAlwaysFalse)
			v.performClick();

		// If we're forwarding events to the original handler, do so
		if (mPassingEventFlag) {
			return false;
		}

		return onTouchAux(event);
	}

	private void processGestureEvent(MotionEvent event) {
		int actionMasked = event.getActionMasked();
		if (actionMasked == MotionEvent.ACTION_DOWN) {
			mStartEventTimeMillis = event.getEventTime();
			mTouchStrokeSet = new StrokeSet();
		}

		float eventTime = ((event.getEventTime() - mStartEventTimeMillis) / 1000.0f);

		int activeId = event.getPointerId(event.getActionIndex());
		MotionEvent.PointerCoords mCoord = new MotionEvent.PointerCoords();
		for (int i = 0; i < event.getPointerCount(); i++) {
			int ptrId = event.getPointerId(i);
			event.getPointerCoords(i, mCoord);
			Point pt = new Point(mCoord.x, mCoord.y);
			pt.y = mView.getHeight() - mCoord.y;
			mTouchStrokeSet.addPoint(eventTime, ptrId, pt);
		}

		mListener.strokeSetExtended(mTouchStrokeSet);

		if (actionMasked == MotionEvent.ACTION_UP
				|| actionMasked == MotionEvent.ACTION_POINTER_UP) {
			mTouchStrokeSet.stopStroke(activeId);
			if (!mTouchStrokeSet.areStrokesActive()) {
				mTouchStrokeSet.freeze();
				mListener.strokeSetCompleted(mTouchStrokeSet);
			}
		}

	}

	public static interface Listener {
		void strokeSetExtended(StrokeSet strokeSet);

		void strokeSetCompleted(StrokeSet strokeSet);
	}

	// Stroke set from user touch event
	private StrokeSet mTouchStrokeSet;
	private Listener mListener;
	private long mStartEventTimeMillis;

	private static boolean sAlwaysFalse = false;
	private static Handler sHandler = new Handler();
	private View mView;
	private boolean mTraceActive;
	private Queue<MotionEvent> mEventQueue = new ArrayDeque();
	private boolean mPassingEventFlag;
	private int mState;
}
