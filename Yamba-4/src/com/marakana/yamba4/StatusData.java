package com.marakana.yamba4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class StatusData { // <1>
  private static final String TAG = StatusData.class.getSimpleName();

  static final int VERSION = 1;
  static final String DATABASE = "timeline.db";
  static final String TABLE = "timeline";

  public static final String C_ID = "_id";
  public static final String C_CREATED_AT = "created_at";
  public static final String C_TEXT = "txt";
  public static final String C_USER = "user";

  private static final String GET_ALL_ORDER_BY = C_CREATED_AT + " DESC";

  private static final String[] MAX_CREATED_AT_COLUMNS = { "max("
      + StatusData.C_CREATED_AT + ")" };

  private static final String[] DB_TEXT_COLUMNS = { C_TEXT };

  // DbHelper implementations
  class DbHelper extends SQLiteOpenHelper {

    public DbHelper(Context context) {
      super(context, DATABASE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      Log.i(TAG, "Creating database: " + DATABASE);
      db.execSQL("create table " + TABLE + " (" + C_ID + " int primary key, "
          + C_CREATED_AT + " int, " + C_USER + " text, " + C_TEXT + " text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("drop table " + TABLE);
      this.onCreate(db);
    }
  }

  private final DbHelper dbHelper; // <2>

  public StatusData(Context context) {  // <3>
    this.dbHelper = new DbHelper(context);
    Log.i(TAG, "Initialized data");
  }

  public void close() { // <4>
    this.dbHelper.close();
  }

  public void insertOrIgnore(ContentValues values) {  // <5>
    Log.d(TAG, "insertOrIgnore on " + values);
    SQLiteDatabase db = this.dbHelper.getWritableDatabase();  // <6>
    try {
      db.insertWithOnConflict(TABLE, null, values,
          SQLiteDatabase.CONFLICT_IGNORE);  // <7>
    } finally {
      db.close(); // <8>
    }
  }

  /**
   * 
   * @return Cursor where the columns are _id, created_at, user, txt
   */
  public Cursor getStatusUpdates() {  // <9>
    SQLiteDatabase db = this.dbHelper.getReadableDatabase();
    return db.query(TABLE, null, null, null, null, null, GET_ALL_ORDER_BY);
  }

  /**
   * 
   * @return Timestamp of the latest status we ahve it the database
   */
  public long getLatestStatusCreatedAtTime() {  // <10>
    SQLiteDatabase db = this.dbHelper.getReadableDatabase();
    try {
      Cursor cursor = db.query(TABLE, MAX_CREATED_AT_COLUMNS, null, null, null,
          null, null);
      try {
        return cursor.moveToNext() ? cursor.getLong(0) : Long.MIN_VALUE;
      } finally {
        cursor.close();
      }
    } finally {
      db.close();
    }
  }

  /**
   * 
   * @param id of the status we are looking for
   * @return Text of the status
   */
  public String getStatusTextById(long id) {  // <11>
    SQLiteDatabase db = this.dbHelper.getReadableDatabase();
    try {
      Cursor cursor = db.query(TABLE, DB_TEXT_COLUMNS, C_ID + "=" + id, null,
          null, null, null);
      try {
        return cursor.moveToNext() ? cursor.getString(0) : null;
      } finally {
        cursor.close();
      }
    } finally {
      db.close();
    }
  }


}
