/***************************************************************************************
 * This program is free software; you can redistribute it and/or modify it under *
 * the terms of the GNU General Public License as published by the Free Software *
 * Foundation; either version 3 of the License, or (at your option) any later *
 * version. *
 * *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 * *
 * You should have received a copy of the GNU General Public License along with *
 * this program. If not, see <http://www.gnu.org/licenses/>. *
 ****************************************************************************************/

package com.ichi2.anki;

import com.ichi2.anki2.R;

import android.app.Activity;
import android.app.Dialog;
import android.support.v4.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anim.ViewAnimation;
import com.ichi2.async.DeckTask;
import com.ichi2.charts.ChartBuilder;
import com.ichi2.compat.Compat;
import com.ichi2.compat.CompatV11;
import com.ichi2.compat.CompatV3;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Stats;
import com.ichi2.libanki.Utils;
import com.ichi2.themes.StyledDialog;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.themes.Themes;
import com.ichi2.widget.WidgetStatus;
import com.tomgibara.android.veecheck.util.PrefSettings;
import com.zeemote.zc.Controller;
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;
import com.zeemote.zc.ui.android.ControllerAndroidUi;
import com.zeemote.zc.util.JoystickToButtonAdapter;

import java.io.IOException;
import java.util.HashMap;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONException;

public class StudyOptionsFragment extends Fragment implements IButtonListener {

	/**
	 * Available options performed by other activities
	 */
	public static final int PREFERENCES_UPDATE = 0;
	private static final int REQUEST_REVIEW = 1;
	private static final int ADD_NOTE = 2;
	private static final int BROWSE_CARDS = 3;
	private static final int STATISTICS = 4;
	private static final int DECK_OPTIONS = 5;

	/**
	 * Constants for selecting which content view to display
	 */
	private static final int CONTENT_STUDY_OPTIONS = 0;
	private static final int CONTENT_CONGRATS = 1;

	private static final int DIALOG_STATISTIC_TYPE = 0;
	private static final int DIALOG_CRAM = 1;

	private HashMap<Integer, StyledDialog> mDialogs = new HashMap<Integer, StyledDialog>();

	/** Zeemote messages */
	private static final int MSG_ZEEMOTE_BUTTON_A = 0x110;
	private static final int MSG_ZEEMOTE_BUTTON_B = MSG_ZEEMOTE_BUTTON_A + 1;
	private static final int MSG_ZEEMOTE_BUTTON_C = MSG_ZEEMOTE_BUTTON_A + 2;
	private static final int MSG_ZEEMOTE_BUTTON_D = MSG_ZEEMOTE_BUTTON_A + 3;
	private static final int MSG_ZEEMOTE_STICK_UP = MSG_ZEEMOTE_BUTTON_A + 4;
	private static final int MSG_ZEEMOTE_STICK_DOWN = MSG_ZEEMOTE_BUTTON_A + 5;
	private static final int MSG_ZEEMOTE_STICK_LEFT = MSG_ZEEMOTE_BUTTON_A + 6;
	private static final int MSG_ZEEMOTE_STICK_RIGHT = MSG_ZEEMOTE_BUTTON_A + 7;

	/** Broadcast that informs us when the sd card is about to be unmounted */
	private BroadcastReceiver mUnmountReceiver = null;

	/**
	 * Preferences
	 */
	private int mStartedByBigWidget;
	private boolean mSwipeEnabled;
	private int mCurrentContentView;
	boolean mInvertedColors = false;
	String mLocale;
	private boolean mZeemoteEnabled;

	private boolean mDontSaveOnStop = false;

	/** Alerts to inform the user about different situations */
	private StyledProgressDialog mProgressDialog;

	// /*
	// * Cram related
	// */
	// // private StyledDialog mCramTagsDialog;
	// private String allCramTags[];
	// private HashSet<String> activeCramTags;
	// private String cramOrder;
	// private static final String[] cramOrderList = { "type, modified",
	// "created", "random()" };

	/**
	 * UI elements for "Study Options" view
	 */
	private View mStudyOptionsView;
	private Button mButtonStart;
	private ToggleButton mToggleNight;
	private ToggleButton mToggleCram;
	private TextView mTextDeckName;
	private TextView mTextDeckDescription;
	private TextView mTextTodayNew;
	private TextView mTextTodayLrn;
	private TextView mTextTodayRev;
	private TextView mTextNewTotal;
	private TextView mTextTotal;
	private TextView mTextETA;
	private LinearLayout mSmallChart;
	private LinearLayout mDeckCounts;
	private LinearLayout mDeckChart;
	private ImageButton mAddNote;
	private ImageButton mCardBrowser;
	private Button mDeckOptions;
	private Button mStatisticsButton;

	/**
	 * UI elements for "Congrats" view
	 */
	private View mCongratsView;
	private TextView mTextCongratsMessage;
	// private Button mButtonCongratsLearnMore;
	// private Button mButtonCongratsReviewEarly;
	private Button mButtonCongratsOpenOtherDeck;
	private Button mButtonCongratsFinish;

	/**
	 * UI elements for "Cram Tags" view
	 */
	// private ListView mCramTagsListView;
	// private Spinner mSpinnerCramOrder;

	/**
	 * Swipe Detection
	 */
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	/**
	 * Statistics
	 */
	public static int mStatisticType;
	private View mBarsMax;
	private View mGlobalBar;
	private View mGlobalMatBar;
	private double mProgressMature;
	private double mProgressAll;

	/** Used to perform operation in a platform specific way. */
	private Compat mCompat;

	private Collection mCol;

	private boolean mFragmented;

	/**
	 * Zeemote controller
	 */
	protected JoystickToButtonAdapter adapter;
	ControllerAndroidUi controllerUi;

	/**
	 * Callbacks for UI events
	 */
	private View.OnClickListener mButtonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.studyoptions_start:
				openReviewer();
				return;
			case R.id.studyoptions_cram:
				v.setEnabled(false);
				// if (mToggleCram.isChecked()) {
				// mToggleCram.setChecked(!mToggleCram.isChecked());
				// activeCramTags.clear();
				// cramOrder = cramOrderList[0];
				// showDialog(DIALOG_CRAM);
				// } else {
				// onCramStop();
				// resetAndUpdateValuesFromDeck();
				// }
				return;
			case R.id.studyoptions_night:
				if (mInvertedColors != mToggleNight.isChecked()) {
					mInvertedColors = mToggleNight.isChecked();
					savePreferences("invertedColors", mInvertedColors);
				}
				return;
				// case R.id.studyoptions_congrats_learnmore:
				// startLearnMore();
				// return;
				// case R.id.studyoptions_congrats_reviewearly:
				// startEarlyReview();
				// return;
			case R.id.studyoptions_congrats_open_other_deck:
				closeStudyOptions();
				return;
			case R.id.studyoptions_congrats_finish:
				finishCongrats();
				return;
			case R.id.studyoptions_card_browser:
				openCardBrowser();
				return;
			case R.id.studyoptions_statistics:
				 showDialog(DIALOG_STATISTIC_TYPE);
				return;
			case R.id.studyoptions_congrats_message:
				// mStatisticType = 0;
				// openStatistics(0);
				return;
			case R.id.studyoptions_options:
				Intent i = new Intent(getActivity(), DeckOptions.class);
				startActivityForResult(i, DECK_OPTIONS);
				if (UIUtils.getApiLevel() > 4) {
					ActivityTransitionAnimation.slide(getActivity(),
							ActivityTransitionAnimation.FADE);
				}
				return;
			case R.id.studyoptions_add:
				addNote();
				return;
			default:
				return;
			}
		}
	};

	Handler ZeemoteHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_ZEEMOTE_STICK_UP:
				// sendKey(KeyEvent.KEYCODE_DPAD_UP);
				break;
			case MSG_ZEEMOTE_STICK_DOWN:
				// sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
				break;
			case MSG_ZEEMOTE_STICK_LEFT:
				// sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
				break;
			case MSG_ZEEMOTE_STICK_RIGHT:
				// sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
				break;
			case MSG_ZEEMOTE_BUTTON_A:
				// sendKey(KeyEvent.KEYCODE_ENTER);
				openReviewer();
				break;
			case MSG_ZEEMOTE_BUTTON_B:
				// sendKey(KeyEvent.KEYCODE_BACK);
				if (mCurrentContentView == CONTENT_CONGRATS) {
					finishCongrats();
				} else {
					closeStudyOptions();
				}
				break;
			case MSG_ZEEMOTE_BUTTON_C:
				sendKey(KeyEvent.KEYCODE_BACK);
				break;
			case MSG_ZEEMOTE_BUTTON_D:
				break;
			}
			super.handleMessage(msg);
		}
	};

	protected void sendKey(int keycode) {
//		this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keycode));
//		this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keycode));
		Log.d("Zeemote", "dispatched key " + keycode);
	}

    public static StudyOptionsFragment newInstance(int index) {
    	StudyOptionsFragment f = new StudyOptionsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);

        return f;
    }

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // Currently in a layout without a container, so no
            // reason to create our view.
            return null;
        }

//        ScrollView scroller = new ScrollView(getActivity());
//        TextView text = new TextView(getActivity());
//        int padding = (int)TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP,
//                4, getActivity().getResources().getDisplayMetrics());
//        text.setPadding(padding, padding, padding, padding);
//        scroller.addView();
        return createView(inflater, savedInstanceState);
    }

	protected View createView(LayoutInflater inflater, Bundle savedInstanceState) {
//		Themes.applyTheme(this);
		super.onCreate(savedInstanceState);

		Log.i(AnkiDroidApp.TAG, "StudyOptions - OnCreate()");

		restorePreferences();

//		registerExternalStorageListener();

		mCol = Collection.currentCollection();
		if (mCol == null) {
			reloadCollection(savedInstanceState);
			return null;
		}

		Intent intent = getActivity().getIntent();
		if (intent != null && intent.hasExtra(DeckPicker.EXTRA_DECK_ID)) {
			mCol.getDecks().select(intent.getLongExtra(DeckPicker.EXTRA_DECK_ID, 1));
		}

		// activeCramTags = new HashSet<String>();
        mFragmented = getActivity().getClass() != StudyOptionsActivity.class;

		initAllContentViews(inflater);

		if (mSwipeEnabled) {			
			gestureDetector = new GestureDetector(new MyGestureDetector());
			gestureListener = new View.OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					if (gestureDetector.onTouchEvent(event)) {
						return true;
					}
					return false;
				}
			};
		}

		if (UIUtils.getApiLevel() >= 11) {
			mCompat = new CompatV11();
		} else {
			mCompat = new CompatV3();
		}

		View view = showContentView(CONTENT_STUDY_OPTIONS);

		// Zeemote controller initialization
		if (mZeemoteEnabled) {
			if (AnkiDroidApp.zeemoteController() == null) {
				AnkiDroidApp.setZeemoteController(new Controller(
						Controller.CONTROLLER_1));
			}
//			controllerUi = new ControllerAndroidUi(this,
//					AnkiDroidApp.zeemoteController());
			com.zeemote.util.Strings zstrings = com.zeemote.util.Strings
					.getStrings();
			if (zstrings.isLocaleAvailable(mLocale)) {
				Log.d("Zeemote", "Zeemote locale " + mLocale
						+ " is available. Setting.");
				zstrings.setLocale(mLocale);
			} else {
				Log.d("Zeemote", "Zeemote locale " + mLocale + " is not available.");
			}
			if (mZeemoteEnabled) {
				if (!AnkiDroidApp.zeemoteController().isConnected()) {
					Log.d("Zeemote", "starting connection in onCreate");
					controllerUi.startConnectionProcess();
				}
			}			
		}
		return view;
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(AnkiDroidApp.TAG, "onConfigurationChanged");
    	mDontSaveOnStop = true;
    	CharSequence title = mTextDeckName.getText();
    	CharSequence desc = mTextDeckDescription.getText();
    	int descVisibility = mTextDeckDescription.getVisibility();
    	CharSequence newToday = mTextTodayNew.getText();
    	CharSequence lrnToday = mTextTodayLrn.getText();
		CharSequence revToday = mTextTodayRev.getText();
		CharSequence newTotal = mTextNewTotal.getText();
		CharSequence total = mTextTotal.getText();
		CharSequence eta = mTextETA.getText();
        super.onConfigurationChanged(newConfig);
        mDontSaveOnStop = false;
//		initAllContentViews();
		showContentView(mCurrentContentView, false);
		mTextDeckName.setText(title);
		mTextDeckName.setVisibility(View.VISIBLE);
		mTextDeckDescription.setText(desc);
		mTextDeckDescription.setVisibility(descVisibility);
		mDeckCounts.setVisibility(View.VISIBLE);
		mTextTodayNew.setText(newToday);
		mTextTodayLrn.setText(lrnToday);
		mTextTodayRev.setText(revToday);
		mTextNewTotal.setText(newTotal);
		mTextTotal.setText(total);
		mTextETA.setText(eta);
		updateStatisticBars();
    }

//	/**
//	 * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
//	 * intent will call closeExternalStorageFiles() if the external media is
//	 * going to be ejected, so applications can clean up any files they have
//	 * open.
//	 */
//	public void registerExternalStorageListener() {
//		if (mUnmountReceiver == null) {
//			mUnmountReceiver = new BroadcastReceiver() {
//				@Override
//				public void onReceive(Context context, Intent intent) {
//					closeStudyOptions(DeckPicker.RESULT_MEDIA_EJECTED);
//				}
//			};
//			IntentFilter iFilter = new IntentFilter();
//			iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
//
//			// ACTION_MEDIA_EJECT is never invoked (probably due to an android bug
////			iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
//			iFilter.addDataScheme("file");
//			registerReceiver(mUnmountReceiver, iFilter);
//		}
//	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(AnkiDroidApp.TAG, "StudyOptions - onDestroy()");
//		if (mUnmountReceiver != null) {
//			unregisterReceiver(mUnmountReceiver);
//		}
		// Disconnect Zeemote if connected
		if (mZeemoteEnabled && (AnkiDroidApp.zeemoteController() != null)
				&& (AnkiDroidApp.zeemoteController().isConnected())) {
			try {
				Log.d("Zeemote", "trying to disconnect in onDestroy...");
				AnkiDroidApp.zeemoteController().disconnect();
			} catch (IOException ex) {
				Log.e("Zeemote",
						"Error on zeemote disconnection in onDestroy: "
								+ ex.getMessage());
			}
		}
	}

	@Override
	public void onPause() {
		if (mZeemoteEnabled && (AnkiDroidApp.zeemoteController() != null)
				&& (AnkiDroidApp.zeemoteController().isConnected())) {
			Log.d(AnkiDroidApp.TAG, "Zeemote: Removing listener in onPause");
			AnkiDroidApp.zeemoteController().removeButtonListener(this);
			AnkiDroidApp.zeemoteController().removeJoystickListener(adapter);
			adapter.removeButtonListener(this);
			adapter = null;
		}
		super.onPause();
	}



	@Override
	public void onResume() {
		super.onResume();
		if (mZeemoteEnabled && (AnkiDroidApp.zeemoteController() != null)
				&& (AnkiDroidApp.zeemoteController().isConnected())) {
			Log.d("Zeemote", "Adding listener in onResume");
			AnkiDroidApp.zeemoteController().addButtonListener(this);
			adapter = new JoystickToButtonAdapter();
			AnkiDroidApp.zeemoteController().addJoystickListener(adapter);
			adapter.addButtonListener(this);
		}
		if (mCol != null) {
			if (Utils.now() > mCol.getSched().getDayCutoff()) {
				updateValuesFromDeck(true);
			}
		}
	}

	private void closeStudyOptions() {
		closeStudyOptions(getActivity().RESULT_OK);
	}

	private void closeStudyOptions(int result) {
//		mCompat.invalidateOptionsMenu(this);
//		setResult(result);
//		finish();
//		if (UIUtils.getApiLevel() > 4) {
//			ActivityTransitionAnimation.slide(getActivity(),
//					ActivityTransitionAnimation.RIGHT);
//		}
	}

	private void openReviewer() {
		mDontSaveOnStop = true;
		Intent reviewer = new Intent(getActivity(), Reviewer.class);
		startActivityForResult(reviewer, REQUEST_REVIEW);
		if (UIUtils.getApiLevel() > 4) {
			ActivityTransitionAnimation.slide(getActivity(),
					ActivityTransitionAnimation.LEFT);
		}
	}

	private void addNote() {
		Intent intent = new Intent(getActivity(), CardEditor.class);
		intent.putExtra(CardEditor.EXTRA_CALLER, CardEditor.CALLER_STUDYOPTIONS);
		startActivityForResult(intent, ADD_NOTE);
		if (UIUtils.getApiLevel() > 4) {
			ActivityTransitionAnimation.slide(getActivity(),
					ActivityTransitionAnimation.LEFT);
		}
	}

	// private void startEarlyReview() {
	// // Deck deck = DeckManager.getMainDeck();
	// // if (deck != null) {
	// // mInReviewer = true;
	// // deck.setupReviewEarlyScheduler();
	// // deck.reset();
	// // Intent reviewer = new Intent(getActivity(), Reviewer.class);
	// // reviewer.putExtra("deckFilename", mDeckFilename);
	// // startActivityForResult(reviewer, REQUEST_REVIEW);
	// // if (UIUtils.getApiLevel() > 4) {
	// // ActivityTransitionAnimation.slide(getActivity(),
	// // ActivityTransitionAnimation.LEFT);
	// // }
	// // }
	// }
	//
	// private void startLearnMore() {
	// // Deck deck = DeckManager.getMainDeck();
	// // if (deck != null) {
	// // mInReviewer = true;
	// // deck.setupLearnMoreScheduler();
	// // deck.reset();
	// // Intent reviewer = new Intent(getActivity(), Reviewer.class);
	// // reviewer.putExtra("deckFilename", mDeckFilename);
	// // startActivityForResult(reviewer, REQUEST_REVIEW);
	// // if (UIUtils.getApiLevel() > 4) {
	// // ActivityTransitionAnimation.slide(getActivity(),
	// // ActivityTransitionAnimation.LEFT);
	// // }
	// // }
	// }

	private void reloadCollection(final Bundle savedInstanceState) {
		DeckTask.launchDeckTask(DeckTask.TASK_TYPE_OPEN_COLLECTION, new DeckTask.TaskListener() {

			@Override
			public void onPostExecute(DeckTask.TaskData result) {
				if (mProgressDialog.isShowing()) {
	                try {
	                    mProgressDialog.dismiss();
	                } catch (Exception e) {
	                    Log.e(AnkiDroidApp.TAG, "onPostExecute - Dialog dismiss Exception = " + e.getMessage());
	                }
	            }
				mCol = result.getCollection();
				Collection.putCurrentCollection(mCol);
				if (mCol == null) {
//					finish();
				} else {
					onCreate(savedInstanceState);
				}
			}

			@Override
			public void onPreExecute() {
	            mProgressDialog = StyledProgressDialog.show(getActivity(), "", getResources().getString(R.string.open_collection), true, true, new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
//						finish();
					}
				});
			}

			@Override
			public void onProgressUpdate(DeckTask.TaskData... values) {
			}
	    }, new DeckTask.TaskData(PrefSettings.getSharedPrefs(getActivity().getBaseContext()).getString("deckPath", AnkiDroidApp.getDefaultAnkiDroidDirectory()) + AnkiDroidApp.COLLECTION_PATH));
	}

	private void initAllContentViews(LayoutInflater inflater) {
		mStudyOptionsView = inflater.inflate(R.layout.studyoptions,
				null);
		Themes.setContentStyle(mStudyOptionsView, Themes.CALLER_STUDYOPTIONS);
		mTextDeckName = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_deck_name);
		mTextDeckDescription = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_deck_description);
		mButtonStart = (Button) mStudyOptionsView
				.findViewById(R.id.studyoptions_start);
//		mToggleCram = (ToggleButton) mStudyOptionsView
//				.findViewById(R.id.studyoptions_cram);
//		mToggleNight = (ToggleButton) mStudyOptionsView
//				.findViewById(R.id.studyoptions_night);
//		mToggleNight.setChecked(mInvertedColors);

//		mAddNote = (ImageButton) mStudyOptionsView
//				.findViewById(R.id.studyoptions_add);
//		mCardBrowser = (ImageButton) mStudyOptionsView
//				.findViewById(R.id.studyoptions_card_browser);
		mStatisticsButton = (Button) mStudyOptionsView
				.findViewById(R.id.studyoptions_statistics);
		mDeckOptions = (Button) mStudyOptionsView
				.findViewById(R.id.studyoptions_options);

		mGlobalBar = (View) mStudyOptionsView
				.findViewById(R.id.studyoptions_global_bar);
		mGlobalMatBar = (View) mStudyOptionsView
				.findViewById(R.id.studyoptions_global_mat_bar);
		mBarsMax = (View) mStudyOptionsView
				.findViewById(R.id.studyoptions_progressbar_content);
		mTextTodayNew = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_new);
		mTextTodayLrn = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_lrn);
		mTextTodayRev = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_rev);
		mTextNewTotal = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_total_new);
		mTextTotal = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_total);
		mTextETA = (TextView) mStudyOptionsView
				.findViewById(R.id.studyoptions_eta);
		mSmallChart = (LinearLayout) mStudyOptionsView
				.findViewById(R.id.studyoptions_mall_chart);

		mGlobalMatBar.setVisibility(View.INVISIBLE);
		mGlobalBar.setVisibility(View.INVISIBLE);

		mDeckCounts = (LinearLayout) mStudyOptionsView.findViewById(R.id.studyoptions_deckcounts);
		mDeckChart = (LinearLayout) mStudyOptionsView.findViewById(R.id.studyoptions_chart);
		
		mButtonStart.setOnClickListener(mButtonClickListener);
//		mToggleCram.setOnClickListener(mButtonClickListener);
//		mToggleNight.setOnClickListener(mButtonClickListener);
//		mAddNote.setOnClickListener(mButtonClickListener);
//		mCardBrowser.setOnClickListener(mButtonClickListener);
		mStatisticsButton.setOnClickListener(mButtonClickListener);
		mDeckOptions.setOnClickListener(mButtonClickListener);

		// The view that shows the congratulations view.
		mCongratsView = inflater.inflate(
				R.layout.studyoptions_congrats, null);

		Themes.setWallpaper(mCongratsView);

		mTextCongratsMessage = (TextView) mCongratsView
				.findViewById(R.id.studyoptions_congrats_message);
		Themes.setTextViewStyle(mTextCongratsMessage);

		mTextCongratsMessage.setOnClickListener(mButtonClickListener);
		// mButtonCongratsLearnMore = (Button) mCongratsView
		// .findViewById(R.id.studyoptions_congrats_learnmore);
		// mButtonCongratsReviewEarly = (Button) mCongratsView
		// .findViewById(R.id.studyoptions_congrats_reviewearly);
		mButtonCongratsOpenOtherDeck = (Button) mCongratsView
				.findViewById(R.id.studyoptions_congrats_open_other_deck);
		mButtonCongratsFinish = (Button) mCongratsView
				.findViewById(R.id.studyoptions_congrats_finish);

		// mButtonCongratsLearnMore.setOnClickListener(mButtonClickListener);
		// mButtonCongratsReviewEarly.setOnClickListener(mButtonClickListener);
		mButtonCongratsOpenOtherDeck.setOnClickListener(mButtonClickListener);
		mButtonCongratsFinish.setOnClickListener(mButtonClickListener);
	}

	private void showDialog(int id) {
		if (!mDialogs.containsKey(id)) {
			mDialogs.put(id, onCreateDialog(id));
		}
		onPrepareDialog(id, mDialogs.get(id));
		mDialogs.get(id).show();
	}

	protected StyledDialog onCreateDialog(int id) {
		StyledDialog dialog = null;
		Resources res = getResources();
		StyledDialog.Builder builder = new StyledDialog.Builder(getActivity());

		switch (id) {

		case DIALOG_STATISTIC_TYPE:
			dialog = ChartBuilder.getStatisticsDialog(getActivity(), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_STATISTICS, mLoadStatisticsHandler, new DeckTask.TaskData(mCol, which, false));
				}
				});
			break;

		case DIALOG_CRAM:
			// builder.setTitle(R.string.studyoptions_cram_dialog_title);
			// builder.setPositiveButton(res.getString(R.string.begin_cram),
			// new OnClickListener() {
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// mToggleCram.setChecked(true);
			// onCram();
			// }
			// });
			// builder.setNegativeButton(res.getString(R.string.cancel), null);
			//
			// Spinner spinner = new Spinner(this);
			//
			// ArrayAdapter<CharSequence> adapter = ArrayAdapter
			// .createFromResource(this, R.array.cram_review_order_labels,
			// android.R.layout.simple_spinner_item);
			// adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// spinner.setAdapter(adapter);
			// spinner.setSelection(0);
			// spinner.setOnItemSelectedListener(new
			// AdapterView.OnItemSelectedListener() {
			// @Override
			// public void onItemSelected(AdapterView<?> parent, View view,
			// int position, long id) {
			// cramOrder = cramOrderList[position];
			// }
			//
			// @Override
			// public void onNothingSelected(AdapterView<?> arg0) {
			// return;
			// }
			// });
			//
			// builder.setView(spinner, false, true);
			// dialog = builder.create();
			// break;

		default:
			dialog = null;
		}

		dialog.setOwnerActivity(getActivity());
		return dialog;
	}


	protected void onPrepareDialog(int id, StyledDialog dialog) {
		StyledDialog ad = (StyledDialog) dialog;

		// wait for deck loading thread (to avoid problems with resuming
		// destroyed activities)
		if (mCol == null) {
			return;
		}

		switch (id) {

		case DIALOG_CRAM:
			// allCramTags = DeckManager.getMainDeck().allTags_();
			// if (allCramTags == null) {
			// Themes.showThemedToast(getActivity(),
			// getResources().getString(R.string.error_insufficient_memory),
			// false);
			// ad.setEnabled(false);
			// return;
			// }
			// ad.setMultiChoiceItems(allCramTags, new
			// boolean[allCramTags.length],
			// new DialogInterface.OnClickListener() {
			// @Override
			// public void onClick(DialogInterface arg0, int which) {
			// String tag = allCramTags[which];
			// if (activeCramTags.contains(tag)) {
			// Log.i(AnkiDroidApp.TAG, "unchecked tag: " + tag);
			// activeCramTags.remove(tag);
			// } else {
			// Log.i(AnkiDroidApp.TAG, "checked tag: " + tag);
			// activeCramTags.add(tag);
			// }
			// }
			// });
			break;
		}
	}

	private View showContentView(int which) {
		return showContentView(which, true);
	}
	private View showContentView(int which, boolean reload) {
		mCurrentContentView = which;

		switch (mCurrentContentView) {

		case CONTENT_STUDY_OPTIONS:
			// TODO: update togglebuttons
			// Enable timeboxing in case it was disabled from the previous deck
			// if ((DeckManager.getMainDeck() != null) &&
			// (DeckManager.getMainDeck().name().equals("cram"))) {
			// mToggleCram.setChecked(false);
			// mToggleLimit.setEnabled(true);
			// }
//			setContentView(mStudyOptionsView);
			if (reload) {
				resetAndUpdateValuesFromDeck();				
			}
			return mStudyOptionsView;

		case CONTENT_CONGRATS:
			// TODO: mTextCongratsMessage.setText(getCongratsMessage(this)
			// Resources res = getResources();
			// Deck deck = AnkiDroidApp.deck();
			// if (deck != null) {
			// int newCards = deck.getSched().newTomorrow();
			// int revCards = deck.getSched().revTomorrow() +
			// deck.getSched().lrnTomorrow();
			// int eta = 0; // TODO
			// String newCardsText =
			// res.getQuantityString(R.plurals.studyoptions_congrats_new_cards,
			// newCards, newCards);
			// String etaText =
			// res.getQuantityString(R.plurals.studyoptions_congrats_eta, eta,
			// eta);
			// }
		 	mTextCongratsMessage.setText(mCol.getSched().finishedMsg(getActivity()));
			if (reload) {
				updateValuesFromDeck();
			}
			return mCongratsView;
		}
		return null;
	}

	private void resetAndUpdateValuesFromDeck() {
		updateValuesFromDeck(true);
	}

	private void updateValuesFromDeck() {
		updateValuesFromDeck(false);
	}
	private void updateValuesFromDeck(boolean reset) {
		String fullName;
		try {
			fullName = mCol.getDecks().current().getString("name");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		String[] name = fullName.split("::");
		StringBuilder nameBuilder = new StringBuilder();
		if (name.length > 0) {
			nameBuilder.append(name[0]);
		}
		if (name.length > 1) {
			nameBuilder.append("\n").append(name[1]);
		}
		if (name.length > 3) {
			nameBuilder.append("...");
		}
		if (name.length > 2) {
			nameBuilder.append("\n").append(name[name.length - 1]);
		}
		mTextDeckName.setText(nameBuilder.toString());

		if (!mFragmented) {
			getActivity().setTitle(fullName);			
		}

		String desc = mCol.getDecks().getActualDescription();
		if (desc.length() > 0) {
			mTextDeckDescription.setText(desc);
			mTextDeckDescription.setVisibility(View.VISIBLE);
		} else {
			mTextDeckDescription.setVisibility(View.GONE);
		}

	 	DeckTask.launchDeckTask(DeckTask.TASK_TYPE_UPDATE_VALUES_FROM_DECK, mUpdateValuesFromDeckListener, new DeckTask.TaskData(mCol.getSched(), reset));
	}

	private void updateStatisticBars() {
		mBarsMax.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				mBarsMax.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				int maxWidth = mBarsMax.getWidth() + 1;
				int height = mBarsMax.getHeight();
				int mat = (int) (mProgressMature * maxWidth);
				Utils.updateProgressBars(mGlobalMatBar, mat, height);
				Utils.updateProgressBars(mGlobalBar, (int)(mProgressAll * maxWidth) - mat, height);
				if (mGlobalMatBar.getVisibility() == View.INVISIBLE) {
					mGlobalMatBar.setVisibility(View.VISIBLE);
					mGlobalMatBar.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_IN, 100, 0));
					mGlobalBar.setVisibility(View.VISIBLE);
					mGlobalBar.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_IN, 100, 0));					
				}
			}
		});
	}

	private void updateChart(double[][] serieslist) {
		if (mSmallChart != null) {
			Resources res = getResources();
		    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		    XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(res.getColor(R.color.stats_young));
            renderer.addSeriesRenderer(r);
		    r = new XYSeriesRenderer();
            r.setColor(res.getColor(R.color.stats_mature));
            renderer.addSeriesRenderer(r);

			for (int i = 1; i < serieslist.length; i++) {
				XYSeries series = new XYSeries("");
	        	for (int j = 0; j < serieslist[i].length; j++) {
	        		series.add(serieslist[0][j], serieslist[i][j]);
	        	}
	        	dataset.addSeries(series);				
			}
			renderer.setBarSpacing(0.4);
			renderer.setShowLegend(false);
			renderer.setLabelsTextSize(13);
			renderer.setXAxisMin(-0.5);
			renderer.setXAxisMax(7.5);
			renderer.setYAxisMin(0);
			renderer.setGridColor(Color.LTGRAY);
			renderer.setShowGrid(true);
			renderer.setBackgroundColor(Color.WHITE);
            renderer.setMarginsColor(Color.WHITE);
			renderer.setAxesColor(Color.BLACK);
			renderer.setLabelsColor(Color.BLACK);
			renderer.setYLabelsColor(0, Color.BLACK);
			renderer.setYLabelsAngle(-90);
			renderer.setXLabelsColor(Color.BLACK);
			renderer.setXLabelsAlign(Align.CENTER);
			renderer.setYLabelsAlign(Align.CENTER);
			renderer.setZoomEnabled(false, false);
//            mRenderer.setMargins(new int[] { 15, 48, 30, 10 });
			renderer.setAntialiasing(true);
			renderer.setPanEnabled(true, false);
			GraphicalView chartView = ChartFactory.getBarChartView(getActivity(), dataset, renderer, BarChart.Type.STACKED);
            mSmallChart.addView(chartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			if(mDeckChart.getVisibility() == View.INVISIBLE) {
				mDeckChart.setVisibility(View.VISIBLE);
				mDeckChart.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_IN, 500, 0));
			}
		}
	}


	private void finishCongrats() {
		mStudyOptionsView.setVisibility(View.INVISIBLE);
		mCongratsView.setVisibility(View.INVISIBLE);
		mCongratsView.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_OUT,
				500, 0));
		showContentView(CONTENT_STUDY_OPTIONS);
		mStudyOptionsView.setVisibility(View.VISIBLE);
		mStudyOptionsView.setAnimation(ViewAnimation.fade(
				ViewAnimation.FADE_IN, 500, 0));
		mCongratsView.setVisibility(View.VISIBLE);
	}

	private void openCardBrowser() {
		mDontSaveOnStop = true;
		Intent cardBrowser = new Intent(getActivity(), CardBrowser.class);
		startActivityForResult(cardBrowser, BROWSE_CARDS);
		if (UIUtils.getApiLevel() > 4) {
			ActivityTransitionAnimation.slide(getActivity(),
					ActivityTransitionAnimation.LEFT);
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		Log.i(AnkiDroidApp.TAG, "StudyOptions: onActivityResult");

		if (resultCode == DeckPicker.RESULT_DB_ERROR) {
			closeStudyOptions(DeckPicker.RESULT_DB_ERROR);
        }

		if (resultCode == AnkiDroidApp.RESULT_TO_HOME) {
			closeStudyOptions();
			return;
		}

		// TODO: proper integration of big widget
		if (resultCode == DeckPicker.RESULT_MEDIA_EJECTED) {
			closeStudyOptions(DeckPicker.RESULT_MEDIA_EJECTED);
		} else if (requestCode == DECK_OPTIONS) {
			resetAndUpdateValuesFromDeck();
		} else if (requestCode == ADD_NOTE && resultCode != getActivity().RESULT_CANCELED) {
			resetAndUpdateValuesFromDeck();
		} else if (requestCode == PREFERENCES_UPDATE) {
			restorePreferences();
		} else if (requestCode == REQUEST_REVIEW) {
			Log.i(AnkiDroidApp.TAG, "Result code = " + resultCode);
			// TODO: Return to standard scheduler
			// TODO: handle big widget
			switch (resultCode) {
			case Reviewer.RESULT_SESSION_COMPLETED:
			default:
				// do not reload counts, if activity is created anew because it has been before destroyed by android
				 showContentView(CONTENT_STUDY_OPTIONS, mDontSaveOnStop);
				break;
			case Reviewer.RESULT_NO_MORE_CARDS:
				showContentView(CONTENT_CONGRATS, mDontSaveOnStop);
				break;
			}
			mDontSaveOnStop = false;
		} else if (requestCode == BROWSE_CARDS && resultCode == getActivity().RESULT_OK) {
			mDontSaveOnStop = false;
			resetAndUpdateValuesFromDeck();
		} else if (requestCode == STATISTICS && mCurrentContentView == CONTENT_CONGRATS) {
			showContentView(CONTENT_STUDY_OPTIONS);
		}
	}

	private void savePreferences(String name, boolean value) {
		SharedPreferences preferences = PrefSettings
				.getSharedPrefs(getActivity().getBaseContext());
		Editor editor = preferences.edit();
		editor.putBoolean(name, value);
		editor.commit();
	}

	private SharedPreferences restorePreferences() {
		SharedPreferences preferences = PrefSettings
				.getSharedPrefs(getActivity().getBaseContext());

		mSwipeEnabled = preferences.getBoolean("swipe", false);
		mInvertedColors = preferences.getBoolean("invertedColors", false);

		mZeemoteEnabled = preferences.getBoolean("zeemote", false);

		// TODO: set language
//		mLocale = preferences.getString("language", "");
//		AnkiDroidApp.setLanguage(mLocale);

		return preferences;
	}

	DeckTask.TaskListener mUpdateValuesFromDeckListener = new DeckTask.TaskListener() {
		@Override
		public void onPostExecute(DeckTask.TaskData result) {
			Object[] obj = result.getObjArray();
			int newCards = (Integer) obj[0]; 
			int lrnCards = (Integer) obj[1]; 
			int revCards = (Integer) obj[2]; 
			int totalNew = (Integer) obj[3]; 
			int totalCards = (Integer) obj[4];
			mProgressMature = (Double) obj[5];
			mProgressAll = (Double) obj[6];
			int eta = (Integer) obj[7];
			double[][] serieslist = (double[][]) obj[8];

			updateStatisticBars();
			updateChart(serieslist);

			mTextTodayNew.setText(String.valueOf(newCards));
			mTextTodayLrn.setText(String.valueOf(lrnCards));
			mTextTodayRev.setText(String.valueOf(revCards));
			mTextNewTotal.setText(String.valueOf(totalNew));
			mTextTotal.setText(String.valueOf(totalCards));
			if (eta != -1) {
				mTextETA.setText(Integer.toString(eta));				
			} else {
				mTextETA.setText("-");
			}

			if(mDeckCounts.getVisibility() == View.INVISIBLE) {
				mDeckCounts.setVisibility(View.VISIBLE);
				mDeckCounts.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_IN, 500, 0));
			}
		}
		@Override
		public void onPreExecute() {
		}
		@Override
		public void onProgressUpdate(DeckTask.TaskData... values) {
		}
	};

	DeckTask.TaskListener mLoadStatisticsHandler = new DeckTask.TaskListener() {

		@Override
		public void onPostExecute(DeckTask.TaskData result) {
			if (mProgressDialog.isShowing()) {
				try {
					mProgressDialog.dismiss();
				} catch (Exception e) {
					Log.e(AnkiDroidApp.TAG,
							"onPostExecute - Dialog dismiss Exception = "
									+ e.getMessage());
				}
			}
			if (result.getBoolean()) {
//				if (mStatisticType == Statistics.TYPE_DECK_SUMMARY) {
//					Statistics.showDeckSummary(getActivity());
//				} else {
					Intent intent = new Intent(getActivity(), com.ichi2.charts.ChartBuilder.class);
					startActivityForResult(intent, STATISTICS);
					if (UIUtils.getApiLevel() > 4) {
						ActivityTransitionAnimation.slide(getActivity(), ActivityTransitionAnimation.DOWN);
					}
//				}
			} else {
				// TODO: db error handling
			}
		}

		@Override
		public void onPreExecute() {
			mProgressDialog = StyledProgressDialog.show(getActivity(), "",
					getResources().getString(R.string.calculating_statistics),
					true);
		}

		@Override
		public void onProgressUpdate(DeckTask.TaskData... values) {
		}

	};


	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (mSwipeEnabled) {
				try {
					if (e1.getX() - e2.getX() > DeckPicker.sSwipeMinDistance
							&& Math.abs(velocityX) > DeckPicker.sSwipeThresholdVelocity
							&& Math.abs(e1.getY() - e2.getY()) < DeckPicker.sSwipeMaxOffPath) {
						// left
						openReviewer();
					} else if (e2.getX() - e1.getX() > DeckPicker.sSwipeMinDistance
							&& Math.abs(velocityX) > DeckPicker.sSwipeThresholdVelocity
							&& Math.abs(e1.getY() - e2.getY()) < DeckPicker.sSwipeMaxOffPath) {
						// right
						closeStudyOptions();
					} else if (e2.getY() - e1.getY() > DeckPicker.sSwipeMinDistance
							&& Math.abs(velocityY) > DeckPicker.sSwipeThresholdVelocity
							&& Math.abs(e1.getX() - e2.getX()) < DeckPicker.sSwipeMaxOffPath) {
						// down
						DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_STATISTICS, mLoadStatisticsHandler, new DeckTask.TaskData(mCol, Stats.TYPE_FORECAST, false));
					} else if (e1.getY() - e2.getY() > DeckPicker.sSwipeMinDistance
							&& Math.abs(velocityY) > DeckPicker.sSwipeThresholdVelocity
							&& Math.abs(e1.getX() - e2.getX()) < DeckPicker.sSwipeMaxOffPath) {
						// up
						addNote();
					}

				} catch (Exception e) {
					Log.e(AnkiDroidApp.TAG,
							"onFling Exception = " + e.getMessage());
				}
			}
			return false;
		}
	}

//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		if (mSwipeEnabled && gestureDetector.onTouchEvent(event))
//			return true;
//		else
//			return false;
//	}

	@Override
	public void buttonPressed(ButtonEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void buttonReleased(ButtonEvent arg0) {
		Log.d("Zeemote", "Button released, id: " + arg0.getButtonID());
		Message msg = Message.obtain();
		msg.what = MSG_ZEEMOTE_BUTTON_A + arg0.getButtonID(); // Button A = 0,
																// Button B =
																// 1...
		if ((msg.what >= MSG_ZEEMOTE_BUTTON_A)
				&& (msg.what <= MSG_ZEEMOTE_BUTTON_D)) { // make sure messages
															// from future
															// buttons don't get
															// throug
			this.ZeemoteHandler.sendMessage(msg);
		}
		if (arg0.getButtonID() == -1) {
			msg.what = MSG_ZEEMOTE_BUTTON_D + arg0.getButtonGameAction();
			if ((msg.what >= MSG_ZEEMOTE_STICK_UP)
					&& (msg.what <= MSG_ZEEMOTE_STICK_RIGHT)) { // make sure
																// messages from
																// future
																// buttons don't
																// get throug
				this.ZeemoteHandler.sendMessage(msg);
			}
		}
	}
}
