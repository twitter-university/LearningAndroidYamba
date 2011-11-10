package com.marakana.yamba5;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;

public class TimelineActivity1 extends Activity { // <1>
  DbHelper dbHelper;
  SQLiteDatabase db;
  Cursor cursor;
  TextView textTimeline;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.timeline);

    // Find your views
    textTimeline = (TextView) findViewById(R.id.textTimeline);

    // Connect to database
    dbHelper = new DbHelper(this);  // <2>
    db = dbHelper.getReadableDatabase();  // <3>
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    // Close the database
    db.close(); // <4>
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Get the data from the database
    cursor = db.query(DbHelper.TABLE, null, null, null, null, null,
        DbHelper.C_CREATED_AT + " DESC"); // <5>
    startManagingCursor(cursor);  // <6>

    // Iterate over all the data and print it out
    String user, text, output;
    while (cursor.moveToNext()) {  // <7>
      user = cursor.getString(cursor.getColumnIndex(DbHelper.C_USER));  // <8>
      text = cursor.getString(cursor.getColumnIndex(DbHelper.C_TEXT));
      output = String.format("%s: %s\n", user, text); // <9>
      textTimeline.append(output); // <10>
    }
  }
  
  

}
