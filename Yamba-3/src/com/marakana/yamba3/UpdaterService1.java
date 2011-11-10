package com.marakana.yamba3;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService1 extends Service {
  static final String TAG = "UpdaterService"; // <1>

  @Override
  public IBinder onBind(Intent intent) { // <2>
    return null;
  }

  @Override
  public void onCreate() { // <3>
    super.onCreate();
    Log.d(TAG, "onCreated");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) { // <4>
    super.onStartCommand(intent, flags, startId);
    Log.d(TAG, "onStarted");
    return START_STICKY;
  }

  @Override
  public void onDestroy() { // <5>
    super.onDestroy();
    Log.d(TAG, "onDestroyed");
  }
}
