/****************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General private License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General private License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General private License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.libanki;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.ichi2.anki.AnkiDatabaseManager;
import com.ichi2.anki.AnkiDb;
import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.UIUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

// Anki maintains a cache of used tags so it can quickly present a list of tags
// for autocomplete and in the browser. For efficiency, deletions are not
// tracked, so unused tags can only be removed from the list with a DB check.
//
// This module manages the tag cache and tags for notes.

public class Collection {

	private AnkiDb mDb;
	private boolean mServer;
	private double mLastSave;
	private Media mMedia;
	private Decks mDecks;
	private Models mModels;
	private Tags mTags;

	private Sched mStdSched;
	private Sched mSched;

	private int mSessionStartReps;
	private int mSessionRepLimit;
	private double mSessionStartTime;
	private double mLastSessionStart;
	private double mSessionTimeLimit;
	private boolean mUndoEnabled;

	private int mRepsToday;

	// BEGIN: SQL table columns
	private long mCrt;
	private long mMod;
	private long mScm;
	private boolean mDty;
	private int mUsn;
	private long mLs;
	private JSONObject mConf;
	// END: SQL table columns

	private Object[] mUndo;

	private String mPath;
	private boolean mClosing = false;

	// other options
	private static final String defaultConf = "{"
			+
			// review options
			"'activeDecks': [1], " + "'curDeck': 1, " + "'newSpread': "
			+ Sched.NEW_CARDS_DISTRIBUTE + ", " + "'collapseTime': 1200, "
			+ "'timeLim': 0, " + "'estTimes': True, " + "'dueCounts': True, "
			+
			// other config
			"'curModel': None, " + "'nextPos': 1, "
			+ "'sortType': \"noteFld\", " + "'sortBackwards': False, }";

	private static Collection sCurrentCollection;

	public static synchronized Collection openCollection(String path) {
		AnkiDb ankiDB = AnkiDatabaseManager.getDatabase(path);
		sCurrentCollection = new Collection(ankiDB, path);
		return sCurrentCollection;
	}

	public Collection(AnkiDb db, String path) {
		this(db, path, false);
	}

	public Collection(AnkiDb db, String path, boolean server) {
		mDb = db;
		mServer = server;
		mLastSave = Utils.now();
		clearUndo();
		mPath = path;
		mMedia = new Media(this);
		mModels = new Models(this);
		mDecks = new Decks(this);
		mTags = new Tags(this);
		load();
		if (mCrt == 0) {
			mCrt = UIUtils.getDayStart() / 1000;
		}
		mUndoEnabled = false;
		mSessionStartReps = 0;
		mSessionStartTime = 0;
		mLastSessionStart = 0;
		mStdSched = new Sched(this);
		mSched = mStdSched;
		// check for improper shutdown
		cleanup();
	}

	public static Collection currentCollection() {
		if (sCurrentCollection == null || sCurrentCollection.mClosing || sCurrentCollection.mDb == null) {
			return null;
		} else {
			return sCurrentCollection;
		}
	}

	public String name() {
		String n = (new File(mPath)).getName().replace(".anki2", "");
		// TODO:
		return n;
	}

	/**
	 * DB-related
	 * ***************************************************************
	 * ********************************
	 */

	public boolean load() {
		Cursor cursor = null;
		try {
			// Read in deck table columns
			cursor = mDb.getDatabase().rawQuery(
					"SELECT crt, mod, scm, dty, usn, ls, conf, "
							+ "models, decks, dconf, tags FROM col", null);
			if (!cursor.moveToFirst()) {
				return false;
			}
			mCrt = cursor.getLong(0);
			mMod = cursor.getLong(1);
			mScm = cursor.getLong(2);
			mDty = cursor.getInt(3) == 1;
			mUsn = cursor.getInt(4);
			mLs = cursor.getLong(5);
			try {
				mConf = new JSONObject(cursor.getString(6));
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			mModels.load(cursor.getString(7));
			mDecks.load(cursor.getString(8), cursor.getString(9));
			mTags.load(cursor.getString(10));
			return true;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Mark DB modified. DB operations and the deck/tag/model managers do this
	 * automatically, so this is only necessary if you modify properties of this
	 * object or the conf dict.
	 */
	public void setMod() {
		mDb.setMod(true);
	}

	public void flush() {
		flush(0);
	}

	/**
	 * Flush state to DB, updating mod time.
	 */
	public void flush(long mod) {
		Log.i(AnkiDroidApp.TAG, "flush - Saving information to DB...");
		mMod = (mod == 0 ? Utils.intNow(1000) : mod);
		ContentValues values = new ContentValues();
		values.put("crt", mCrt);
		values.put("mod", mMod);
		values.put("scm", mScm);
		values.put("dty", mDty ? 1 : 0);
		values.put("usn", mUsn);
		values.put("ls", mLs);
		values.put("conf", mConf.toString());
		mDb.update("col", values);
	}

	/**
	 * Flush, commit DB, and take out another write lock.
	 */
	public synchronized void save() {
		save(null, 0);
	}

	public synchronized void save(String name, long mod) {
		// let the managers conditionally flush
		mModels.flush();
		mDecks.flush();
		mTags.flush();
		// and flush deck + bump mod if db has been changed
		if (mDb.getMod()) {
			flush(mod);
			mDb.commit();
			lock();
			mDb.setMod(false);
		}
		_markOp(name);
		mLastSave = Utils.now();
	}

	/** Save if 5 minutes has passed since last save. */
	public void autosave() {
		if ((Utils.now() - mLastSave) > 300) {
			save();
		}
	}

	/** make sure we don't accidentally bump mod time */
	public void lock() {
		// make sure we don't accidentally bump mod time
		boolean mod = mDb.getMod();
		mDb.execute("UPDATE col SET mod=mod");
		mDb.setMod(mod);
	}

	/**
	 * Disconnect from DB.
	 */
	public synchronized void close() {
		close(true);
	}

	public synchronized void close(boolean save) {
		// if (wait) {
		// Wait for any thread working on the deck to finish.
		// DeckTask.waitToFinish();
		// }
		mClosing = true;
		if (mDb != null) {
			cleanup();
			if (save) {
				getDb().getDatabase().beginTransaction();
				try {
					save();
					getDb().getDatabase().setTransactionSuccessful();
				} finally {
					getDb().getDatabase().endTransaction();
				}
			} else {
				rollback();
			}
			AnkiDatabaseManager.closeDatabase(mPath);
			mDb = null;
			// mMedia.close();
			Log.i(AnkiDroidApp.TAG, "Collection closed");
		}
	}

	public void reopen() {
		if (mDb == null) {
			mDb = AnkiDatabaseManager.getDatabase(mPath);
			// mMedia.connect();
		}
	}

	public void rollback() {
		// TODO:
	}

	/** Mark schema modified. Call this first so user can abort if necessary. */
	public void modSchema() {
		modSchema(true);
	}
	public void modSchema(boolean check) {
		if (!schemaChanged()) {
			if (check) {
				// TODO: ask user
			}
		}
		mScm = Utils.intNow(1000);
	}

	/** True if schema changed since last sync. */
	public boolean schemaChanged() {
		return mScm > mLs;
	}

	/**
	 * Signal there are temp. suspended cards that need cleaning up on close.
	 */
	public void setDirty() {
		mDty = true;
	}

	/**
	 * Unsuspend any temporarily suspended cards.
	 */
	private void cleanup() {
		if (mDty) {
			mStdSched.onClose();
			mDty = false;
		}
	}

	public int usn() {
		if (mServer) {
			return mUsn;
		} else {
			return -1;
		}
	}

	/** called before a full upload */
	public void beforeUpload() {
		String[] tables = new String[]{"notes", "cards", "revlog", "graves"};
		for (String t : tables) {
			mDb.execute("UPDATE " + t + " SET usn=0 WHERE usn=-1");
		}
		mUsn += 1;
		mModels.beforeUpload();
		mTags.beforeUpload();
		mDecks.beforeUpload();
		modSchema();
		mLs = mScm;
		close();
	}

	/**
	 * Object creation helpers
	 * **************************************************
	 * *********************************************
	 */

	public Card getCard(long id) {
		return new Card(this, id);
	}

	public Note getNote(long id) {
		return new Note(this, id);
	}

	/**
	 * Utils
	 * ********************************************************************
	 * ***************************
	 */

	public int nextID(String type) {
		type = "next" + type.toUpperCase();
		int id;
		try {
			id = mConf.getInt(type);
		} catch (JSONException e) {
			id = 1;
		}
		try {
			mConf.put(type, id + 1);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return id;
	}

	/**
	 * Rebuild the queue and reload data after DB modified.
	 */
	public void reset() {
		mSched.reset();
	}

	/**
	 * Deletion logging
	 * *********************************************************
	 * **************************************
	 */

	public void _logRem(long[] ids, int type) {
		for (long id : ids) {
			ContentValues values = new ContentValues();
			values.put("usn", usn());
			values.put("oid", id);
			values.put("type", type);
			mDb.insert("graves", null, values);
		}
	}

	/**
	 * Notes
	 * ********************************************************************
	 * ***************************
	 */

	public int noteCount() {
		return (int) mDb.queryScalar("SELECT count() FROM notes");
	}

	public Note newNote() {
		return newNote(mModels.current());
	}
	/**
	 * Return a new note with the current model.
	 */
	public Note newNote(JSONObject m) {
		return new Note(this, m);
	}

	/**
	 * Add a note to the collection. Return number of new cards.
	 */
	public int addNote(Note note) {
		// check we have card models available, then save
		ArrayList<JSONObject> cms = findTemplates(note);
		if (cms.size() == 0) {
			return 0;
		}
		note.flush();
		// deck conf governs which of these are used
		int due = nextID("pos");
		// add cards
		int ncards = 0;
		for (JSONObject template : cms) {
			_newCard(note, template, due);
			ncards += 1;
		}
		return ncards;
	}

	public void remNotes(long[] ids) {
		ArrayList<Long> list = mDb.queryColumn(Long.class,
				"SELECT id FROM cards WHERE nid IN " + Utils.ids2str(ids), 0);
		long[] cids = new long[list.size()];
		int i = 0;
		for (long l : list) {
			cids[i++] = l;
		}
		remCards(cids);
	}

	/**
	 * Bulk delete facts by ID. Don't call this directly.
	 */
	public void _remNotes(long[] ids) {
		if (ids.length == 0) {
			return;
		}
		String strids = Utils.ids2str(ids);
		// we need to log these independently of cards, as one side may have
		// more card templates
		_logRem(ids, Sched.REM_NOTE);
		mDb.execute("DELETE FROM notes WHERE id IN " + strids);
	}

	/**
	 * Card creation
	 * ************************************************************
	 * ***********************************
	 */

	/**
	 * @return (active), non-empty templates.
	 */
	private ArrayList<JSONObject> findTemplates(Note note) {
		ArrayList<JSONObject> ok = new ArrayList<JSONObject>();
		JSONObject model = note.model();
		ArrayList<Integer> avail = mModels.availOrds(model,
				Utils.joinFields(note.values()));
		JSONArray tmpls;
		try {
			tmpls = model.getJSONArray("tmpls");
			for (int i = 0; i < tmpls.length(); i++) {
				JSONObject t = tmpls.getJSONObject(i);
				if (avail.contains(t.getInt("ord"))) {
					ok.add(t);
				}
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return ok;
	}

	/**
	 * Generate cards for non-empty templates, return ids to remove.
	 */
	public ArrayList<Long> genCards(long[] nids) {
		// build map of (nid,ord) so we don't create dupes
		String snids = Utils.ids2str(nids);
		HashMap<Long, HashMap<Integer, Long>> have = new HashMap<Long, HashMap<Integer, Long>>();
		Cursor cur = null;
		try {
			cur = mDb.getDatabase().rawQuery(
					"SELECT id, nid, ord FROM cards WHERE nid IN " + snids,
					null);
			while (cur.moveToNext()) {
				long nid = cur.getLong(1);
				if (!have.containsKey(nids)) {
					have.put(nid, null);
				}
				have.get(nid).put(cur.getInt(2), cur.getLong(0));
			}
		} finally {
			if (cur != null && !cur.isClosed()) {
				cur.close();
			}
		}
		// build cards for each note
		ArrayList<Object[]> data = new ArrayList<Object[]>();
		long ts = Utils.maxID(mDb);
		long now = Utils.intNow();
		ArrayList<Long> rem = new ArrayList<Long>();
		int usn = usn();
		cur = null;
		try {
			cur = mDb.getDatabase()
					.rawQuery(
							"SELECT id, mid, did, flds FROM notes WHERE id IN "
									+ snids, null);
			while (cur.moveToNext()) {
				JSONObject model = mModels.get(cur.getLong(1));
				ArrayList<Integer> avail = mModels.availOrds(model,
						cur.getString(3));
				long nid = cur.getLong(0);
				JSONArray tmpls = model.getJSONArray("tmpls");
				for (int i = 0; i < tmpls.length(); i++) {
					JSONObject t = tmpls.getJSONObject(i);
					int tord = t.getInt("ord");
					boolean doHave = have.containsKey(nid)
							&& have.get(nid).containsKey(tord);
					// if have ord but empty, add cid to remove list
					// (may not have nid if generating before any cards added)
					if (doHave && !avail.contains(tord)) {
						rem.add(have.get(nid).get(tord));
					}
					// if missing ord and is available, generate
					if (!doHave && avail.contains(tord)) {
						long did = t.getLong("did");
						data.add(new Object[] { ts, nid,
								did != 0 ? did : cur.getLong(2), tord, now,
								usn, cur.getLong(0) });
						ts += 1;
					}
				}
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} finally {
			if (cur != null && !cur.isClosed()) {
				cur.close();
			}
		}
		// bulk update
		mDb.executeMany("INSERT INTO cards VALUES (?, ?, ?, ?, ?, ?, 0, 0, ?, 0, 0, 0, 0, 0, 0, 0)", data);
		return rem;
	}

	// previewCards

	/**
	 * Create a new card.
	 */
	private Card _newCard(Note note, JSONObject template, int due) {
		return _newCard(note, template, due, true);
	}

	private Card _newCard(Note note, JSONObject template, int due, boolean flush) {
		Card card = new Card(this);
		card.setNid(note.getId());
		try {
			card.setOrd(template.getInt("ord"));
		} catch (JSONException e) {
			new RuntimeException(e);
		}
		long did;
		try {
			did = template.getLong("did");
		} catch (JSONException e) {
			did = 0;
		}
		card.setDid(did != 0 ? did : note.getDid());
		card.setDue(_dueForDid(card.getDid(), due));
		if (flush) {
			card.flush();
		}
		return card;
	}

	public int _dueForDid(long did, int due) {
		JSONObject conf = mDecks.confForDid(did);
		// in order due?
		try {
			if (conf.getJSONObject("new").getInt("order") == Sched.NEW_CARDS_DUE) {
				return due;
			} else {
				// random mode; seed with note ts so all cards of this note get
				// the same random number
				Random r = new Random();
				r.setSeed(due);
				return r.nextInt((int) Math.pow(2, 32) - 2) + 1;
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Cards
	 * ********************************************************************
	 * ***************************
	 */

	public boolean isEmpty() {
		return mDb.queryScalar("SELECT 1 FROM cards LIMIT 1", false) == 0;
	}

	public int cardCount() {
		return mDb.queryScalar("SELECT count() FROM cards");
	}

	/**
	 * Bulk delete cards by ID.
	 */
	public void remCards(long[] ids) {
		if (ids.length == 0) {
			return;
		}
		String sids = Utils.ids2str(ids);
		long[] nids = Utils.arrayList2array(mDb.queryColumn(Long.class,
				"SELECT nid FROM cards WHERE id IN " + sids, 0));
		// remove cards
		_logRem(ids, Sched.REM_CARD);
		mDb.execute("DELETE FROM cards WHERE id IN " + sids);
		mDb.execute("DELETE FROM revlog WHERE cid IN " + sids);
		// then notes
		nids = Utils.arrayList2array(mDb.queryColumn(Long.class,
				"SELECT id FROM notes WHERE id IN " + Utils.ids2str(nids)
						+ " AND id NOT IN (SELECT nid FROM cards)", 0));
		_remNotes(nids);
	}

	public void remEmptyCards(long[] ids) {
		if (ids.length == 0) {
			return;
		}
		// TODO: ask user
		if (true) {
			remCards(ids);
		}
	}

	/**
	 * Field checksums and sorting fields
	 * ***************************************
	 * ********************************************************
	 */

	private ArrayList<Object[]> _fieldData(String snids) {
		ArrayList<Object[]> result = new ArrayList<Object[]>();
		Cursor cur = null;
		try {
			cur = mDb.getDatabase().rawQuery(
					"SELECT id, mid, flds FROM notes WHERE id IN " + snids,
					null);
			while (cur.moveToNext()) {
				result.add(new Object[] { cur.getLong(0), cur.getLong(1),
						cur.getString(2) });
			}
		} finally {
			if (cur != null && !cur.isClosed()) {
				cur.close();
			}
		}
		return result;
	}

	/** Update field checksums and sort cache, after find&replace, etc. */
	public void updateFieldCache(long[] nids) {
		String snids = Utils.ids2str(nids);
		ArrayList<Object[]> r = new ArrayList<Object[]>();
		for (Object[] o : _fieldData(snids)) {
			String[] fields = Utils.splitFields((String) o[2]);
			JSONObject model = mModels.get((Long) o[1]);
			// apply, relying on calling code to bump usn+mod
			r.add(new Object[]{Utils.stripHTML(fields[mModels.sortIdx(model)]), Utils.fieldChecksum(fields[0]), o[0]});
		}
		mDb.executeMany("UPDATE notes SET sfld=?, csum=? WHERE id=?", r);
	}

	/**
	 * Q/A generation
	 * ***********************************************************
	 * ************************************
	 */

	public ArrayList<HashMap<String, String>> renderQA() {
		return renderQA(null, "card");
	}

	public ArrayList<HashMap<String, String>> renderQA(int[] ids, String type) {
		String where;
		if (type.equals("card")) {
			where = "AND c.id IN " + Utils.ids2str(ids);
		} else if (type.equals("fact")) {
			where = "AND f.id IN " + Utils.ids2str(ids);
		} else if (type.equals("model")) {
			where = "AND m.id IN " + Utils.ids2str(ids);
		} else if (type.equals("all")) {
			where = "";
		} else {
			throw new RuntimeException();
		}
		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
		for (Object[] row : _qaData(where)) {
			result.add(_renderQA(row));
		}
		return result;
	}

	/**
	 * Returns hash of id, question, answer.
	 */
	public HashMap<String, String> _renderQA(Object[] data) {
		// data is [cid, nid, mid, did, ord, tags, flds]
		// unpack fields and create dict
		String[] flist = Utils.splitFields((String) data[6]);
		Map<String, String> fields = new HashMap<String, String>();
		long modelId = (Long) data[2];
		JSONObject model = mModels.get(modelId);
		String[] fnames = mModels.orderedFields(model);
		for (int i = 0; i < flist.length; i++) {
			fields.put(fnames[i], flist[i]);
		}
		fields.put("Tags", (String) data[5]);
		try {
			fields.put("Type", (String) model.get("name"));
			fields.put("Deck", mDecks.name((Long) data[3]));
			JSONObject template = model.getJSONArray("tmpls").getJSONObject(
					(Integer) data[4]);
			fields.put("Card", template.getString("name"));
            Models.fieldParser fparser = new Models.fieldParser(fields);
			// render q & a
			HashMap<String, String> d = new HashMap<String, String>();
			d.put("id", Long.toString((Long) data[0]));
			d.put("q", mModels.getCmpldTemplate(modelId, (Integer) data[4])[0]
					.execute(fparser));
			d.put("a", mModels.getCmpldTemplate(modelId, (Integer) data[4])[1]
					.execute(fparser));
			// TODO: runfilter
			return d;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return [cid, nid, mid, did, ord, tags, flds] db query
	 */
	public ArrayList<Object[]> _qaData() {
		return _qaData("");
	}

	public ArrayList<Object[]> _qaData(String where) {
		ArrayList<Object[]> data = new ArrayList<Object[]>();
		Cursor cur = null;
		try {
			cur = mDb
					.getDatabase()
					.rawQuery(
							"SELECT c.id, n.id, n.mid, c.did, c.ord, "
									+ "n.tags, n.flds FROM cards c, notes n WHERE c.nid == n.id "
									+ where, null);
			while (cur.moveToNext()) {
				data.add(new Object[] { cur.getLong(0), cur.getLong(1),
						cur.getLong(2), cur.getLong(3), cur.getInt(4),
						cur.getString(5), cur.getString(6) });
			}
		} finally {
			if (cur != null && !cur.isClosed()) {
				cur.close();
			}
		}
		return data;
	}

	/**
	 * Finding cards
	 * ************************************************************
	 * ***********************************
	 */

	// findcards
	// findreplace
	// findduplicates

	/**
	 * Stats
	 * ********************************************************************
	 * ***************************
	 */

	// cardstats
	// stats

	/**
	 * Timeboxing
	 * ***************************************************************
	 * ********************************
	 */

	public void startTimebox() {
		mLastSessionStart = mSessionStartTime;
		mSessionStartTime = Utils.now();
		mSessionStartReps = mRepsToday;
	}

	public void stopTimebox() {
		mSessionStartTime = 0;
	}

	public double timeboxStarted() {
		return mSessionStartTime;
	}

	public boolean timeboxReached() {
		if (mSessionStartTime == 0) {
			// not started
			return false;
		}
		if (mSessionTimeLimit != 0
				&& Utils.now() > (mSessionStartTime + mSessionTimeLimit)) {
			return true;
		}
		if (mSessionRepLimit != 0
				&& mSessionRepLimit <= mRepsToday - mSessionStartReps) {
			return true;
		}
		return false;
	}

	/**
	 * Schedulers and cramming
	 * **************************************************
	 * *********************************************
	 */

	/**
	 * True if scheduler changed.
	 */
	public boolean stdSched() {
		if (!mSched.getName().equals("std")) {
			cleanup();
			mSched = mStdSched;
			return true;
		} else {
			return false;
		}
	}

	public void cramDecks() {
		cramDecks("mod DESC", 0, 0);
	}

	public void cramDecks(String order, int min, int max) {
		stdSched();
		// TODO
		// mSched = new CramScheduler(this, order, min, max);
	}

	/**
	 * Undo
	 * *********************************************************************
	 * **************************
	 */

	/**
	 * [type, undoName, data] type 1 = review; type 2 = checkpoint
	 */
	public void clearUndo() {
		mUndo = new Object[3];
	}

	/** Undo menu item name, or None if undo unavailable. */
	public String undoName() {
		if (mUndo[1] == null) {
			return null;
		} else {
			return (String) mUndo[1];
		}
	}

	public boolean undoAvailable() {
		return mUndo[0] != null;
	}

	public Card undo() {
		if (((Integer) mUndo[0]) == 1) {
			return _undoReview();
		} else {
			_undoOp();
			return null;
		}
	}

	public void markReview(Card card) {
		LinkedList<Card> old = new LinkedList<Card>();
		if (mUndo[0] != null) {
			if ((Integer) mUndo[0] == 1) {
				old.addAll((LinkedList<Card>) mUndo[2]);
			}
			clearUndo();
		}
		mUndo[0] = 1;
		mUndo[1] = "review";
		old.add(card.clone());
		mUndo[2] = old;
	}

	private Card _undoReview() {
		LinkedList<Card> data = (LinkedList<Card>) mUndo[2];
		Card c = data.removeLast();
		if (data.size() == 0) {
			clearUndo();
		}
		// write old data
		c.flush();
		// and delete revlog entry
		long last = mDb.queryLongScalar("SELECT id FROM revlog WHERE cid = "
				+ c.getId() + " ORDER BY id DESC LIMIT 1");
		mDb.execute("DELETE FROM revlog WHERE id = " + last);
		// and finally, update daily count
		// FIXME: what to do in cramming case?
		String type = (new String[]{"new", "lrn", "rev"})[c.getQueue()];
		mSched._updateStats(c, type, -1);
		return c;
	}

	/** Call via .save() */
	private void _markOp(String name) {
		if (name != null && name.length() > 0) {
			mUndo[0] = 2;
			mUndo[1] = name;
		} else {
			// saving disables old checkpoint, but not review undo
			if (mUndo[0] != null && (Integer) mUndo[0] == 2) {
				clearUndo();
			}
		}
	}

	private void _undoOp() {
		rollback();
		clearUndo();
	}

	/**
	 * DB maintenance
	 * ***********************************************************
	 * ************************************
	 */

	/** Fix possible problems and rebuild caches. */
	public long fixIntegrity() {
		save();
		File file = new File(mPath);
		long oldSize = file.length();
		if (!mDb.queryString("PRAGMA integrity_check").equals("ok")) {
			return -1;
		}
		// delete any notes with missing cards
		ArrayList<Long> ids = mDb.queryColumn(Long.class, "SELECT id FROM notes WHERE id NOT IN (SELECT DISTINCT nid FROM cards)", 0);
		_remNotes(Utils.arrayList2array(ids));
		// tags
		mTags.registerNotes();
		// field cache
		for (JSONObject m : mModels.all()) {
			updateFieldCache(Utils.arrayList2array(mModels.nids(m)));
		}
		// and finally, optimize
		optimize();
		file = new File(mPath);
		long newSize = file.length();
		return (long)((oldSize - newSize) / 1024);
	}

	public void optimize() {
		Log.i(AnkiDroidApp.TAG, "executing VACUUM statement");
		mDb.execute("VACUUM");
		Log.i(AnkiDroidApp.TAG, "executing ANALYZE statement");
		mDb.execute("ANALYZE");
	}

	/**
	 * Getters/Setters
	 * **********************************************************
	 * *************************************
	 */

	public AnkiDb getDb() {
		return mDb;
	}

	public Decks getDecks() {
		return mDecks;
	}

	public Media getMedia() {
	    return mMedia;
	}
	
	public Models getModels() {
		return mModels;
	}

	public JSONObject getConf() {
		return mConf;
	}

	public void setConf(JSONObject conf) {
		mConf = conf;
	}

	public long getScm() {
		return mScm;
	}

	public boolean getServer() {
		return mServer;
	}

	public void setLs(long ls) {
		mLs = ls;
	}

	public void setUsnAfterSync(int usn) {
		mUsn = usn;
	}

	public long getMod() {
		return mMod;
	}

	/* this getter is only for syncing routines, use usn() instead elsewhere */
	public int getUsnForSync() {
		return mUsn;
	}

	public Tags getTags() {
		return mTags;
	}

	public long getCrt() {
		return mCrt;
	}

	public Sched getSched() {
		return mSched;
	}

	public String getPath() {
		return mPath;
	}

	public void setServer(boolean server) {
		mServer = server;
	}
}
