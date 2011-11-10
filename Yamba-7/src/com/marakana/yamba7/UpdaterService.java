package com.marakana.yamba7;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService extends Service {
  public static final String NEW_STATUS_INTENT = "com.marakana.yamba.NEW_STATUS";
  public static final String NEW_STATUS_EXTRA_COUNT = "NEW_STATUS_EXTRA_COUNT";
  private static final String TAG = "UpdaterService";

  static final int DELAY = 60000; // a minute
  private boolean runFlag = false;
  private Updater updater;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    this.updater = new Updater();

    Log.d(TAG, "onCreated");
  }

  @Override
  public int onStartCommand(Intent intent, int flag, int startId) {
    if (!runFlag) {
      this.runFlag = true;
      this.updater.start();
      ((YambaApplication) super.getApplication()).setServiceRunning(true);

      Log.d(TAG, "onStarted");
    }
    return Service.START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    this.runFlag = false;
    this.updater.interrupt();
    this.updater = null;
    ((YambaApplication) super.getApplication()).setServiceRunning(false);

    Log.d(TAG, "onDestroyed");
  }

  /**
   * Thread that performs the actual update from the online service
   */
  private class Updater extends Thread {
    static final String RECEIVE_TIMELINE_NOTIFICATIONS = "com.marakana.yamba.RECEIVE_TIMELINE_NOTIFICATIONS";

    Intent intent;

    public Updater() {
      super("UpdaterService-Updater");
    }

    @Override
    public void run() {
      UpdaterService updaterService = UpdaterService.this;
      while (updaterService.runFlag) {
        Log.d(TAG, "Running background thread");
        try {
          YambaApplication yamba = (YambaApplication) updaterService
              .getApplication();
          int newUpdates = yamba.fetchStatusUpdates();
          if (newUpdates > 0) {
            Log.d(TAG, "We have a new status");
            intent = new Intent(NEW_STATUS_INTENT);
            intent.putExtra(NEW_STATUS_EXTRA_COUNT, newUpdates);
            updaterService
                .sendBroadcast(intent, RECEIVE_TIMELINE_NOTIFICATIONS);
          }
          Thread.sleep(DELAY);
        } catch (InterruptedException e) {
          updaterService.runFlag = false;
        }
      }
    }
  } // Updater
}
