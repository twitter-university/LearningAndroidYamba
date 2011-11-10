package com.marakana.yamba7;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class YambaWidget extends AppWidgetProvider { // <1>
  private static final String TAG = YambaWidget.class.getSimpleName();

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) { // <2>
    Cursor c = context.getContentResolver().query(StatusProvider.CONTENT_URI,
        null, null, null, null); // <3>
    try {
      if (c.moveToFirst()) { // <4>
        CharSequence user = c.getString(c.getColumnIndex(StatusData.C_USER)); // <5>
        CharSequence createdAt = DateUtils.getRelativeTimeSpanString(context, c
            .getLong(c.getColumnIndex(StatusData.C_CREATED_AT)));
        CharSequence message = c.getString(c.getColumnIndex(StatusData.C_TEXT));

        // Loop through all instances of this widget
        for (int appWidgetId : appWidgetIds) { // <6>
          Log.d(TAG, "Updating widget " + appWidgetId);
          RemoteViews views = new RemoteViews(context.getPackageName(),
              R.layout.yamba_widget); // <7>
          views.setTextViewText(R.id.textUser, user); // <8>
          views.setTextViewText(R.id.textCreatedAt, createdAt);
          views.setTextViewText(R.id.textText, message);
          views.setOnClickPendingIntent(R.id.yamba_icon, PendingIntent
              .getActivity(context, 0, new Intent(context,
                  TimelineActivity.class), 0));
          appWidgetManager.updateAppWidget(appWidgetId, views); // <9>
        }
      } else {
        Log.d(TAG, "No data to update");
      }
    } finally {
      c.close(); // <10>
    }
    Log.d(TAG, "onUpdated");
  }

  @Override
  public void onReceive(Context context, Intent intent) { // <11>
    super.onReceive(context, intent);
    if (intent.getAction().equals(UpdaterService.NEW_STATUS_INTENT)) { // <12>
      Log.d(TAG, "onReceived detected new status update");
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context); // <13>
      this.onUpdate(context, appWidgetManager, appWidgetManager
          .getAppWidgetIds(new ComponentName(context, YambaWidget.class))); // <14>
    }
  }
}
