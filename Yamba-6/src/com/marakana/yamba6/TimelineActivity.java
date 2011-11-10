package com.marakana.yamba6;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class TimelineActivity extends BaseActivity { 
  static final String SEND_TIMELINE_NOTIFICATIONS = "com.marakana.yamba.SEND_TIMELINE_NOTIFICATIONS";

  Cursor cursor;
  ListView listTimeline;
  SimpleCursorAdapter adapter;
  static final String[] FROM = { StatusData.C_CREATED_AT, StatusData.C_USER,
    StatusData.C_TEXT };
  static final int[] TO = { R.id.textCreatedAt, R.id.textUser, R.id.textText };
  TimelineReceiver receiver;
  IntentFilter filter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.timeline);

    // Find your views
    listTimeline = (ListView) findViewById(R.id.listTimeline);
    
    // Create the receiver
    receiver = new TimelineReceiver();
    filter = new IntentFilter( UpdaterService.NEW_STATUS_INTENT );
  }

  @Override
  protected void onResume() {
    super.onResume();

    this.setupList();
    
    // Register the receiver
    super.registerReceiver(receiver, filter,
        SEND_TIMELINE_NOTIFICATIONS, null);
  }
  
  @Override
  protected void onPause() {
    super.onPause();

    // UNregister the receiver
    unregisterReceiver(receiver); 
  }

  // View binder constant to inject business logic for timestamp to relative
  // time conversion
  static final ViewBinder VIEW_BINDER = new ViewBinder() { 

    public boolean setViewValue(View view, Cursor cursor, int columnIndex) { 
      if(view.getId() != R.id.textCreatedAt) return false; 
      
      // Update the created at text to relative time
      long timestamp = cursor.getLong(columnIndex); 
      CharSequence relTime = DateUtils.getRelativeTimeSpanString(view.getContext(), timestamp); 
      ((TextView)view).setText(relTime); 
      
      return true;  
    }

  };
  
  // Responsible for fetching data and setting up the list and the adapter
  private void setupList() {
    // Get the data from the database
    YambaApplication yamba = (YambaApplication) super.getApplication();
    this.cursor = yamba.getStatusData().getStatusUpdates();
    startManagingCursor(this.cursor);

    // Setup the adapter
    adapter = new SimpleCursorAdapter(this, R.layout.row, cursor, FROM, TO);
    adapter.setViewBinder(VIEW_BINDER); 

    listTimeline.setAdapter(adapter); 
  }

  // Receiver to wake up when UpdaterService gets a new status
  // It refreshes the timeline list by requerying the cursor
  class TimelineReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      setupList();
      Log.d("TimelineReceiver", "onReceived");
    }
  }

}
