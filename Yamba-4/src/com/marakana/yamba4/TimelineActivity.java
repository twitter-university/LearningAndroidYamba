package com.marakana.yamba4;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;

public class TimelineActivity extends Activity {
  DbHelper1 dbHelper;
  SQLiteDatabase db;
  Cursor cursor;            // <1>
  ListView listTimeline;    // <2>
  TimelineAdapter adapter;  // <3>

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.timeline);

    // Find your views
    listTimeline = (ListView) findViewById(R.id.listTimeline); // <4>

    // Connect to database
    dbHelper = new DbHelper1(this);
    db = dbHelper.getReadableDatabase();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // Close the database
    db.close();
  }

  @Override
  protected void onResume() {                     // <5>
    super.onResume();

    // Get the data from the database
    cursor = db.query(DbHelper1.TABLE, null, null, null, null, null,
        DbHelper1.C_CREATED_AT + " DESC");         // <6>

    // Create the adapter
    adapter = new TimelineAdapter(this, cursor);  // <7>
    listTimeline.setAdapter(adapter);             // <8>

  }

}
