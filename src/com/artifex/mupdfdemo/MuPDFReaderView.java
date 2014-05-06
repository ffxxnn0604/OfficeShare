package com.artifex.mupdfdemo;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import edu.usc.officeshare.signal.Point;

public class MuPDFReaderView extends ReaderView{
	
	private final static String TAG = "MuPDFReaderView";
	
	enum Mode {Viewing, Selecting, Drawing}
	private final Context mContext;
	private boolean mLinksEnabled = false;
	private Mode mMode = Mode.Viewing;
	private boolean tapDisabled = false;
	private int tapPageMargin;

	protected void onTapMainDocArea() {}
	protected void onDocMotion() {}
	protected void onHit(Hit item) {};
	
	//local arraylist for storing all the points during one draw
	//one draw is defined as pressed the ink button to press the check button,
	//there could be multiple curve lines during one drawing
	private ArrayList<Point> mDrawing;	
	
	public void setLinksEnabled(boolean b) {
		mLinksEnabled = b;
		resetupChildren();
	}

	public void setMode(Mode m) {
		mMode = m;
	}

	public MuPDFReaderView(Activity act) {
		super(act);
		mContext = act;
		// Get the screen size etc to customise tap margins.
		// We calculate the size of 1 inch of the screen for tapping.
		// On some devices the dpi values returned are wrong, so we
		// sanity check it: we first restrict it so that we are never
		// less than 100 pixels (the smallest Android device screen
		// dimension I've seen is 480 pixels or so). Then we check
		// to ensure we are never more than 1/5 of the screen width.
		DisplayMetrics dm = new DisplayMetrics();
		act.getWindowManager().getDefaultDisplay().getMetrics(dm);
		tapPageMargin = (int)dm.xdpi;
		if (tapPageMargin < 100)
			tapPageMargin = 100;
		if (tapPageMargin > dm.widthPixels/5)
			tapPageMargin = dm.widthPixels/5;
		
		mDrawing = new ArrayList<Point>();
		
	}

	public boolean onSingleTapUp(MotionEvent e) {
		LinkInfo link = null;

		if (mMode == Mode.Viewing && !tapDisabled) {
			MuPDFView pageView = (MuPDFView) getDisplayedView();
			Hit item = pageView.passClickEvent(e.getX(), e.getY());
			onHit(item);
			if (item == Hit.Nothing) {
				if (mLinksEnabled && pageView != null
				&& (link = pageView.hitLink(e.getX(), e.getY())) != null) {
					
					link.acceptVisitor(new LinkInfoVisitor() {
						@Override
						public void visitInternal(LinkInfoInternal li) {
							// Clicked on an internal (GoTo) link
							setDisplayedViewIndex(li.pageNumber);
						}

						@Override
						public void visitExternal(LinkInfoExternal li) {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri
									.parse(li.url));
							mContext.startActivity(intent);
						}

						@Override
						public void visitRemote(LinkInfoRemote li) {
							// Clicked on a remote (GoToR) link
						}
					});
				} else if (e.getX() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getX() > super.getWidth() - tapPageMargin) {
					super.smartMoveForwards();
				} else if (e.getY() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getY() > super.getHeight() - tapPageMargin) {
					super.smartMoveForwards();
				} else {
					onTapMainDocArea();
				}
			}
		}
		return super.onSingleTapUp(e);
	}

	@Override
	public boolean onDown(MotionEvent e) {

		return super.onDown(e);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		/*
		MuPDFView pageView = (MuPDFView)getDisplayedView();
		
		Log.i(TAG, "onScroll() method called!");
		
		switch (mMode) {
		case Viewing:
			if (!tapDisabled)
				onDocMotion();

			return super.onScroll(e1, e2, distanceX, distanceY);
		case Selecting:
			if (pageView != null)
				pageView.selectText(e1.getX(), e1.getY(), e2.getX(), e2.getY());
			return true;
		default:
			return true;
		}
		*/
		return true;
	}

	/**
	 * When a real fling triggered by user gesture, we also need to send this action back to MuPDFActivity,
	 * then it will save this action into mChatApplication, and notify the mChatApplication about a
	 * SEND_FLIP_PAGE_EVENT
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.i(TAG, "onFling() method called!");
		
		switch (mMode) {
		case Viewing:
			MuPDFActivity mMuPDFAct = (MuPDFActivity)mContext;
			mMuPDFAct.setNewUserFling(velocityX, velocityY);
			return super.onFling(e1, e2, velocityX, velocityY);			
			
			/*return super.onFling(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(), 0, 200, 200, 1), 
								 MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(), 2, 250, 250, 1), 
								 velocityX, velocityY);*/
		default:
			return true;
		}
	}
	
	//this is called from MuPDFActivity when a HANDLE_RECEIVE_FLIP_PAGE_EVENT msg is received on the mHandler in MuPDFActivity
	public boolean onSimulateFling(MotionEvent dummyE1, MotionEvent dummyE2, float velocityX, float velocityY) {
		Log.i(TAG, "onSimulatedFling() method called!");
		
		switch (mMode) {
		case Viewing:
			
			return super.onFling(dummyE1, dummyE2, velocityX, velocityY);	
			
		default:
			return true;
		}
	}

	public boolean onScaleBegin(ScaleGestureDetector d) {
		// Disabled showing the buttons until next touch.
		// Not sure why this is needed, but without it
		// pinch zoom can make the buttons appear
		tapDisabled = true;
		return super.onScaleBegin(d);
	}

	public boolean onTouchEvent(MotionEvent event) {

		if ( mMode == Mode.Drawing )
		{
			float x = event.getX();
			float y = event.getY();
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					touch_start(x, y);
					break;
				case MotionEvent.ACTION_MOVE:
					touch_move(x, y);
					break;
				case MotionEvent.ACTION_UP:
					touch_up();
					break;
			}
		}

		if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN)
		{
			tapDisabled = false;
		}

		return super.onTouchEvent(event);
	}

	private float mX, mY;

	private static final float TOUCH_TOLERANCE = 2;

	private void touch_start(float x, float y) {

		MuPDFView pageView = (MuPDFView)getDisplayedView();
		if (pageView != null)
		{
			pageView.startDraw(x, y);
		}
		mX = x;
		mY = y;
		
		//a new drawing starts, we clear the arrayList and add the starting point
		//mDrawing.clear();
		float density = getResources().getDisplayMetrics().density;
		mDrawing.add(new Point(Point.START,x/density,y/density));
	}
	
	public void simulated_touch_start(float x, float y) {
		
		MuPDFView pageView = (MuPDFView)getDisplayedView();
		float density = getResources().getDisplayMetrics().density;
		if (pageView != null)
		{
			pageView.startDraw(x*density, y*density);
		}
		
		//we reuse the local variables inside MuPDFReaderView,
		//since only one handler thread of MuPDFActivity exists, we shouldn't need to
		//worry about racing condition on modifying mX and mY.
		mX = x;
		mY = y;
		
		//Also, since this is a simulated aciton, we don't need to record any points into
		//mDrawing, since it wasn't generated locally by the local user.
	}

	private void touch_move(float x, float y) {

		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
		{
			MuPDFView pageView = (MuPDFView)getDisplayedView();
			if (pageView != null)
			{
				pageView.continueDraw(x, y);
			}
			mX = x;
			mY = y;
		}
		
		//during the drawing, we simply add the point to "this" drawing
		float density = getResources().getDisplayMetrics().density;
		mDrawing.add(new Point(Point.NORMAL,x/density,y/density));
		
	}
	
	public void simulated_touh_move (float x, float y) {
		
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		float density = getResources().getDisplayMetrics().density;
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
		{
			MuPDFView pageView = (MuPDFView)getDisplayedView();
			if (pageView != null)
			{
				pageView.continueDraw(x*density, y*density);
			}
			mX = x;
			mY = y;
		}
		
	}

	private void touch_up() {

		// NOOP
		/*Toast toast = Toast.makeText(mContext, "Touch up!", Toast.LENGTH_SHORT);
    	toast.show();*/
    	
	}
	
	public synchronized ArrayList<Point> getDrawing() {
		//TODO return a deep copy (i.e. clone)
		return mDrawing;
	}
	
	public synchronized void clearDrawing() {
		mDrawing.clear();
	}

	protected void onChildSetup(int i, View v) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber == i)
			((MuPDFView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
		else
			((MuPDFView) v).setSearchBoxes(null);

		((MuPDFView) v).setLinkHighlighting(mLinksEnabled);

		((MuPDFView) v).setChangeReporter(new Runnable() {
			public void run() {
				applyToChildren(new ReaderView.ViewMapper() {
					@Override
					void applyToView(View view) {
						((MuPDFView) view).update();
					}
				});
			}
		});
	}

	protected void onMoveToChild(int i) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.set(null);
			resetupChildren();
		}
	}

	@Override
	protected void onMoveOffChild(int i) {
		View v = getView(i);
		if (v != null)
			((MuPDFView)v).deselectAnnotation();
	}

	protected void onSettle(View v) {
		// When the layout has settled ask the page to render
		// in HQ
		((MuPDFView) v).updateHq(false);
	}

	protected void onUnsettle(View v) {
		// When something changes making the previous settled view
		// no longer appropriate, tell the page to remove HQ
		((MuPDFView) v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((MuPDFView) v).releaseResources();
	}

	@Override
	protected void onScaleChild(View v, Float scale) {
		((MuPDFView) v).setScale(scale);
	}
		
}
