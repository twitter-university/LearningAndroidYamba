package com.marakana.yamba8;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

  
  public void onReceive(Context context, Intent callingIntent) {

    // Check if we should do anything at boot at all
    long interval = ((YambaApplication) context.getApplicationContext())
        .getInterval(); // <1>
    if (interval == YambaApplication.INTERVAL_NEVER)  // <2>
      return;

    // Create the pending intent
    Intent intent = new Intent(context, UpdaterService.class);  // <3>
    PendingIntent pendingIntent = PendingIntent.getService(context, -1, intent,
        PendingIntent.FLAG_UPDATE_CURRENT); // <4>

    // Setup alarm service to wake up and start service periodically
    AlarmManager alarmManager = (AlarmManager) context
        .getSystemService(Context.ALARM_SERVICE); // <5>
    alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, System
        .currentTimeMillis(), interval, pendingIntent); // <6>

    Log.d("BootReceiver", "onReceived");
  }

}
