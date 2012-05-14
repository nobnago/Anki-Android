/****************************************************************************************
 * Copyright (c) 2009 Andrew Dubya <andrewdubya@gmail.com>                              *
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2009 Daniel Svärd <daniel.svard@gmail.com>                             * 
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;import com.ichi2.anki2.R;

import android.app.Dialog;
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
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anim.ViewAnimation;
import com.ichi2.async.Connection;
import com.ichi2.async.DeckTask;
import com.ichi2.async.Connection.Payload;
import com.ichi2.async.DeckTask.TaskData;
import com.ichi2.charts.ChartBuilder;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Utils;
import com.ichi2.themes.StyledDialog;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.themes.Themes;
import com.ichi2.widget.WidgetStatus;
import com.tomgibara.android.veecheck.util.PrefSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.json.JSONException;

//zeemote imports
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;
import com.zeemote.zc.util.JoystickToButtonAdapter;


public class DeckPicker extends FragmentActivity {

	public static final int CRAM_DECK_FRAGMENT = -1;
	/**
	 * Dialogs
	 */
	private static final int DIALOG_NO_SDCARD = 0;
	private static final int DIALOG_USER_NOT_LOGGED_IN_SYNC = 1;
	private static final int DIALOG_USER_NOT_LOGGED_IN_ADD_SHARED_DECK = 2;
	private static final int DIALOG_NO_CONNECTION = 3;
	private static final int DIALOG_DELETE_DECK = 4;
	private static final int DIALOG_SELECT_STATISTICS_TYPE = 5;
	private static final int DIALOG_SELECT_STATISTICS_PERIOD = 6;
	private static final int DIALOG_DELETE_BACKUPS = 8;
	private static final int DIALOG_CONTEXT_MENU = 9;
	private static final int DIALOG_REPAIR_COLLECTION = 10;
	private static final int DIALOG_NO_SPACE_LEFT = 11;
	private static final int DIALOG_SYNC_CONFLICT_RESOLUTION = 12;
	private static final int DIALOG_CONNECTION_ERROR = 13;
	private static final int DIALOG_SYNC_LOG = 15;
	private static final int DIALOG_SELECT_HELP = 16;
	private static final int DIALOG_BACKUP_NO_SPACE_LEFT = 17;
	private static final int DIALOG_OK = 18;
	private static final int DIALOG_DB_ERROR = 19;
	private static final int DIALOG_ERROR_HANDLING = 20;
	private static final int DIALOG_LOAD_FAILED = 21;
	private static final int DIALOG_RESTORE_BACKUP = 22;
	private static final int DIALOG_SD_CARD_NOT_MOUNTED = 23;

	private String mDialogMessage;
	private int[] mRepairValues;

	/**
	 * Menus
	 */
    private static final int MENU_ABOUT = 0;
    private static final int MENU_CREATE_DECK = 1;
    private static final int MENU_ADD_SHARED_DECK = 2;
    private static final int MENU_PREFERENCES = 3;
    private static final int MENU_MY_ACCOUNT = 4;
    private static final int MENU_FEEDBACK = 5;
	private static final int MENU_HELP = 6;
    private static final int CHECK_DATABASE = 7;
    private static final int MENU_SYNC = 8;
    private static final int MENU_ADD_NOTE = 9;
    private static final int MENU_ADD_DECK = 10;
    private static final int MENU_STATISTICS = 11;
    private static final int MENU_CARDBROWSER = 12;

	/**
	 * Context Menus
	 */
    private static final int CONTEXT_MENU_DECK_SUMMARY = 0;
    private static final int CONTEXT_MENU_CUSTOM_DICTIONARY = 1;
    private static final int CONTEXT_MENU_RESET_LANGUAGE = 2;
    private static final int CONTEXT_MENU_RENAME_DECK = 3;
    private static final int CONTEXT_MENU_DELETE_DECK = 4;
    
	/**
	 * Message types
	 */
	private static final int MSG_LOADING_DECK = 0;
	private static final int MSG_UPGRADE_NEEDED = 1;
	private static final int MSG_UPGRADE_SUCCESS = 2;
	private static final int MSG_UPGRADE_FAILURE = 3;
	private static final int MSG_COULD_NOT_BE_LOADED = 4;
	private static final int MSG_CREATING_BACKUP = 5;
	private static final int MSG_BACKUP_ERROR = 6;

	public static final String EXTRA_START = "start";
	public static final String EXTRA_DECK_ID = "deckId";
	public static final int EXTRA_START_NOTHING = 0;
	public static final int EXTRA_START_REVIEWER = 1;
	public static final int EXTRA_START_DECKPICKER = 2;
	public static final int EXTRA_DB_ERROR = 3;

	public static final int RESULT_MEDIA_EJECTED = 202;
	public static final int RESULT_DB_ERROR = 203;
	public static final int RESULT_RESTART = 204;

	
    /** Zeemote messages */
    private static final int MSG_ZEEMOTE_BUTTON_A = 0x110;
    private static final int MSG_ZEEMOTE_BUTTON_B = MSG_ZEEMOTE_BUTTON_A+1;
    private static final int MSG_ZEEMOTE_BUTTON_C = MSG_ZEEMOTE_BUTTON_A+2;
    private static final int MSG_ZEEMOTE_BUTTON_D = MSG_ZEEMOTE_BUTTON_A+3;
    private static final int MSG_ZEEMOTE_STICK_UP = MSG_ZEEMOTE_BUTTON_A+4;
    private static final int MSG_ZEEMOTE_STICK_DOWN = MSG_ZEEMOTE_BUTTON_A+5;
    private static final int MSG_ZEEMOTE_STICK_LEFT = MSG_ZEEMOTE_BUTTON_A+6;
    private static final int MSG_ZEEMOTE_STICK_RIGHT = MSG_ZEEMOTE_BUTTON_A+7;
	
    /**
	* Available options performed by other activities
	*/
    private static final int PREFERENCES_UPDATE = 0;
    private static final int CREATE_DECK = 1;
    private static final int DOWNLOAD_SHARED_DECK = 3;
    public static final int REPORT_FEEDBACK = 4;
    private static final int LOG_IN_FOR_DOWNLOAD = 5;
    private static final int LOG_IN_FOR_SYNC = 6;
    private static final int STUDYOPTIONS = 7;
    private static final int SHOW_INFO_WELCOME = 8;
    private static final int SHOW_INFO_NEW_VERSION = 9;
    private static final int REPORT_ERROR = 10;
    private static final int SHOW_STUDYOPTIONS = 11;
    private static final int ADD_NOTE = 12;
    private static final int LOG_IN = 13;
    private static final int BROWSE_CARDS = 14;
    private static final int ADD_SHARED_DECKS = 15;
    private static final int LOG_IN_FOR_SHARED_DECK = 16;
    private static final int ADD_CRAM_DECK = 17;

	private Collection mCol;

	private StyledProgressDialog mProgressDialog;
	private StyledDialog mUpgradeNotesAlert;
	private StyledDialog mMissingMediaAlert;
	private StyledDialog mDeckNotLoadedAlert;
	private StyledDialog mNoSpaceLeftAlert;
	private ImageButton mAddButton;
	private ImageButton mCardsButton;
	private ImageButton mStatsButton;
	private ImageButton mSyncButton;

	private File[] mBackups;
	private ArrayList<String> mBrokenDecks;
	private boolean mRestoredOrDeleted = false;
	private ArrayList<String> mAlreadyDealtWith;

	private SimpleAdapter mDeckListAdapter;
	private ArrayList<HashMap<String, String>> mDeckList;
	private ListView mDeckListView;

	private File[] mFileList;

	private boolean mDontSaveOnStop = false;
	private boolean mDeckIsSelected = false;

	private BroadcastReceiver mUnmountReceiver = null;

	private String mPrefDeckPath = null;
	private long mLastTimeOpened;
	private int mPrefDeckOrder = 0;
	private boolean mPrefStartupDeckPicker = false;
	private long mCurrentDid;
	private String mCurrentDeckPath = null;
	private int mSyncMediaUsn = 0;

	private EditText mDialogEditText;

	int mStatisticType;

	public boolean mFragmented;

	boolean mCompletionBarRestrictToActive = false; // set this to true in order to calculate completion bar only for active cards

	private int[] mDictValues;

	private int mContextMenuPosition;

	/** Swipe Detection */
 	private GestureDetector gestureDetector;
 	View.OnTouchListener gestureListener;
 	private boolean mSwipeEnabled;

	private static final int SWIPE_MIN_DISTANCE_DIP = 65;
	private static final int SWIPE_MAX_OFF_PATH_DIP = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY_DIP = 120;

	public static int sSwipeMinDistance;
	public static int sSwipeMaxOffPath;
	public static int sSwipeThresholdVelocity;
 	
 	/**
 	 * Zeemote controller
 	 */
	protected JoystickToButtonAdapter adapter;

	// ----------------------------------------------------------------------------
	// LISTENERS
	// ----------------------------------------------------------------------------

	private AdapterView.OnItemClickListener mDeckSelHandler = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int p, long id) {
			handleDeckSelection(p);
		}
	};


	private DialogInterface.OnClickListener mContextMenuListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int item) {
			Resources res = getResources();

			@SuppressWarnings("unchecked")
			HashMap<String, String> data = (HashMap<String, String>) mDeckListAdapter.getItem(mContextMenuPosition);
			switch (item) {
			case CONTEXT_MENU_DELETE_DECK:
				mCurrentDeckPath = data.get("filepath");
				showDialog(DIALOG_DELETE_DECK);
				return;
			case CONTEXT_MENU_RESET_LANGUAGE:
//				resetDeckLanguages(data.get("filepath"));
				return;
			case CONTEXT_MENU_CUSTOM_DICTIONARY:
//				String[] dicts = res.getStringArray(R.array.dictionary_labels);
//				String[] vals = res.getStringArray(R.array.dictionary_values);
//				int currentSet = MetaDB.getLookupDictionary(DeckPicker.this, data.get("filepath"));
//
//				mCurrentDeckPath = data.get("filepath");
//				String[] labels = new String[dicts.length + 1];
//				mDictValues = new int[dicts.length + 1];
//				int currentChoice = 0;
//				labels[0] = res.getString(R.string.deckpicker_select_dictionary_default);
//				mDictValues[0] = -1;
//				for (int i = 1; i < labels.length; i++) {
//					labels[i] = dicts[i-1];
//					mDictValues[i] = Integer.parseInt(vals[i-1]);
//					if (currentSet == mDictValues[i]) {
//						currentChoice = i;
//					}
//				}
//				StyledDialog.Builder builder = new StyledDialog.Builder(DeckPicker.this);
//				builder.setTitle(res.getString(R.string.deckpicker_select_dictionary_title));
//				builder.setSingleChoiceItems(labels, currentChoice, new DialogInterface.OnClickListener() {
//					public void onClick(DialogInterface dialog, int item) {
//						MetaDB.storeLookupDictionary(DeckPicker.this, mCurrentDeckPath, mDictValues[item]);
//					}
//				});
//				StyledDialog alert = builder.create();
//				alert.show();
				return;
			case CONTEXT_MENU_RENAME_DECK:
				StyledDialog.Builder builder2 = new StyledDialog.Builder(DeckPicker.this);
				builder2.setTitle(res.getString(R.string.contextmenu_deckpicker_rename_deck));

				mDialogEditText = (EditText) new EditText(DeckPicker.this);
				mDialogEditText.setText(mCol.getDecks().name(mCurrentDid));
//				mDialogEditText.setFilters(new InputFilter[] { mDeckNameFilter });
				builder2.setView(mDialogEditText, false, false);
				builder2.setPositiveButton(res.getString(R.string.rename),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								String newName = mDialogEditText.getText().toString().replaceAll("[\'\"\\s\\n\\r\\[\\]\\(\\)]", "");
								if (mCol.getDecks().rename(mCol.getDecks().get(mCurrentDid), newName)) {
									for (HashMap<String, String> d : mDeckList) {
										if (d.get("did").equals(Long.toString(mCurrentDid))) {
											d.put("name", newName);
										}
									}
									mDeckListAdapter.notifyDataSetChanged();
									loadCounts();
								} else {
									try {
										Themes.showThemedToast(DeckPicker.this, getResources().getString(R.string.rename_error, mCol.getDecks().get(mCurrentDid).get("name")), false);
									} catch (JSONException e) {
										throw new RuntimeException(e);
									}
								}
							}
						});
				builder2.setNegativeButton(res.getString(R.string.cancel), null);
				builder2.create().show();
				return;
			case CONTEXT_MENU_DECK_SUMMARY:
//				mStatisticType = 0;
//				DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_STATISTICS, mLoadStatisticsHandler, new DeckTask.TaskData(DeckPicker.this, new String[]{data.get("filepath")}, mStatisticType, 0));
				return;
			}
		}
	};


	private Connection.TaskListener mSyncListener = new Connection.TaskListener() {

		String currentMessage;
		long countUp;
		long countDown;

		@Override
		public void onDisconnected() {
			showDialog(DIALOG_NO_CONNECTION);
		}

		@Override
		public void onPreExecute() {
			countUp = 0;
			countDown = 0;
			if (mProgressDialog == null || !mProgressDialog.isShowing()) {
				mProgressDialog = StyledProgressDialog.show(DeckPicker.this, getResources().getString(R.string.sync_title), getResources().getString(R.string.sync_prepare_syncing) + "\n" + getResources().getString(R.string.sync_up_down_size, countUp, countDown), true, false);
			}
		}

		@Override
		public void onProgressUpdate(Object... values) {
            Resources res = getResources();
            if (values[0] instanceof Boolean) {
                // This is the part Download missing media of syncing
                int total = ((Integer)values[1]).intValue();
                int done = ((Integer)values[2]).intValue();
                values[0] = ((String)values[3]);
                values[1] = res.getString(R.string.sync_downloading_media, done, total);
            } else if (values[0] instanceof Integer) {
    			int id = (Integer) values[0];
    			if (id != 0) {
    				currentMessage = res.getString(id);
    			}
    			if (values.length >= 3) {
    				countUp = (Long) values[1];
    				countDown = (Long) values[2];
    			}            	
            }
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
//				mProgressDialog.setTitle((String) values[0]);
				mProgressDialog.setMessage(currentMessage + "\n" + res.getString(R.string.sync_up_down_size, countUp / 1024, countDown / 1024));
			}
		}

		@Override
		public void onPostExecute(Payload data) {
			Log.i(AnkiDroidApp.TAG, "onPostExecute");
			Resources res = DeckPicker.this.getResources();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
			}
			if (!data.success) {
				Object[] result = (Object[]) data.result;
				if (result[0] instanceof String) {
					String resultType = (String) result[0];
					if (resultType.equals("badAuth")) {
						// delete old auth information
						SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
				        Editor editor = preferences.edit();
				        editor.putString("username", "");
				        editor.putString("hkey", "");
				        editor.commit();
				        // then show 
						showDialog(DIALOG_USER_NOT_LOGGED_IN_SYNC);
					} else if (resultType.equals("noChanges")) {
						mDialogMessage = res.getString(R.string.sync_no_changes_message);
						showDialog(DIALOG_SYNC_LOG);
					} else if (resultType.equals("clockOff")) {
                        long diff = (Long) result[1];
                        if (diff >= 86400) {
                            // The difference if more than a day
                        	mDialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff, res.getString(R.string.sync_log_clocks_unsynchronized_date));
                        } else if (Math.abs((diff % 3600.0) - 1800.0) >= 1500.0) {
                            // The difference would be within limit if we adjusted the time by few hours
                            // It doesn't work for all timezones, but it covers most and it's a guess anyway
                        	mDialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff, res.getString(R.string.sync_log_clocks_unsynchronized_tz));
                        } else {
                            mDialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff, "");
                        }
						showDialog(DIALOG_SYNC_LOG);
					} else if (resultType.equals("fullSync")) {
					    if (data.data != null && data.data.length >= 1 && data.data[0] instanceof Integer) {
	                        mSyncMediaUsn = (Integer) data.data[0];
	                    }
						showDialog(DIALOG_SYNC_CONFLICT_RESOLUTION);
					} else if (resultType.equals("dbError")) {
						mDialogMessage = res.getString(R.string.sync_corrupt_database, R.string.repair_deck);
						showDialog(DIALOG_SYNC_LOG);
					} else if (resultType.equals("overwriteError")) {
						mDialogMessage = res.getString(R.string.sync_overwrite_error);
						showDialog(DIALOG_SYNC_LOG);
					} else if (resultType.equals("remoteDbError")) {
						mDialogMessage = res.getString(R.string.sync_remote_db_error);
						showDialog(DIALOG_SYNC_LOG);
					} else if (resultType.equals("sdAccessError")) {
						mDialogMessage = res.getString(R.string.sync_write_access_error);
						showDialog(DIALOG_SYNC_LOG);
					} else if (resultType.equals("finishError")) {
						mDialogMessage = res.getString(R.string.sync_log_finish_error);
						showDialog(DIALOG_SYNC_LOG);
					} else if (resultType.equals("genericError")) {
						mDialogMessage = res.getString(R.string.sync_generic_error);
						showDialog(DIALOG_SYNC_LOG);
					} else {
						int type = (Integer) result[1];
						switch (type) {
						case 503:
	                        mDialogMessage = res.getString(R.string.sync_too_busy);
							break;
						default:
							mDialogMessage = res.getString(R.string.sync_log_error_specific, Integer.toString(type), (String)result[2]);
							break;
						}
						showDialog(DIALOG_SYNC_LOG);
					}
					if (data.data != null && data.data.length >= 1 && data.data[0] instanceof Collection) {
						mCol = (Collection) data.data[0];
					}
				}
			} else {
				updateDecksList((TreeSet<Object[]>) data.result, (Integer)data.data[2], (Integer)data.data[3]);
				if (data.data.length > 0 && data.data[0] instanceof String && ((String)data.data[0]).length() > 0) {
					String dataString = (String) data.data[0];
					if (dataString.equals("upload")) {
						mDialogMessage = res.getString(R.string.sync_log_uploading_message);
						mCol = (Collection) data.data[1];
					} else if (dataString.equals("download")) {
						mDialogMessage = res.getString(R.string.sync_log_downloading_message);
						// set downloaded collection as current one
						mCol = (Collection) data.data[1];
					} else {
						mDialogMessage = res.getString(R.string.sync_database_success);
					}
				} else {
					mDialogMessage = res.getString(R.string.sync_database_success);
				}
				showDialog(DIALOG_SYNC_LOG);
			}
            mSyncButton.setClickable(true);
		}
	};

//   private Connection.TaskListener mDownloadMediaListener = new Connection.TaskListener() {
//
//        @Override
//        public void onDisconnected() {
//            showDialog(DIALOG_NO_CONNECTION);
//        }
//
//        @Override
//        public void onPreExecute() {
//            // Pass
//        }
//
//        @Override
//        public void onProgressUpdate(Object... values) {
//            int total = ((Integer)values[1]).intValue();
//            int done = ((Integer)values[2]).intValue();
//            if (!((Boolean)values[0]).booleanValue()) {
//                // Initializing, just get the count of missing media
//                if (mProgressDialog != null && mProgressDialog.isShowing()) {
//                    mProgressDialog.dismiss();
//                }
//                mProgressDialog.setMax(total);
//                mProgressDialog.show();
//            } else {
//                mProgressDialog.setProgress(done);
//            }
//        }
//
//        @Override
//        public void onPostExecute(Payload data) {
//            Log.i(AnkiDroidApp.TAG, "onPostExecute");
//            Resources res = getResources();
//            if (mProgressDialog != null) {
//                mProgressDialog.dismiss();
//            }
//
//            if (data.success) {
//                int total = ((Integer)data.data[0]).intValue();
//                if (total == 0) {
//                    mMissingMediaAlert
//                        .setMessage(res.getString(R.string.deckpicker_download_missing_none));
//                } else {
//                    int done = ((Integer)data.data[1]).intValue();
//                    int missing = ((Integer)data.data[2]).intValue();
//                    mMissingMediaAlert
//                        .setMessage(res.getString(R.string.deckpicker_download_missing_success, done, missing));
//                }
//            } else {
//                String failedFile = (String)data.data[0];
//                mMissingMediaAlert
//                    .setMessage(res.getString(R.string.deckpicker_download_missing_error, failedFile));
//            }
//            mMissingMediaAlert.show();
//            
//            Deck deck = (Deck) data.result;
//			DeckManager.closeDeck(deck.getDeckPath(), DeckManager.REQUESTING_ACTIVITY_DECKPICKER);
//         }
//    };


    DeckTask.TaskListener mOpenCollectionHandler = new DeckTask.TaskListener() {

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
			if (mCol == null) {
				showDialog(DIALOG_LOAD_FAILED);
				return;
			}
			Object[] res = result.getObjArray();
			updateDecksList((TreeSet<Object[]>) res[0], (Integer)res[1], (Integer)res[2]);
			mDeckListView.setVisibility(View.VISIBLE);
			mDeckListView.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_IN, 500, 0));

			if (mFragmented) {
				long active = mCol.getDecks().selected();
				for (int i = 0; i < mDeckList.size(); i++) {
					if (mDeckList.get(i).get("did").equals(Long.toString(active))) {
						mDeckListView.setSelection(i);
						break;
					}
				}
			}
		}

		@Override
		public void onPreExecute() {
            mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "", getResources().getString(R.string.open_collection), true, true, new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface arg0) {
					// TODO: close dbs?
					DeckTask.cancelTask();
					finish();
				}
			});
			mDeckListView.setVisibility(View.INVISIBLE);
			mDeckListView.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_OUT, 500, 0));
		}

		@Override
		public void onProgressUpdate(DeckTask.TaskData... values) {
			String message = values[0].getString();
			if (message != null) {
				mProgressDialog.setMessage(message);
			}
		}
    };


    DeckTask.TaskListener mLoadCountsHandler = new DeckTask.TaskListener() {

		@Override
		public void onPostExecute(DeckTask.TaskData result) {
			if (result == null) {
				// TODO: error handling
				return;
			}
			Object[] res = result.getObjArray();
			updateDecksList((TreeSet<Object[]>) res[0], (Integer)res[1], (Integer)res[2]);
		}

		@Override
		public void onPreExecute() {
		}

		@Override
		public void onProgressUpdate(DeckTask.TaskData... values) {
		}
    };


    DeckTask.TaskListener mCloseCollectionHandler = new DeckTask.TaskListener() {

		@Override
		public void onPostExecute(DeckTask.TaskData result) {
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
                    Log.e(AnkiDroidApp.TAG, "onPostExecute - Dialog dismiss Exception = " + e.getMessage());
                }
            }
            if (result.getBoolean()) {
//		    	if (mStatisticType == Statistics.TYPE_DECK_SUMMARY) {
//		    		Statistics.showDeckSummary(DeckPicker.this);
//		    	} else {
			    	Intent intent = new Intent(DeckPicker.this, com.ichi2.charts.ChartBuilder.class);
			    	startActivity(intent);
			        if (UIUtils.getApiLevel() > 4) {
			            ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.DOWN);
			        }	
//		    	}
			} else {
				// TODO: db errro handling
			}
		}

		@Override
		public void onPreExecute() {
            mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "", getResources()
                    .getString(R.string.calculating_statistics), true);
		}

		@Override
		public void onProgressUpdate(DeckTask.TaskData... values) {
		}

    };


    DeckTask.TaskListener mRepairDeckHandler = new DeckTask.TaskListener() {

    	@Override
        public void onPreExecute() {
            mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "", getResources()
                    .getString(R.string.backup_repair_deck_progress), true);
        }


        @Override
        public void onPostExecute(DeckTask.TaskData result) {
        	if (mProgressDialog != null && mProgressDialog.isShowing()) {
        		mProgressDialog.dismiss();
        	}
        	if (result.getBoolean()) {
        		loadCollection();
        	} else {
        		Themes.showThemedToast(DeckPicker.this, getResources().getString(R.string.deck_repair_error), true);
        	}
        }
 
		@Override
		public void onProgressUpdate(TaskData... values) {
		}

    };


    DeckTask.TaskListener mRestoreDeckHandler = new DeckTask.TaskListener() {

    	@Override
        public void onPreExecute() {
            mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "", getResources()
                    .getString(R.string.backup_restore_deck), true);
        }


        @Override
        public void onPostExecute(DeckTask.TaskData result) {
			switch (result.getInt()) {
    		case BackupManager.RETURN_DECK_RESTORED:
    			mRestoredOrDeleted = true;
//                handleRestoreDecks(true);
                break;    			
    		case BackupManager.RETURN_ERROR:
        		mDeckNotLoadedAlert.show();
        		Themes.showThemedToast(DeckPicker.this, getResources().getString(R.string.backup_restore_error), true);
    			break;
    		case BackupManager.RETURN_NOT_ENOUGH_SPACE:
    			mDeckNotLoadedAlert.show();
    			mNoSpaceLeftAlert.show();
    			break;
    		}        		
        	if (mProgressDialog != null && mProgressDialog.isShowing()) {
        		mProgressDialog.dismiss();
        	}
        }
 
		@Override
		public void onProgressUpdate(TaskData... values) {
		}

    };

    //Zeemote handler
	Handler ZeemoteHandler = new Handler() {
		public void handleMessage(Message msg){
			switch(msg.what){
			case MSG_ZEEMOTE_STICK_UP:
				mDeckListView.requestFocusFromTouch();
				sendKey(KeyEvent.KEYCODE_DPAD_UP);
				break;
			case MSG_ZEEMOTE_STICK_DOWN:
				mDeckListView.requestFocusFromTouch();
				sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
				break;
			case MSG_ZEEMOTE_STICK_LEFT:
				break;
			case MSG_ZEEMOTE_STICK_RIGHT:
				break;
			case MSG_ZEEMOTE_BUTTON_A:
				sendKey(KeyEvent.KEYCODE_ENTER);
				break;
			case MSG_ZEEMOTE_BUTTON_B:
				sendKey(KeyEvent.KEYCODE_BACK);
				break;
			case MSG_ZEEMOTE_BUTTON_C:
				break;
			case MSG_ZEEMOTE_BUTTON_D:
				break;
			}
			super.handleMessage(msg);
		}
	};

    
	// ----------------------------------------------------------------------------
	// ANDROID METHODS
	// ----------------------------------------------------------------------------

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) throws SQLException {
		Log.i(AnkiDroidApp.TAG, "DeckPicker - onCreate");
        Intent intent = getIntent();
		if (!isTaskRoot()) {
			Log.i(AnkiDroidApp.TAG, "DeckPicker - onCreate: Detected multiple instance of this activity, closing it and return to root activity");
	        Intent reloadIntent = new Intent(DeckPicker.this, DeckPicker.class);
	        reloadIntent.setAction(Intent.ACTION_MAIN);
	        if (intent != null && intent.getExtras() != null) {
		        reloadIntent.putExtras(intent.getExtras());	        	
	        }
	        reloadIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	        reloadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			finish();
			startActivityIfNeeded(reloadIntent, 0);
		}

		Themes.applyTheme(this);
		super.onCreate(savedInstanceState);

//        mStartedByBigWidget = intent.getIntExtra(EXTRA_START, EXTRA_START_NOTHING);

		SharedPreferences preferences = restorePreferences();

		// activate broadcast messages if first start of a day
		if (mLastTimeOpened < UIUtils.getDayStart()) {
			preferences.edit().putBoolean("showBroadcastMessageToday", true).commit();			
		}
		preferences.edit().putLong("lastTimeOpened", System.currentTimeMillis()).commit();

//		if (intent != null && intent.hasExtra(EXTRA_DECK_ID)) {
//			openStudyOptions(intent.getLongExtra(EXTRA_DECK_ID, 1));
//		}

		BroadcastMessages.checkForNewMessages(this);

		View mainView = getLayoutInflater().inflate(R.layout.deck_picker, null);
		setContentView(mainView);

		// check, if tablet layout
		View studyoptionsFrame = findViewById(R.id.studyoptions_fragment);
		mFragmented = studyoptionsFrame != null && studyoptionsFrame.getVisibility() == View.VISIBLE;
        
		Themes.setContentStyle(mainView, Themes.CALLER_DECKPICKER);

		registerExternalStorageListener();

		mAddButton = (ImageButton) findViewById(R.id.deckpicker_add);
		mAddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addNote();
			}
		});

		mCardsButton = (ImageButton) findViewById(R.id.deckpicker_card_browser);
		mCardsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openCardBrowser();
			}
		});

		mStatsButton = (ImageButton) findViewById(R.id.statistics_all_button);
		mStatsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_SELECT_STATISTICS_TYPE);
			}
		});

		mSyncButton = (ImageButton) findViewById(R.id.sync_all_button);
		mSyncButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sync();
			}
		});
		
		mDeckList = new ArrayList<HashMap<String, String>>();
		mDeckListView = (ListView) findViewById(R.id.files);
		mDeckListAdapter = new SimpleAdapter(this, mDeckList,
				R.layout.deck_item, new String[] { "name", "new", "lrn", "rev", //"complMat", "complAll", 
				"sep" }, new int[] {
						R.id.DeckPickerName, R.id.deckpicker_new, R.id.deckpicker_lrn, 
						R.id.deckpicker_rev, //R.id.deckpicker_bar_mat, R.id.deckpicker_bar_all, 
						R.id.DeckPickerName });
		mDeckListAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Object data, String text) {
				if (view.getId() == R.id.DeckPickerName) {
					View parent = (View) view.getParent().getParent();
					if (text.equals("top")) {
						parent.setBackgroundResource(R.drawable.white_deckpicker_top);
						return true;
					} else if (text.equals("bot")) {
						parent.setBackgroundResource(R.drawable.white_deckpicker_bottom);
						return true;
					} else if (text.equals("ful")) {
						parent.setBackgroundResource(R.drawable.white_deckpicker_full);
						return true;
					} else if (text.equals("cen")) {
						parent.setBackgroundResource(R.drawable.white_deckpicker_center);
						return true;
					}
					return false;
//				} else if (view.getId() == R.id.deckpicker_bar_mat || view.getId() == R.id.deckpicker_bar_all) {
//					if (text.length() > 0 && !text.equals("-1.0")) {
//						View parent = (View)view.getParent().getParent();
//						if (text.equals("-2")) {
//							parent.setVisibility(View.GONE);							
//						} else {
//							Utils.updateProgressBars(view, (int) UIUtils.getDensityAdjustedValue(DeckPicker.this, 3.4f), (int) (Double.parseDouble(text) * ((View)view.getParent().getParent().getParent()).getHeight()));
//							if (parent.getVisibility() == View.INVISIBLE) {
//								parent.setVisibility(View.VISIBLE);
//								parent.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_IN, 500, 0));							
//							}							
//						}
//					}
//					return true;
//				} else if (view.getVisibility() == View.INVISIBLE) {
//					if (!text.equals("-1")) {
//						view.setVisibility(View.VISIBLE);
//						view.setAnimation(ViewAnimation.fade(ViewAnimation.FADE_IN, 500, 0));
//						return false;						
//					}
//				} else if (text.equals("-1")){
//					view.setVisibility(View.INVISIBLE);
//					return false;					
				}
				return false;
			}
		});
		mDeckListView.setOnItemClickListener(mDeckSelHandler);
		mDeckListView.setAdapter(mDeckListAdapter);

		if (mFragmented) {
			mDeckListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);			
		}

		registerForContextMenu(mDeckListView);

		showStartupScreensAndDialogs(preferences, 0);

		if (mSwipeEnabled) {
			gestureDetector = new GestureDetector(new MyGestureDetector());
	        mDeckListView.setOnTouchListener(new View.OnTouchListener() {
	        	public boolean onTouch(View v, MotionEvent event) {
	        		if (gestureDetector.onTouchEvent(event)) {
	        			return true;
	        		}
	        		return false;
	        		}
	        	});
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCol != null) {
			if (Utils.now() > mCol.getSched().getDayCutoff() && AnkiDroidApp.isSdCardMounted()) {
				loadCounts();
			}
		}
	}

	private void loadCollection() {
		DeckTask.launchDeckTask(DeckTask.TASK_TYPE_OPEN_COLLECTION, mOpenCollectionHandler, new DeckTask.TaskData(AnkiDroidApp.getCollectionPath()));
	}

	private void loadCounts() {
		if (mCol != null) {
			DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_DECK_COUNTS, mLoadCountsHandler, new TaskData(mCol));
		}
	}

	private void addNote() {
		Intent intent = new Intent(DeckPicker.this, CardEditor.class);
		intent.putExtra(CardEditor.EXTRA_CALLER, CardEditor.CALLER_DECKPICKER);
		startActivityForResult(intent, ADD_NOTE);
		if (UIUtils.getApiLevel() > 4) {
			ActivityTransitionAnimation.slide(DeckPicker.this,
					ActivityTransitionAnimation.LEFT);
		}
	}

	private void openCardBrowser() {
		Intent cardBrowser = new Intent(DeckPicker.this, CardBrowser.class);
		cardBrowser.putExtra("fromDeckpicker", true);
		startActivityForResult(cardBrowser, BROWSE_CARDS);
		if (UIUtils.getApiLevel() > 4) {
			ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.LEFT);
		}		
	}

	private boolean hasErrorFiles() {
        for (String file : this.fileList()) {
            if (file.endsWith(".stacktrace")) {
                return true;
            }
        }
        return false;
    }


	private SharedPreferences restorePreferences() {
        SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
        mPrefDeckPath = preferences.getString("deckPath", AnkiDroidApp.getDefaultAnkiDroidDirectory());
        mLastTimeOpened = preferences.getLong("lastTimeOpened", 0);
        mSwipeEnabled = preferences.getBoolean("swipe", false);

        // Convert dip to pixel, code in parts from http://code.google.com/p/k9mail/
        final float gestureScale = getResources().getDisplayMetrics().density;
        int sensibility = preferences.getInt("swipeSensibility", 100);
        if (sensibility != 100) {
            float sens = (200 - sensibility) / 100.0f;
            sSwipeMinDistance = (int)(SWIPE_MIN_DISTANCE_DIP * sens * gestureScale + 0.5f);
            sSwipeThresholdVelocity = (int)(SWIPE_THRESHOLD_VELOCITY_DIP * sens * gestureScale + 0.5f);
            sSwipeMaxOffPath = (int)(SWIPE_MAX_OFF_PATH_DIP * Math.sqrt(sens) * gestureScale + 0.5f);
        } else {
            sSwipeMinDistance = (int)(SWIPE_MIN_DISTANCE_DIP * gestureScale + 0.5f);
            sSwipeThresholdVelocity = (int)(SWIPE_THRESHOLD_VELOCITY_DIP * gestureScale + 0.5f);
            sSwipeMaxOffPath = (int)(SWIPE_MAX_OFF_PATH_DIP * gestureScale + 0.5f);
        }

//        mInvertedColors = preferences.getBoolean("invertedColors", false);
//        mSwap = preferences.getBoolean("swapqa", false);
//        mLocale = preferences.getString("language", "");
//        mZeemoteEnabled = preferences.getBoolean("zeemote", false);
//       	setLanguage(mLocale);

		mSwipeEnabled = preferences.getBoolean("swipe", false);

        return preferences;
    }


	private void showStartupScreensAndDialogs(SharedPreferences preferences, int skip) {
		if (skip < 1 && preferences.getLong("lastTimeOpened", 0) == 0) {
			Intent infoIntent = new Intent(this, Info.class);
			infoIntent.putExtra(Info.TYPE_EXTRA, Info.TYPE_WELCOME);
			startActivityForResult(infoIntent, SHOW_INFO_WELCOME);
			if (skip != 0 && UIUtils.getApiLevel() > 4) {
				ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.LEFT);
			}
		} else if (skip < 2 && !preferences.getString("lastVersion", "").equals(AnkiDroidApp.getPkgVersion())) {
			preferences.edit().putBoolean("showBroadcastMessageToday", true).commit();
			Intent infoIntent = new Intent(this, Info.class);
			infoIntent.putExtra(Info.TYPE_EXTRA, Info.TYPE_NEW_VERSION);
			startActivityForResult(infoIntent, SHOW_INFO_NEW_VERSION);
            if (skip != 0 && UIUtils.getApiLevel() > 4) {
            	ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.LEFT);
            }
		} else if (skip < 3 && hasErrorFiles()) {
			Intent i = new Intent(this, Feedback.class);
			startActivityForResult(i, REPORT_ERROR);
			if (skip != 0 && UIUtils.getApiLevel() > 4) {
				ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.LEFT);
			}
		} else if (!AnkiDroidApp.isSdCardMounted()) {
			showDialog(DIALOG_SD_CARD_NOT_MOUNTED);			
		} else if (!BackupManager.enoughDiscSpace(mPrefDeckPath)) {// && !preferences.getBoolean("dontShowLowMemory", false)) {
			showDialog(DIALOG_NO_SPACE_LEFT);
		} else if (preferences.getBoolean("noSpaceLeft", false)) {
			showDialog(DIALOG_BACKUP_NO_SPACE_LEFT);
			preferences.edit().putBoolean("noSpaceLeft", false).commit();
		} else if (mCol == null){
			loadCollection();
		}
	}


	protected void sendKey(int keycode) {
		this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,keycode));
		this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,keycode));
	}

	@Override
	protected void onPause() {
		Log.i(AnkiDroidApp.TAG, "DeckPicker - onPause");

        if ((AnkiDroidApp.zeemoteController() != null) && (AnkiDroidApp.zeemoteController().isConnected())){ 
        	Log.d("Zeemote","Removing listener in onPause");
//        	AnkiDroidApp.zeemoteController().removeButtonListener(this);
//        	AnkiDroidApp.zeemoteController().removeJoystickListener(adapter);
//    		adapter.removeButtonListener(this);
    		adapter = null;
        }

		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.i(AnkiDroidApp.TAG, "DeckPicker - onStop");
		super.onStop();
		if (!mDontSaveOnStop) {
			WidgetStatus.update(this);
			if (isFinishing()) {
				DeckTask.launchDeckTask(DeckTask.TASK_TYPE_CLOSE_DECK, mCloseCollectionHandler, new TaskData(mCol));
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(AnkiDroidApp.TAG, "DeckPicker - onDestroy()");
		if (mUnmountReceiver != null) {
			unregisterReceiver(mUnmountReceiver);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		StyledDialog dialog;
		Resources res = getResources();
		StyledDialog.Builder builder = new StyledDialog.Builder(this);

		switch (id) {
		case DIALOG_OK:
			builder.setPositiveButton(R.string.ok, null);
			dialog = builder.create();
			break;

		case DIALOG_NO_SDCARD:
			builder.setMessage("The SD card could not be read. Please, turn off USB storage.");
			builder.setPositiveButton(R.string.ok, null);
			dialog = builder.create();
			break;

		case DIALOG_SELECT_HELP:
			builder.setTitle(res.getString(R.string.help_title));
			builder.setItems(
					new String[] { res.getString(R.string.help_tutorial),
							res.getString(R.string.help_online),
							res.getString(R.string.help_faq) },
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							if (arg1 == 0) {
								createTutorialDeck();
							} else {
								if (Utils.isIntentAvailable(DeckPicker.this,
										"android.intent.action.VIEW")) {
									Intent intent = new Intent(
											"android.intent.action.VIEW",
											Uri.parse(getResources()
													.getString(
															arg1 == 0 ? R.string.link_help
																	: R.string.link_faq)));
									startActivity(intent);
								} else {
									startActivity(new Intent(DeckPicker.this,
											Info.class));
								}
							}
						}

					});
			dialog = builder.create();
			break;

	 	case DIALOG_CONNECTION_ERROR:
			 builder.setTitle(res.getString(R.string.connection_error_title));
			 builder.setIcon(android.R.drawable.ic_dialog_alert);
			 builder.setMessage(res.getString(R.string.connection_error_message));
			 builder.setPositiveButton(res.getString(R.string.retry), new DialogInterface.OnClickListener() {
				 @Override
				 public void onClick(DialogInterface dialog, int which) {
					 sync();
				 }
			 });
			 builder.setNegativeButton(res.getString(R.string.cancel), null);
			 dialog = builder.create();
			 break;

		case DIALOG_SYNC_CONFLICT_RESOLUTION:
			builder.setTitle(res.getString(R.string.sync_conflict_title));
			builder.setIcon(android.R.drawable.ic_input_get);
			builder.setMessage(res.getString(R.string.sync_conflict_message));
			builder.setPositiveButton(res.getString(R.string.sync_conflict_local),
					mSyncConflictResolutionListener);
		 	builder.setNeutralButton(res.getString(R.string.sync_conflict_remote),
		 			mSyncConflictResolutionListener);
		 	builder.setNegativeButton(res.getString(R.string.sync_conflict_cancel),
		 			mSyncConflictResolutionListener);
		 	builder.setCancelable(true);
		 	dialog = builder.create();
		 	break;

			// builder.setTitle(res.getString(R.string.backup_manager_title));
			// builder.setIcon(android.R.drawable.ic_dialog_alert);
			// builder.setPositiveButton(res.getString(R.string.retry), new
			// OnClickListener() {
			//
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// displayProgressDialogAndLoadDeck();
			// }
			// });
			// builder.setNegativeButton(res.getString(R.string.backup_restore), new
			// OnClickListener() {
			//
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// Resources res = getResources();
			// mBackups = BackupManager.getDeckBackups(new File(mDeckFilename));
			// if (mBackups.length == 0) {
			// StyledDialog.Builder builder = new
			// StyledDialog.Builder(StudyOptions.this);
			// builder.setTitle(res.getString(R.string.backup_manager_title))
			// .setIcon(android.R.drawable.ic_dialog_alert)
			// .setMessage(res.getString(R.string.backup_restore_no_backups))
			// .setPositiveButton(res.getString(R.string.ok), new
			// Dialog.OnClickListener() {
			//
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// showDialog(DIALOG_DECK_NOT_LOADED);
			// }
			// }).setCancelable(true).setOnCancelListener(new OnCancelListener() {
			//
			// @Override
			// public void onCancel(DialogInterface arg0) {
			// showDialog(DIALOG_DECK_NOT_LOADED);
			// }
			// }).show();
			// } else {
			// String[] dates = new String[mBackups.length];
			// for (int i = 0; i < mBackups.length; i++) {
			// dates[i] =
			// mBackups[i].getName().replaceAll(".*-(\\d{4}-\\d{2}-\\d{2}).anki",
			// "$1");
			// }
			// StyledDialog.Builder builder = new
			// StyledDialog.Builder(StudyOptions.this);
			// builder.setTitle(res.getString(R.string.backup_restore_select_title))
			// .setIcon(android.R.drawable.ic_input_get)
			// .setSingleChoiceItems(dates, dates.length, new
			// DialogInterface.OnClickListener(){
			//
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// // DeckTask.launchDeckTask(DeckTask.TASK_TYPE_RESTORE_DECK,
			// mRestoreDeckHandler, new DeckTask.TaskData(null, new String[]
			// {mDeckFilename, mBackups[which].getPath()}, 0, 0));
			// dialog.dismiss();
			// }
			// }).setCancelable(true).setOnCancelListener(new OnCancelListener() {
			//
			// @Override
			// public void onCancel(DialogInterface arg0) {
			// showDialog(DIALOG_DECK_NOT_LOADED);
			// }
			// }).show();
			// }
			// }
			// });
			// builder.setNeutralButton(res.getString(R.string.backup_repair_deck),
			// new OnClickListener() {
			//
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// DeckTask.launchDeckTask(DeckTask.TASK_TYPE_REPAIR_DECK,
			// mRepairDeckHandler, new DeckTask.TaskData(mDeckFilename));
			// }
			// });
			// builder.setCancelable(true);

			case DIALOG_LOAD_FAILED:
				 builder.setMessage(res.getString(R.string.open_collection_failed_message, BackupManager.BROKEN_DECKS_SUFFIX, res.getString(R.string.repair_deck)));
				 builder.setTitle(R.string.open_collection_failed_title);
				 builder.setIcon(android.R.drawable.ic_dialog_alert);
				 builder.setPositiveButton(res.getString(R.string.error_handling_options),
					 new DialogInterface.OnClickListener() {
					 	@Override
					 	public void onClick(DialogInterface dialog, int which) {
					 		showDialog(DIALOG_ERROR_HANDLING);
					 	}
				 	});
				 builder.setNegativeButton(res.getString(R.string.close), new DialogInterface.OnClickListener() {
					 	@Override
					 	public void onClick(DialogInterface dialog, int which) {
					 		finish();
					 	}
				 	});
				 builder.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							finish();
						}
				 });
				 dialog = builder.create();
				break;

			case DIALOG_DB_ERROR:
				 builder.setMessage(R.string.answering_error_message);
				 builder.setTitle(R.string.answering_error_title);
				 builder.setIcon(android.R.drawable.ic_dialog_alert);
				 builder.setPositiveButton(res.getString(R.string.error_handling_options),
					 new DialogInterface.OnClickListener() {
					 	@Override
					 	public void onClick(DialogInterface dialog, int which) {
					 		showDialog(DIALOG_ERROR_HANDLING);
					 	}
				 	});
				 builder.setNeutralButton(res.getString(R.string.answering_error_report),
				    new DialogInterface.OnClickListener() {
					 	@Override
					 	public void onClick(DialogInterface dialog, int which) {
					 		Intent i = new Intent(DeckPicker.this, Feedback.class);
			            	i.putExtra("request", RESULT_DB_ERROR);
					 		dialog.dismiss();
					 		startActivityForResult(i, REPORT_ERROR);
					 		if (UIUtils.getApiLevel() > 4) {
					 			ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.RIGHT);
					 		}
					 	}
				 	});
				 builder.setNegativeButton(res.getString(R.string.close), null);
				 builder.setCancelable(true);
				 dialog = builder.create();
			 break;

	 	case DIALOG_ERROR_HANDLING:
	 		builder.setTitle(res.getString(R.string.error_handling_title));
	 		builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setSingleChoiceItems(new String[]{"1"}, 0, null);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (mCol == null) {
						showDialog(DIALOG_LOAD_FAILED);
					} else {
						showDialog(DIALOG_DB_ERROR);
					}
				}
			});
			builder.setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
				 	@Override
				 	public void onClick(DialogInterface dialog, int which) {
						if (mCol == null) {
							showDialog(DIALOG_LOAD_FAILED);
						} else {
							showDialog(DIALOG_DB_ERROR);
						}
				 	}
			 	});
	 		dialog = builder.create();
	 		break;

	 	case DIALOG_USER_NOT_LOGGED_IN_ADD_SHARED_DECK:
			builder.setTitle(res.getString(R.string.connection_error_title));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(res.getString(R.string.no_user_password_error_message));
			builder.setNegativeButton(res.getString(R.string.cancel), null);
			builder.setPositiveButton(res.getString(R.string.log_in),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent myAccount = new Intent(DeckPicker.this, MyAccount.class);
							myAccount.putExtra("notLoggedIn", true);
							startActivityForResult(myAccount, LOG_IN_FOR_SHARED_DECK);
					        if (UIUtils.getApiLevel() > 4) {
					            ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.FADE);
					        }
						}
					});
			dialog = builder.create();			
			break;

	 	case DIALOG_USER_NOT_LOGGED_IN_SYNC:
			builder.setTitle(res.getString(R.string.connection_error_title));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(res.getString(R.string.no_user_password_error_message));
			builder.setNegativeButton(res.getString(R.string.cancel), null);
			builder.setPositiveButton(res.getString(R.string.log_in),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent myAccount = new Intent(DeckPicker.this, MyAccount.class);
							myAccount.putExtra("notLoggedIn", true);
							startActivityForResult(myAccount, LOG_IN_FOR_SYNC);
					        if (UIUtils.getApiLevel() > 4) {
					            ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.FADE);
					        }
						}
					});
			dialog = builder.create();			
			break;


//		case DIALOG_USER_NOT_LOGGED_IN_DOWNLOAD:
//			if (id == DIALOG_USER_NOT_LOGGED_IN_SYNC) {
//			} else {
//				builder.setPositiveButton(res.getString(R.string.log_in),
//						new DialogInterface.OnClickListener() {
//
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								Intent myAccount = new Intent(DeckPicker.this,
//										MyAccount.class);
//								myAccount.putExtra("notLoggedIn", true);
//								startActivityForResult(myAccount, LOG_IN_FOR_DOWNLOAD);
//						        if (UIUtils.getApiLevel() > 4) {
//						            ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.LEFT);
//						        }
//							}
//						});
//			}
//			builder.setNegativeButton(res.getString(R.string.cancel), null);
//			dialog = builder.create();
//			break;

		case DIALOG_NO_CONNECTION:
			builder.setTitle(res.getString(R.string.connection_error_title));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(res.getString(R.string.connection_needed));
			builder.setPositiveButton(res.getString(R.string.ok), null);
			dialog = builder.create();
			break;

		case DIALOG_DELETE_DECK:
			builder.setTitle(res.getString(R.string.delete_deck_title));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(String.format(res.getString(R.string.delete_deck_message), "\'" + mCol.getDecks().name(mCurrentDid) + "\'"));
			builder.setPositiveButton(res.getString(R.string.delete_deck_confirm),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (mCol.getDecks().selected() == mCurrentDid) {
								Fragment frag = (Fragment) getSupportFragmentManager().findFragmentById(R.id.studyoptions_fragment);
								if (frag != null && frag instanceof StudyOptionsFragment) {
									FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
									ft.remove(frag);
									ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
									ft.commit();
								}
							}
							DeckTask.launchDeckTask(DeckTask.TASK_TYPE_DELETE_DECK, new DeckTask.TaskListener() {
								@Override
								public void onPreExecute() {
									mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "",
											getResources().getString(R.string.delete_deck),
											true);
								}
								@Override
								public void onPostExecute(TaskData result) {
									if (result == null) {
										return;
									}
									Object[] res = result.getObjArray();
									updateDecksList((TreeSet<Object[]>) res[0], (Integer)res[1], (Integer)res[2]);
									if (mProgressDialog.isShowing()) {
										try {
											mProgressDialog.dismiss();
										} catch (Exception e) {
											Log.e(AnkiDroidApp.TAG,
													"onPostExecute - Dialog dismiss Exception = "
															+ e.getMessage());
										}
									}
								}
								@Override
								public void onProgressUpdate(TaskData... values) {
								}
							}, new TaskData(mCol, mCurrentDid));
						}
					});
			builder.setNegativeButton(res.getString(R.string.cancel), null);
			dialog = builder.create();
			break;

		case DIALOG_SELECT_STATISTICS_TYPE:
			dialog = ChartBuilder.getStatisticsDialog(this, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_STATISTICS, mLoadStatisticsHandler, new DeckTask.TaskData(mCol, which, true));
				}
				});
			break;

		case DIALOG_CONTEXT_MENU:
			String[] entries = new String[5];
			entries[CONTEXT_MENU_DECK_SUMMARY] = "XXXsum";//res.getStringArray(R.array.statistics_type_labels)[0];
			entries[CONTEXT_MENU_CUSTOM_DICTIONARY] = res.getString(R.string.contextmenu_deckpicker_set_custom_dictionary);
			entries[CONTEXT_MENU_RESET_LANGUAGE] = res.getString(R.string.contextmenu_deckpicker_reset_language_assignments);
			entries[CONTEXT_MENU_RENAME_DECK] = res.getString(R.string.contextmenu_deckpicker_rename_deck);
			entries[CONTEXT_MENU_DELETE_DECK] = res.getString(R.string.contextmenu_deckpicker_delete_deck);
			builder.setTitle("Context Menu");
	        builder.setIcon(R.drawable.ic_menu_manage);
	        builder.setItems(entries, mContextMenuListener);
	        dialog = builder.create();
			break;

		case DIALOG_REPAIR_COLLECTION:
    		builder.setTitle(res.getString(R.string.backup_repair_deck));
    		builder.setMessage(res.getString(R.string.repair_deck_dialog, BackupManager.BROKEN_DECKS_SUFFIX));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
    		builder.setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
			 		DeckTask.launchDeckTask(DeckTask.TASK_TYPE_REPAIR_DECK, mRepairDeckHandler, new DeckTask.TaskData(mCol, mCol.getPath()));
				}
    		});
    		builder.setNegativeButton(res.getString(R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
			 		showDialog(DIALOG_ERROR_HANDLING);
				}
    		});
    		builder.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface arg0) {
			 		showDialog(DIALOG_ERROR_HANDLING);
				}
    			
    		});
			dialog = builder.create();
			break;

		case DIALOG_SYNC_LOG:
			builder.setTitle(res.getString(R.string.sync_log_title));
			builder.setPositiveButton(res.getString(R.string.ok), null);
			dialog = builder.create();
			break;

//			// Upgrade notes dialog
//			builder = new StyledDialog.Builder(this);
//			builder.setTitle(res.getString(
//					R.string.deckpicker_upgrade_notes_title));
//			builder.setPositiveButton(res.getString(R.string.ok), null);
//			mUpgradeNotesAlert = builder.create();
//			builder = new StyledDialog.Builder(this);
//	        builder.setTitle(res.getString(R.string.deckpicker_download_missing_title));
//	        builder.setPositiveButton(res.getString(R.string.ok), null);
//	        mMissingMediaAlert = builder.create();
//	        mProgressDialog = new StyledProgressDialog(DeckPicker.this);
//	        mProgressDialog.setTitle(R.string.deckpicker_download_missing_title);
//	        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//	        mProgressDialog.setMax(100);
//	        mProgressDialog.setCancelable(false);

//	        // backup system restore dialog
//	        builder.setTitle(getResources().getString(R.string.backup_manager_title));
//	        builder.setIcon(android.R.drawable.ic_dialog_alert);
//	        builder.setMessage(getResources().getString(R.string.backup_deck_no_space_left));
//			builder.setPositiveButton(getResources().getString(R.string.ok), null);
//			mNoSpaceLeftAlert = builder.create();
//
//	        builder.setTitle(res.getString(R.string.backup_manager_title));
//	        builder.setIcon(android.R.drawable.ic_dialog_alert);
//	        builder.setPositiveButton(res.getString(R.string.backup_restore), new Dialog.OnClickListener() {
//
//	            @Override
//	            public void onClick(DialogInterface dialog, int which) {
//	            	Resources res = getResources();
//	            	mBackups = BackupManager.getDeckBackups(new File(mCurrentDeckPath));
//	            	if (mBackups.length == 0) {
//	            		StyledDialog.Builder builder = new StyledDialog.Builder(DeckPicker.this);
//	            		builder.setTitle(res.getString(R.string.backup_manager_title))
//	            			.setIcon(android.R.drawable.ic_dialog_alert)
//	            			.setMessage(res.getString(R.string.backup_restore_no_backups))
//	            			.setPositiveButton(res.getString(R.string.ok), new Dialog.OnClickListener() {
//
//					            @Override
//					            public void onClick(DialogInterface dialog, int which) {
//							mDeckNotLoadedAlert.show();
//					            }
//						}).setCancelable(true).setOnCancelListener(new OnCancelListener() {
//
//							@Override
//							public void onCancel(DialogInterface arg0) {
//								mDeckNotLoadedAlert.show();
//							}
//						}).show();
//	            	} else {
//	            		String[] dates = new String[mBackups.length];
//	            		for (int i = 0; i < mBackups.length; i++) {
//	            			dates[i] = mBackups[i].getName().replaceAll(".*-(\\d{4}-\\d{2}-\\d{2}).anki", "$1");
//	            		}
//	            		StyledDialog.Builder builder = new StyledDialog.Builder(DeckPicker.this);
//	            		builder.setTitle(res.getString(R.string.backup_restore_select_title))
//	            			.setIcon(android.R.drawable.ic_input_get)
//	                    	.setSingleChoiceItems(dates, dates.length, new DialogInterface.OnClickListener(){
//
//								@Override
//								public void onClick(DialogInterface dialog, int which) {
//									DeckTask.launchDeckTask(DeckTask.TASK_TYPE_RESTORE_DECK, mRestoreDeckHandler, new DeckTask.TaskData(null, new String[] {mCurrentDeckPath, mBackups[which].getPath()}, 0, 0));
//									dialog.dismiss();
//								}
//							}).setCancelable(true).setOnCancelListener(new OnCancelListener() {
//
//								@Override
//								public void onCancel(DialogInterface arg0) {
//									mDeckNotLoadedAlert.show();
//								}
//							}).show();
//	        		}
//	            }
//	        });
//	        builder.setNeutralButton(res.getString(R.string.backup_repair_deck), new Dialog.OnClickListener() {
//
//				@Override
//				public void onClick(DialogInterface arg0, int arg1) {
//		        	DeckTask.launchDeckTask(DeckTask.TASK_TYPE_REPAIR_DECK, mRepairDeckHandler, new DeckTask.TaskData(mCurrentDeckPath));
//				}
//	        });
//	        builder.setNegativeButton(res.getString(R.string.delete_deck_title), new Dialog.OnClickListener() {
//
//	            @Override
//	            public void onClick(DialogInterface dialog, int which) {
//	            	Resources res = getResources();
//	            	StyledDialog.Builder builder = new StyledDialog.Builder(DeckPicker.this);
//	            	builder.setCancelable(true).setTitle(res.getString(R.string.delete_deck_title))
//	            		.setIcon(android.R.drawable.ic_dialog_alert)
//	            		.setMessage(String.format(res.getString(R.string.delete_deck_message), "\'" + new File(mCurrentDeckPath).getName().replace(".anki", "") + "\'"))
//	            		.setPositiveButton(res.getString(R.string.delete_deck_confirm), new DialogInterface.OnClickListener() {
//
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								if (BackupManager.moveDeckToBrokenFolder(mCurrentDeckPath)) {
//									Themes.showThemedToast(DeckPicker.this, getResources().getString(R.string.delete_deck_success, "\'" + (new File(mCurrentDeckPath).getName().replace(".anki", "")) + "\'", "\'" + BackupManager.BROKEN_DECKS_SUFFIX.replace("/", "") + "\'"), false);								
//									mRestoredOrDeleted = true;
//									handleRestoreDecks(true);
//								}
//							}
//						}).setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								mDeckNotLoadedAlert.show();
//							}
//						}).setOnCancelListener(new DialogInterface.OnCancelListener() {
//
//							@Override
//							public void onCancel(DialogInterface dialog) {
//								mDeckNotLoadedAlert.show();
//							}
//						}).show();					
//	            }
//	        });
//	        builder.setCancelable(true);
//	        builder.setOnCancelListener(new OnCancelListener() {
//
//				@Override
//				public void onCancel(DialogInterface arg0) {
//					mAlreadyDealtWith.add(mCurrentDeckPath);
//					handleRestoreDecks(true);
//				}
//			});
//	        mDeckNotLoadedAlert = builder.create();

		case DIALOG_BACKUP_NO_SPACE_LEFT:
			builder.setTitle(res.getString(R.string.attention));
			builder.setMessage(res.getString(R.string.backup_deck_no_space_left));
			builder.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					loadCollection();
				}
			});
//        	builder.setNegativeButton(res.getString(R.string.dont_show_again), new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface arg0, int arg1) {
//		            	PrefSettings.getSharedPrefs(getBaseContext()).edit().putBoolean("dontShowLowMemory", true).commit();
//					}
//		        });
			builder.setCancelable(true);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					loadCollection();
				}				
			});
			dialog = builder.create();
			break;

		case DIALOG_SD_CARD_NOT_MOUNTED:
		case DIALOG_NO_SPACE_LEFT:
			builder.setTitle(res.getString(R.string.attention));
			if (id == DIALOG_NO_SPACE_LEFT) {
				builder.setMessage(res.getString(R.string.sd_space_warning, BackupManager.MIN_FREE_SPACE));				
			} else {
				builder.setMessage(res.getString(R.string.sd_card_not_mounted));
			}
			builder.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
//        	builder.setNegativeButton(res.getString(R.string.dont_show_again), new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface arg0, int arg1) {
//		            	PrefSettings.getSharedPrefs(getBaseContext()).edit().putBoolean("dontShowLowMemory", true).commit();
//					}
//		        });
			builder.setCancelable(true);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					finish();
				}
			});
			dialog = builder.create();
			break;

//		case DIALOG_RESTORE_BACKUP:
//			 mBackups = BackupManager.getDeckBackups(new File(mDeckFilename));
//			 if (mBackups.length == 0) {
//	 	    		StyledDialog d = (StyledDialog) onCreateDialog(DIALOG_OK);
//	 	    		d.setTitle(getResources().getString(R.string.backup_restore));
//	 	    		d.setMessage(res.getString(R.string.backup_restore_no_backups));
//	 	    		d.setPositiveButton(res.getString(R.string.ok), new
//	 						 Dialog.OnClickListener() {
//	 					 		@Override
//	 					 		public void onClick(DialogInterface dialog, int which) {
//	 					 			showDialog(DIALOG_ERROR_HANDLING);
//	 					 		}
//	 			 			});
//	 	    		d.setCancelable(true).setOnCancelListener(new OnCancelListener() {
//	 			 				@Override
//	 			 				public void onCancel(DialogInterface arg0) {
//	 			 					showDialog(DIALOG_ERROR_HANDLING);
//	 			 				}
//	 			 			});
//	 	    		d.show();
//			 } else {
//				 String[] dates = new String[mBackups.length];
//				 for (int i = 0; i < mBackups.length; i++) {
//					 dates[i] = mBackups[i].getName().replaceAll(".*-(\\d{4}-\\d{2}-\\d{2}).anki2", "$1");
//				 }
//			 StyledDialog.Builder builder = new
//			 StyledDialog.Builder(StudyOptions.this);
//			 builder.setTitle(res.getString(R.string.backup_restore_select_title))
//			 .setIcon(android.R.drawable.ic_input_get)
//			 .setSingleChoiceItems(dates, dates.length, new
//			 DialogInterface.OnClickListener(){
//			
//			 @Override
//			 public void onClick(DialogInterface dialog, int which) {
//			 // DeckTask.launchDeckTask(DeckTask.TASK_TYPE_RESTORE_DECK,
//			 mRestoreDeckHandler, new DeckTask.TaskData(null, new String[]
//			 {mDeckFilename, mBackups[which].getPath()}, 0, 0));
//			 dialog.dismiss();
//			 }
//			 }).setCancelable(true).setOnCancelListener(new OnCancelListener() {
//			
//			 @Override
//			 public void onCancel(DialogInterface arg0) {
//			 showDialog(DIALOG_DECK_NOT_LOADED);
//			 }
//			 }).show();
//			 }
//			break;

		default:
			dialog = null;
		}
		if (dialog != null) {
			dialog.setOwnerActivity(this);			
		}
		return dialog;
	}


	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		Resources res = getResources();
		StyledDialog ad = (StyledDialog)dialog;
		switch (id) {
		case DIALOG_DELETE_DECK:
			mCurrentDid = Long.parseLong(mDeckList.get(mContextMenuPosition).get("did"));
			ad.setMessage(String.format(res.getString(R.string.delete_deck_message), "\'" + mCol.getDecks().name(mCurrentDid) + "\'"));
			break;

		case DIALOG_CONTEXT_MENU:
			mCurrentDid = Long.parseLong(mDeckList.get(mContextMenuPosition).get("did"));
			ad.setTitle(mCol.getDecks().name(mCurrentDid));
			break;

		case DIALOG_SYNC_LOG:
			ad.setMessage(mDialogMessage);
			break;

		case DIALOG_DB_ERROR:
			ad.getButton(Dialog.BUTTON3).setEnabled(hasErrorFiles());
			break;

		case DIALOG_ERROR_HANDLING:
	 		ArrayList<String> options = new ArrayList<String>();
	 		ArrayList<Integer> values = new ArrayList<Integer>();
	 		if (mCol == null) {
		 		// retry
		 		options.add(res.getString(R.string.backup_retry_opening));
		 		values.add(0);
	 		} else {
		 		// fix integrity
		 		options.add(res.getString(R.string.check_db));
		 		values.add(1);
	 		}
	 		// repair db with sqlite
	 		options.add(res.getString(R.string.backup_error_menu_repair));
	 		values.add(2);
//	 		// restore from backup
//	 		options.add(res.getString(R.string.backup_restore));
//	 		values.add(3);
//	 		// delete old collection and build new one
//	 		options.add(res.getString(R.string.backup_new_collection));
//	 		values.add(4);

	 		String[] titles = new String[options.size()];
	 		mRepairValues = new int[options.size()];
	 		for (int i = 0; i < options.size(); i++) {
	 			titles[i] = options.get(i);
	 			mRepairValues[i] = values.get(i);
	 		}
	 		ad.setSingleChoiceItems(titles, 0, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (mRepairValues[which]) {
					case 0:
						loadCollection();
						return;
					case 1:
						integrityCheck();
						return;
					case 2:
						showDialog(DIALOG_REPAIR_COLLECTION);
						return;
					case 3:
						showDialog(DIALOG_RESTORE_BACKUP);
						return;
					case 4:
//						TODO: asdf
						return;
					}
				}
			});
	 		break;
		}
	}


    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	mContextMenuPosition = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
    	showDialog(DIALOG_CONTEXT_MENU);
	}


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	Log.i(AnkiDroidApp.TAG, "DeckPicker - onBackPressed()");
        	finish();
            if (UIUtils.getApiLevel() > 4) {
                ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.NONE);
            }
        	return true;
        }
        return super.onKeyDown(keyCode, event);
    }

	// ----------------------------------------------------------------------------
	// CUSTOM METHODS
	// ----------------------------------------------------------------------------

	/**
	 * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
	 * intent will call closeExternalStorageFiles() if the external media is
	 * going to be ejected, so applications can clean up any files they have
	 * open.
	 */
	private void registerExternalStorageListener() {
		if (mUnmountReceiver == null) {
			mUnmountReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
						if (mCol != null) {
							mCol.close();
						}
			        	showDialog(DIALOG_SD_CARD_NOT_MOUNTED);
					} else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
			        	showDialog(DIALOG_LOAD_FAILED);
					}

		        	showDialog(DIALOG_SD_CARD_NOT_MOUNTED);
				}
			};
			IntentFilter iFilter = new IntentFilter();

			// ACTION_MEDIA_EJECT is never invoked (probably due to an android bug
			iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
			iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
			iFilter.addDataScheme("file");
			registerReceiver(mUnmountReceiver, iFilter);
		}
	}

    /**
     * Creates an intent to load a deck given the full pathname of it.
     * The constructed intent is equivalent (modulo the extras) to the open used by the launcher
     * shortcut, which means it will not open a new study options window but bring the existing one
     * to the front.
     */
    public static Intent getLoadDeckIntent(Context context, long deckId) {
        Intent loadDeckIntent = new Intent(context, DeckPicker.class);
        loadDeckIntent.setAction(Intent.ACTION_MAIN);
        loadDeckIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        loadDeckIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        loadDeckIntent.putExtra(EXTRA_DECK_ID, deckId);
        return loadDeckIntent;
    }


//	private void handleRestoreDecks(boolean reloadIfEmpty) {
//		if (mBrokenDecks.size() != 0) {
//			while (true) {
//				mCurrentDeckPath = mBrokenDecks.remove(0);
//				if (!mAlreadyDealtWith.contains(mCurrentDeckPath) || mBrokenDecks.size() == 0) {
//					break;
//				}
//			}
//        	mDeckNotLoadedAlert.setMessage(getResources().getString(R.string.open_deck_failed, "\'" + new File(mCurrentDeckPath).getName() + "\'", BackupManager.BROKEN_DECKS_SUFFIX.replace("/", ""), getResources().getString(R.string.repair_deck)));
//			mDeckNotLoadedAlert.show();
//		} else if (reloadIfEmpty) {
//			if (mRestoredOrDeleted) {
//				mBrokenDecks = new ArrayList<String>();
////				populateDeckList(mPrefDeckPath);
//			}
//		}
//	}


	private void sync() {
		sync(null, 0);
	}
	/**
	 * The mother of all syncing attempts.
	 * This might be called from sync() as first attempt to sync a collection OR from
	 * the mSyncConflictResolutionListener if the first attempt determines that a full-sync
	 * is required. In the second case, we have passed the mediaUsn that was obtained during
	 * the first attempt.
	 * @param syncConflictResolution Either "upload" or "download", depending on the user's choice.
	 * @param syncMediaUsn The media Usn, as determined during the prior sync() attempt that
	 * determined that full syncing was required.
	 */
	private void sync(String syncConflictResolution, int syncMediaUsn) {
		if (mCol != null) {
			SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
			String hkey = preferences.getString("hkey", "");
			if (hkey.length() == 0) {
				showDialog(DIALOG_USER_NOT_LOGGED_IN_SYNC);
			} else {
				mSyncButton.setClickable(false);
				Connection.sync(mSyncListener, new Connection.Payload(new Object[] { hkey, true, syncConflictResolution, syncMediaUsn}));
			}
		}
	}

	private void addCramDeck() {
        if (mFragmented) {
//          getListView().setItemChecked(index, true);
        	
			Fragment frag = (Fragment) getSupportFragmentManager().findFragmentById(R.id.studyoptions_fragment);
			if (!(frag instanceof CramDeckFragment)) {
				CramDeckFragment details = CramDeckFragment.newInstance(CRAM_DECK_FRAGMENT);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.studyoptions_fragment, details);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.commit();
			}
        } else {
    		mDontSaveOnStop = true;
        	Intent intent = new Intent();
        	intent.putExtra("index", CRAM_DECK_FRAGMENT);
        	intent.setClass(this, CramDeckActivity.class);
    		startActivityForResult(intent, ADD_CRAM_DECK);
//    		if (deckId != 0) {
//    			if (UIUtils.getApiLevel() > 4) {
//        			ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.NONE);
//    			}
//    		} else {
    			if (UIUtils.getApiLevel() > 4) {
        			ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.LEFT);
    			}			
//    		}
        }
	}

	private void addSharedDeck() {
		Intent intent = new Intent(DeckPicker.this, Info.class);
		intent.putExtra(Info.TYPE_EXTRA, Info.TYPE_SHARED_DECKS);
	    startActivityForResult(intent, ADD_SHARED_DECKS);
	    if (UIUtils.getApiLevel() > 4) {
	        ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.RIGHT);
	    }		
	}

    private DialogInterface.OnClickListener mSyncConflictResolutionListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    sync("upload", mSyncMediaUsn);
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    sync("download", mSyncMediaUsn);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                default:
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item;

        if (mFragmented) {
    		UIUtils.addMenuItemInActionBar(menu, Menu.NONE, MENU_SYNC, Menu.NONE,
    				R.string.sync_title, R.drawable.ic_menu_refresh);

    		UIUtils.addMenuItemInActionBar(menu, Menu.NONE, MENU_ADD_NOTE, Menu.NONE,
    				R.string.add, R.drawable.ic_menu_add);

    		UIUtils.addMenuItemInActionBar(menu, Menu.NONE, MENU_STATISTICS, Menu.NONE,
    				R.string.statistics_menu, R.drawable.ic_menu_statistics);

    		UIUtils.addMenuItemInActionBar(menu, Menu.NONE, MENU_CARDBROWSER, Menu.NONE,
    				R.string.menu_cardbrowser, R.drawable.ic_menu_cardbrowser);        	
        }
		UIUtils.addMenuItemInActionBar(menu, Menu.NONE, MENU_HELP, Menu.NONE,
				R.string.help_title, R.drawable.ic_menu_help);

		item = menu.add(Menu.NONE, MENU_CREATE_DECK, Menu.NONE, R.string.new_deck);
        item.setIcon(R.drawable.ic_menu_add);
        item = menu.add(Menu.NONE, MENU_ADD_SHARED_DECK, Menu.NONE, R.string.menu_get_shared_decks);
        item.setIcon(R.drawable.ic_menu_download);
        item = menu.add(Menu.NONE, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
        item.setIcon(R.drawable.ic_menu_preferences);
        item = menu.add(Menu.NONE, MENU_MY_ACCOUNT, Menu.NONE, R.string.menu_my_account);
        item.setIcon(R.drawable.ic_menu_home);
        UIUtils.addMenuItem(menu, Menu.NONE, CHECK_DATABASE, Menu.NONE, R.string.check_db, R.drawable.ic_menu_search);
        item = menu.add(Menu.NONE, MENU_FEEDBACK, Menu.NONE, R.string.studyoptions_feedback);
        item.setIcon(R.drawable.ic_menu_send);
        item = menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.menu_about);
        item.setIcon(R.drawable.ic_menu_info_details);
        item = menu.add(Menu.NONE, StudyOptionsActivity.MENU_ROTATE, Menu.NONE, R.string.menu_rotate);
        item.setIcon(R.drawable.ic_menu_always_landscape_portrait);
        
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean sdCardAvailable = AnkiDroidApp.isSdCardMounted();
//        menu.findItem(SUBMENU_DOWNLOAD).setEnabled(sdCardAvailable);
//        menu.findItem(MENU_DOWNLOAD_PERSONAL_DECK).setVisible(sdCardAvailable);
        return true;
    }


    /** Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Resources res = getResources();

        switch (item.getItemId()) {

        	case MENU_HELP:
        		showDialog(DIALOG_SELECT_HELP);
        		return true;

        	case MENU_SYNC:
            	sync();
        		return true;

        	case MENU_ADD_NOTE:
        		addNote();
        		return true;

        	case MENU_STATISTICS:
				showDialog(DIALOG_SELECT_STATISTICS_TYPE);
        		return true;

        	case MENU_CARDBROWSER:
        		openCardBrowser();
        		return true;

            case MENU_CREATE_DECK:
				StyledDialog.Builder builder2 = new StyledDialog.Builder(DeckPicker.this);
				builder2.setTitle(res.getString(R.string.new_deck));

				mDialogEditText = (EditText) new EditText(DeckPicker.this);
//				mDialogEditText.setFilters(new InputFilter[] { mDeckNameFilter });
				builder2.setView(mDialogEditText, false, false);
				builder2.setPositiveButton(res.getString(R.string.create),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								String deckName = mDialogEditText.getText().toString().replaceAll("[\'\"\\s\\n\\r\\[\\]\\(\\)]", "");
								Log.i(AnkiDroidApp.TAG, "Creating deck: " + deckName);
								mCol.getDecks().id(deckName, true);
								loadCounts();
							}
						});
				builder2.setNegativeButton(res.getString(R.string.cancel), null);
				builder2.create().show();
                return true;

            case MENU_ABOUT:
                startActivity(new Intent(DeckPicker.this, Info.class));
                if (UIUtils.getApiLevel() > 4) {
                    ActivityTransitionAnimation.slide(DeckPicker.this, ActivityTransitionAnimation.RIGHT);
                }
                return true;

            case MENU_ADD_SHARED_DECK:
        		if (mCol != null) {
        			SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
        			String hkey = preferences.getString("hkey", "");
        			if (hkey.length() == 0) {
        				showDialog(DIALOG_USER_NOT_LOGGED_IN_ADD_SHARED_DECK);
        			} else {
        				addSharedDeck();
        			}
        		}
                return true;

            case MENU_MY_ACCOUNT:
                startActivity(new Intent(DeckPicker.this, MyAccount.class));
                return true;

            case MENU_PREFERENCES:
                startActivityForResult(
                        new Intent(DeckPicker.this, Preferences.class),
                        PREFERENCES_UPDATE);
                return true;

            case MENU_FEEDBACK:
            	Intent i = new Intent(DeckPicker.this, Feedback.class);
            	i.putExtra("request", REPORT_FEEDBACK);
                startActivityForResult(i, REPORT_FEEDBACK);
    			if (UIUtils.getApiLevel() > 4) {
    				ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.RIGHT);
    			}
                return true;

            case CHECK_DATABASE:
            	integrityCheck();
            	return true;

            case StudyOptionsActivity.MENU_ROTATE:
            	if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
    				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    			} else {
    				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    			}
            	return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

		mDontSaveOnStop = false;
		if (resultCode == RESULT_MEDIA_EJECTED) {
			showDialog(DIALOG_SD_CARD_NOT_MOUNTED);
			return;
		} else if (resultCode == RESULT_DB_ERROR) {
        	handleDbError();
        	return;
        }
    	if (requestCode == SHOW_STUDYOPTIONS && resultCode == RESULT_OK) {
    		loadCounts();
    	} else if (requestCode == ADD_NOTE && resultCode != RESULT_CANCELED) {
    		loadCounts();
        } else if (requestCode == REPORT_ERROR) {
        	showStartupScreensAndDialogs(PrefSettings.getSharedPrefs(getBaseContext()), 3);
        } else if (requestCode == SHOW_INFO_WELCOME || requestCode == SHOW_INFO_NEW_VERSION) {
    		if (resultCode == RESULT_OK) {
    			showStartupScreensAndDialogs(PrefSettings.getSharedPrefs(getBaseContext()), requestCode == SHOW_INFO_WELCOME ? 1 : 2);
    		} else {
    			finish();
    		}
        } else if (requestCode == PREFERENCES_UPDATE) {
//                if (resultCode == StudyOptions.RESULT_RESTART) {
//                	setResult(StudyOptions.RESULT_RESTART);
//                	finish();
//                } else {
//                	SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
//    				BackupManager.initBackup();
//                    if (!mPrefDeckPath.equals(preferences.getString("deckPath", AnkiDroidApp.getStorageDirectory())) || mPrefDeckOrder != Integer.parseInt(preferences.getString("deckOrder", "0"))) {
////                    	populateDeckList(preferences.getString("deckPath", AnkiDroidApp.getStorageDirectory()));
//                    }
//                }
        } else if ((requestCode == CREATE_DECK || requestCode == DOWNLOAD_SHARED_DECK) && resultCode == RESULT_OK) {
//            	populateDeckList(mPrefDeckPath);
        } else if (requestCode == REPORT_FEEDBACK && resultCode == RESULT_OK) {
        } else if (requestCode == LOG_IN_FOR_SYNC && resultCode == RESULT_OK) {
        	sync();
        } else if (requestCode == LOG_IN_FOR_SHARED_DECK && resultCode == RESULT_OK) {
        	addSharedDeck();
        } else if (requestCode == ADD_SHARED_DECKS) {
        	sync();
        }

    	// workaround for hidden dialog on return
		BroadcastMessages.showDialog();
    }

    private void integrityCheck() {
		DeckTask.launchDeckTask(DeckTask.TASK_TYPE_CHECK_DATABASE, new DeckTask.TaskListener() {
			@Override
			public void onPreExecute() {
				mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "",
						getResources().getString(R.string.check_db_message),
						true);
			}
			@Override
			public void onPostExecute(TaskData result) {
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				if (result.getBoolean()) {
    	    		StyledDialog dialog = (StyledDialog) onCreateDialog(DIALOG_OK);
    	    		dialog.setTitle(getResources().getString(R.string.check_db_title));
    	    		dialog.setMessage(String.format(Utils.ENGLISH_LOCALE, getResources().getString(R.string.check_db_result_message), Math.round(result.getLong() / 1024)));
    	    		dialog.show();
				} else {
					showDialog(DIALOG_DB_ERROR);
				}
			}
			@Override
			public void onProgressUpdate(TaskData... values) {
			}
		}, new DeckTask.TaskData(mCol));
    }

	private void resetDeckLanguages(String deckPath) {
		if (MetaDB.resetDeckLanguages(this, deckPath)) {
			Themes.showThemedToast(this, getResources().getString(R.string.contextmenu_deckpicker_reset_reset_message), true);
		}
	}


    public void openSharedDeckPicker() {
//        // deckLoaded = false;
//        startActivityForResult(new Intent(this, SharedDeckPicker.class), DOWNLOAD_SHARED_DECK);
//        if (UIUtils.getApiLevel() > 4) {
//            ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.RIGHT);
//        }
    }


    public void handleDbError() {
    	DeckTask.launchDeckTask(DeckTask.TASK_TYPE_RESTORE_IF_MISSING, new DeckTask.TaskListener() {
			@Override
			public void onPreExecute() {
				mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "",
						getResources().getString(R.string.backup_restore_if_missing),
						true);
			}
			@Override
			public void onPostExecute(TaskData result) {
				if (mProgressDialog.isShowing()) {
					try {
						mProgressDialog.dismiss();
					} catch (Exception e) {
						Log.e(AnkiDroidApp.TAG,
								"onPostExecute - Dialog dismiss Exception = "
										+ e.getMessage());
					}
				}					
		    	showDialog(DIALOG_DB_ERROR);
			}
			@Override
			public void onProgressUpdate(TaskData... values) {
			}
		}, new DeckTask.TaskData(AnkiDroidApp.getCollectionPath()));
    }


    private void openStudyOptions() {
		openStudyOptions(-1);
	}
	private void openStudyOptions(int id) {
        if (mFragmented) {
//          getListView().setItemChecked(index, true);
			Fragment frag = (Fragment) getSupportFragmentManager().findFragmentById(R.id.studyoptions_fragment);
			if (frag == null || !(frag instanceof StudyOptionsFragment) || ((StudyOptionsFragment) frag).getShownIndex() != id) {
				StudyOptionsFragment details = StudyOptionsFragment.newInstance(id);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.studyoptions_fragment, details);
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				ft.commit();
			}
        } else {
    		mDontSaveOnStop = true;
        	Intent intent = new Intent();
        	intent.putExtra("index", id);
        	intent.setClass(this, StudyOptionsActivity.class);
//    		if (deckId != 0) {
//    			intent.putExtra(EXTRA_DECK_ID, deckId);
//    		}
    		startActivityForResult(intent, SHOW_STUDYOPTIONS);
//    		if (deckId != 0) {
//    			if (UIUtils.getApiLevel() > 4) {
//        			ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.NONE);
//    			}
//    		} else {
    			if (UIUtils.getApiLevel() > 4) {
        			ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.LEFT);
    			}			
//    		}
        }
	}

	private void handleDeckSelection(int id) {
		String deckFilename = null;

		@SuppressWarnings("unchecked")
		HashMap<String, String> data = (HashMap<String, String>) mDeckListAdapter.getItem(id);
		Log.i(AnkiDroidApp.TAG, "Selected " + deckFilename);
		mCol.getDecks().select(Long.parseLong(data.get("did")));
		openStudyOptions(id);
	}


	private void createTutorialDeck() {
		DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_TUTORIAL, new DeckTask.TaskListener() {
			@Override
			public void onPreExecute() {
				mProgressDialog = StyledProgressDialog.show(DeckPicker.this, "",
						getResources().getString(R.string.tutorial_load),
						true);
			}
			@Override
			public void onPostExecute(TaskData result) {
				if (result.getBoolean()) {
					loadCounts();
					openStudyOptions();
				} else {
					Themes.showThemedToast(DeckPicker.this, getResources().getString(R.string.tutorial_loading_error), false);
				}
				if (mProgressDialog.isShowing()) {
					try {
						mProgressDialog.dismiss();
					} catch (Exception e) {
						Log.e(AnkiDroidApp.TAG,
								"onPostExecute - Dialog dismiss Exception = "
										+ e.getMessage());
					}
				}					
			}
			@Override
			public void onProgressUpdate(TaskData... values) {
			}
		}, new DeckTask.TaskData(mCol));
	}

	private CharSequence getSyncLogMessage(
			ArrayList<HashMap<String, String>> decksChangelogs) {
		SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
		int len = decksChangelogs.size();
		for (int i = 0; i < len; i++) {
			HashMap<String, String> deckChangelog = decksChangelogs.get(i);
			String deckName = deckChangelog.get("deckName");

			// Append deck name
			spannableStringBuilder.append(deckName);
			// Underline deck name
			spannableStringBuilder.setSpan(new UnderlineSpan(),
					spannableStringBuilder.length() - deckName.length(),
					spannableStringBuilder.length(), 0);
			// Put deck name in bold style
			spannableStringBuilder.setSpan(new StyleSpan(
					android.graphics.Typeface.BOLD), spannableStringBuilder
					.length()
					- deckName.length(), spannableStringBuilder.length(), 0);

			// Append sync message
			spannableStringBuilder.append("\n" + deckChangelog.get("message"));

			// If it is not the last element, add the proper separation
			if (i != (len - 1)) {
				spannableStringBuilder.append("\n\n");
			}
		}

		return spannableStringBuilder;
	}


	private void updateDecksList(TreeSet<Object[]> decks, int eta, int count) {
		mDeckList.clear();
		int due = 0;
        for (Object[] d : decks) {
        	HashMap<String, String> m = new HashMap<String, String>();
        	String[] name = ((String[])d[0]);
        	m.put("name", readableDeckName(name));
        	m.put("did", ((Long)d[1]).toString());
        	m.put("new", ((Integer)d[2]).toString());
        	m.put("lrn", ((Integer)d[3]).toString());
        	m.put("rev", ((Integer)d[4]).toString());
//        	m.put("complMat", ((Float)d[5]).toString());
//        	m.put("complAll", ((Float)d[6]).toString());
        	if (name.length == 1) {
        		due += Integer.parseInt(m.get("new")) + Integer.parseInt(m.get("lrn")) + Integer.parseInt(m.get("rev"));
        		// top position
        		m.put("sep", "top");
        		// correct previous deck
        		if (mDeckList.size() > 0) {
        			HashMap<String, String> map = mDeckList.get(mDeckList.size() - 1);
        			if (map.get("sep").equals("top")) {
        				map.put("sep", "ful");
        			} else {
        				map.put("sep", "bot");
        			}
        		}
        	} else {
        		// center position
        		m.put("sep", "cen");
        	}
        	if (mDeckList.size() > 0 && mDeckList.size() == decks.size() - 1) {
        		// bottom position
        		if (name.length == 1) {
        			m.put("sep", "ful");
        		} else {
        			m.put("sep", "bot");
        		}        		
        	}
        	mDeckList.add(m);
        }
        mDeckListAdapter.notifyDataSetChanged();

        // set title
        Resources res = getResources();
        if (count != -1) {
            String time = "-"; 
        	if (eta != -1) {
        		time = res.getQuantityString(R.plurals.deckpicker_title_minutes, eta, eta);	
        	}
            UIUtils.setActionBarSubtitle(this, res.getQuantityString(R.plurals.deckpicker_title, due, due, count, time));
        } else {
    		setTitle(res.getString(R.string.app_name));
        }
	}

//	private void restartApp() {
//		// restarts application in order to apply new themes or localisations
//		Intent i = getBaseContext().getPackageManager()
//				.getLaunchIntentForPackage(getBaseContext().getPackageName());
//		mCompat.invalidateOptionsMenu(this);
//		MetaDB.closeDB();
//		StudyOptions.this.finish();
//		startActivity(i);
//	}

    // ----------------------------------------------------------------------------
	// INNER CLASSES
	// ----------------------------------------------------------------------------


//    private class ThemedAdapter extends SimpleAdapter {
//    	    public ThemedAdapter(Context context, ArrayList<HashMap<String, String>> items, int resource, String[] from, int[] to) {
//    	        super(context, items, resource, from, to);
//    	    }
//
////    	    @Override
////    	    public View getView(int position, View convertView, ViewGroup parent) {
////	    	  View view = super.getView(position, convertView, parent);
////	    	  Themes.setContentStyle(view, Themes.CALLER_DECKPICKER_DECK);
////    	      return view;
////    	    }
//    }


	class MyGestureDetector extends SimpleOnGestureListener {	
    	@Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mSwipeEnabled) {
                try {
       				if (e1.getX() - e2.getX() > sSwipeMinDistance && Math.abs(velocityX) > sSwipeThresholdVelocity && Math.abs(e1.getY() - e2.getY()) < sSwipeMaxOffPath) {
       					openStudyOptions();
                    }
       			}
                catch (Exception e) {
                  	Log.e(AnkiDroidApp.TAG, "onFling Exception = " + e.getMessage());
                }
            }	            	
            return false;
    	}
    }

//	@Override
//	public void buttonPressed(ButtonEvent arg0) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void buttonReleased(ButtonEvent arg0) {
//		Log.d("Zeemote","Button released, id: "+arg0.getButtonID());
//		Message msg = Message.obtain();
//		msg.what = MSG_ZEEMOTE_BUTTON_A + arg0.getButtonID(); //Button A = 0, Button B = 1...
//		if ((msg.what >= MSG_ZEEMOTE_BUTTON_A) && (msg.what <= MSG_ZEEMOTE_BUTTON_D)) { //make sure messages from future buttons don't get throug
//			this.ZeemoteHandler.sendMessage(msg);
//		}
//		if (arg0.getButtonID()==-1)
//		{
//			msg.what = MSG_ZEEMOTE_BUTTON_D+arg0.getButtonGameAction();
//			if ((msg.what >= MSG_ZEEMOTE_STICK_UP) && (msg.what <= MSG_ZEEMOTE_STICK_RIGHT)) { //make sure messages from future buttons don't get throug
//				this.ZeemoteHandler.sendMessage(msg);
//			}
//		}
//	}

//	      if ((AnkiDroidApp.zeemoteController() != null) && (AnkiDroidApp.zeemoteController().isConnected())){
//	    	  Log.d("Zeemote","Adding listener in onResume");
//	    	  AnkiDroidApp.zeemoteController().addButtonListener(this);
//	      	  adapter = new JoystickToButtonAdapter();
//	      	  AnkiDroidApp.zeemoteController().addJoystickListener(adapter);
//	      	  adapter.addButtonListener(this);
//	      }
//	}

//	@Override
//	protected void onNewIntent(Intent intent) {
//		super.onNewIntent(intent);
//		String deck = intent.getStringExtra(EXTRA_DECK);
//		Log.d(AnkiDroidApp.TAG, "StudyOptions.onNewIntent: " + intent
//				+ ", deck=" + deck);
//		// if (deck != null && !deck.equals(mDeckFilename)) {
//		// mDeckFilename = deck;
//		// // loadPreviousDeck();
//		// }
//	}

    public static String readableDeckName(String[] name) {
    	int len = name.length;
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < len; i++) {
    		if (i == len - 1) {
    			sb.append(name[i]);
    		} else if (i == len - 2) {
    			sb.append("\u21aa");
    		} else {
    			sb.append("    ");
    		}
    	}
    	return sb.toString();
    }


//    private InputFilter mDeckNameFilter = new InputFilter() {
//		public CharSequence filter(CharSequence source, int start,
//				int end, Spanned dest, int dstart, int dend) {
//			for (int i = start; i < end; i++) {
//				if (!Character.isLetterOrDigit(source.charAt(i))) {
//					char comp = source.charAt(i);
//					if (comp == ' ') {
//						return "";
//					}
//					boolean forbidden = true;
//					for (char c : new char[]{':', '+', '-', '!', '_'}) {
//						if (c == comp) {
//							forbidden = false;
//							break;
//						}
//					}
//					if (forbidden) {
//						return "";						
//					}
//				}
//			}
//			return null;
//		}
//	};

}

