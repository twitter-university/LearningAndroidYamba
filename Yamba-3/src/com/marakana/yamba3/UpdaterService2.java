package com.marakana.yamba3;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService2 extends Service {
  private static final String TAG = "UpdaterService";

  static final int DELAY = 60000; // a minute <1>
  private boolean runFlag = false;  // <2>
  private Updater updater;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    
    this.updater = new Updater(); // <3>

    Log.d(TAG, "onCreated");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);

    this.runFlag = true; // <4>
    this.updater.start(); 
    
    Log.d(TAG, "onStarted");
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    this.runFlag = false; // <5>
    this.updater.interrupt(); // <6>
    this.updater = null;

    Log.d(TAG, "onDestroyed");
  }

  /**
   * Thread that performs the actual update from the online service
   */
  private class Updater extends Thread {  // <7>
    
    public Updater() {
      super("UpdaterService-Updater");  // <8>
    }

    @Override
    public void run() { // <9>
      UpdaterService2 updaterService = UpdaterService2.this;  // <10> 
      while (updaterService.runFlag) {  // <11>
        Log.d(TAG, "Updater running");
        try {
          // Some work goes here...
          Log.d(TAG, "Updater ran");
          Thread.sleep(DELAY);  // <12>
        } catch (InterruptedException e) {  // <13>
          updaterService.runFlag = false;
        }
      }
    }
  } // Updater
}
