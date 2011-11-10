package com.marakana.yamba5;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class TimelineActivity2 extends Activity {
  DbHelper dbHelper;
  SQLiteDatabase db;
  Cursor cursor;  // <1>
  ListView listTimeline;  // <2>
  SimpleCursorAdapter adapter;  // <3>
  static final String[] FROM = { DbHelper.C_CREATED_AT, DbHelper.C_USER,
      DbHelper.C_TEXT };  // <4>
  static final int[] TO = { R.id.textCreatedAt, R.id.textUser, R.id.textText }; // <5>

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.timeline);

    // Find your views
    listTimeline = (ListView) findViewById(R.id.listTimeline);  // <6>

    // Connect to database
    dbHelper = new DbHelper(this);
    db = dbHelper.getReadableDatabase();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // Close the database
    db.close(); 
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Get the data from the database
    cursor = db.query(DbHelper.TABLE, null, null, null, null, null,
        DbHelper.C_CREATED_AT + " DESC");
    startManagingCursor(cursor);

    // Setup the adapter
    adapter = new SimpleCursorAdapter(this, R.layout.row, cursor, FROM, TO);  // <7>
    listTimeline.setAdapter(adapter); // <8>
  }

}
