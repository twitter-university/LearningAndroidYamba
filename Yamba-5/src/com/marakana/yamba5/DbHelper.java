package com.marakana.yamba5;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {
  static final String TAG = "DbHelper";
  static final String DB_NAME = "timeline.db";
  static final int DB_VERSION = 3;
  static final String TABLE = "timeline";
  static final String C_ID = "_id";
  static final String C_CREATED_AT = "created_at";
  static final String C_SOURCE = "source";
  static final String C_TEXT = "txt";
  static final String C_USER = "user";
  Context context;

  // Constructor
  public DbHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
    this.context = context;
  }

  // Called only once, first time the DB is created
  @Override
  public void onCreate(SQLiteDatabase db) {
    String sql = context.getString(R.string.sql1);

    Log.d(TAG, "onCreated sql: " + sql);

    db.execSQL(sql);
  }

  // Called whenever newVersion != oldVersion
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Typically do ALTER TABLE statements, but...we're just in development,
    // so:

    db.execSQL("drop table if exists " + TABLE); // blow the old database
    // away
    Log.d(TAG, "onUpdated");
    onCreate(db); // run onCreate to get new database
  }

}
