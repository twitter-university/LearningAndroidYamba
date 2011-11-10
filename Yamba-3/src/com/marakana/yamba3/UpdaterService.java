package com.marakana.yamba3;

import java.util.List;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService extends Service {
  private static final String TAG = "UpdaterService";

  static final int DELAY = 60000; // wait a minute
  private boolean runFlag = false;
  private Updater updater;
  private YambaApplication yamba; // <1>

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    this.yamba = (YambaApplication) getApplication(); // <2>
    this.updater = new Updater();

    Log.d(TAG, "onCreated");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);

    this.runFlag = true;
    this.updater.start();
    this.yamba.setServiceRunning(true); // <3>

    Log.d(TAG, "onStarted");
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    this.runFlag = false;
    this.updater.interrupt();
    this.updater = null;
    this.yamba.setServiceRunning(false); // <4>

    Log.d(TAG, "onDestroyed");
  }

  /**
   * Thread that performs the actual update from the online service
   */
  private class Updater extends Thread {
    List<Twitter.Status> timeline; // <5>

    public Updater() {
      super("UpdaterService-Updater");
    }

    @Override
    public void run() {
      UpdaterService updaterService = UpdaterService.this;
      while (updaterService.runFlag) {
        Log.d(TAG, "Updater running");
        try {
          // Get the timeline from the cloud
          try {
            timeline = yamba.getTwitter().getFriendsTimeline(); // <6>
          } catch (TwitterException e) {
            Log.e(TAG, "Failed to connect to twitter service", e);  // <7>
          }

          // Loop over the timeline and print it out
          for (Twitter.Status status : timeline) { // <8>
            Log.d(TAG, String.format("%s: %s", status.user.name, status.text)); // <9>
          }

          Log.d(TAG, "Updater ran");
          Thread.sleep(DELAY);
        } catch (InterruptedException e) {
          updaterService.runFlag = false;
        }
      }
    }
  } // Updater
}
