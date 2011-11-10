package com.marakana.yamba6;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver { // <1>

  @Override
  public void onReceive(Context context, Intent intent) { // <2>
    context.startService(new Intent(context, UpdaterService.class)); // <3>
    Log.d("BootReceiver", "onReceived");
  }

}
