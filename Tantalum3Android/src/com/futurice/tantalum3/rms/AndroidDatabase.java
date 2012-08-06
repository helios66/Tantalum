package com.futurice.tantalum3.rms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AndroidDatabase extends SQLiteOpenHelper {

	protected static final int DB_VERSION = 1;
	protected static final String DB_NAME = "TantalumRMS";
	protected static final String TABLE_NAME = "TantalumRMS_Table";
	protected static final String COL_ID = "id";
	protected static final String COL_KEY = "key";
	protected static final String COL_DATA = "data";
	protected static final String CREATE_DB = "CREATE TABLE IF NOT EXISTS "
			+ TABLE_NAME + "(" + COL_ID + " INTEGER PRIMARY KEY, " + COL_KEY
			+ " TEXT NOT NULL, " + COL_DATA + " BLOB NOT NULL)";

	private static Context context;

	public AndroidDatabase() {
		super(context, DB_NAME, null, DB_VERSION);
	}

    @Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_DB);
	}

    @Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	public synchronized byte[] getData(String key) {

		System.out.println(key);
		String[] fields = new String[] { COL_DATA };

		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_NAME, fields, COL_KEY + "=?",
				new String[] { String.valueOf(key) }, null, null, null, null);

		if (cursor == null || cursor.getCount() == 0) {
			db.close();
			return null;
		} else {
			cursor.moveToFirst();
			System.out.println(cursor.getColumnNames());
			System.out.println(cursor.getColumnCount());
			byte[] data = cursor.getBlob(0);
			db.close();
			return data;
		}

	}

	public synchronized void putData(String key, byte[] data) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(COL_KEY, key);
		values.put(COL_DATA, data);

		db.insert(TABLE_NAME, null, values);
		db.close();

	}

	public synchronized void removeData(String key) {

		SQLiteDatabase db = this.getWritableDatabase();

		String where = COL_KEY + "==\"" + key + "\"";

		db.delete(TABLE_NAME, where, null);
		db.close();

	}

	public static void setContext(Context c) {
		context = c;
	}

}