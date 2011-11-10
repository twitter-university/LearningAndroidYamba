package com.marakana.yamba4;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DbHelper1 extends SQLiteOpenHelper { // <1>
  static final String TAG = "DbHelper";
  static final String DB_NAME = "timeline.db"; // <2>
  static final int DB_VERSION = 1; // <3>
  static final String TABLE = "timeline"; // <4>
  static final String C_ID = BaseColumns._ID;
  static final String C_CREATED_AT = "created_at";
  static final String C_SOURCE = "source";
  static final String C_TEXT = "txt";
  static final String C_USER = "user";
  Context context;

  // Constructor
  public DbHelper1(Context context) { // <5>
    super(context, DB_NAME, null, DB_VERSION);
    this.context = context;
  }

  // Called only once, first time the DB is created
  @Override
  public void onCreate(SQLiteDatabase db) {
    String sql = "create table " + TABLE + " (" + C_ID + " int primary key, "
    + C_CREATED_AT + " int, " + C_USER + " text, " + C_TEXT + " text)"; // <6>
    
    db.execSQL(sql);  // <7>

    Log.d(TAG, "onCreated sql: " + sql);
  }

  // Called whenever newVersion != oldVersion
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { // <8>
    // Typically do ALTER TABLE statements, but...we're just in development,
    // so:

    db.execSQL("drop table if exists " + TABLE); // drops the old database
    Log.d(TAG, "onUpdated");
    onCreate(db); // run onCreate to get new database
  }

}
