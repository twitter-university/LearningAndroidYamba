package com.marakana.yamba5;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class TimelineAdapter extends SimpleCursorAdapter { // <1>
  static final String[] FROM = { DbHelper.C_CREATED_AT, DbHelper.C_USER,
      DbHelper.C_TEXT }; // <2>
  static final int[] TO = { R.id.textCreatedAt, R.id.textUser, R.id.textText }; // <3>

  // Constructor
  public TimelineAdapter(Context context, Cursor c) { // <4>
    super(context, R.layout.row, c, FROM, TO);
  }

  // This is where the actual binding of a cursor to view happens
  @Override
  public void bindView(View row, Context context, Cursor cursor) { // <5>
    super.bindView(row, context, cursor);

    // Manually bind created at timestamp to its view
    long timestamp = cursor.getLong(cursor
        .getColumnIndex(DbHelper.C_CREATED_AT)); // <6>
    TextView textCreatedAt = (TextView) row.findViewById(R.id.textCreatedAt); // <7>
    textCreatedAt.setText(DateUtils.getRelativeTimeSpanString(timestamp)); // <8>
  }

}
