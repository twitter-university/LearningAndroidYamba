package com.marakana.yamba4;

import java.util.List;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService1 extends Service {
  private static final String TAG = "UpdaterService";

  static final int DELAY = 60000; // wait a minute
  private boolean runFlag = false;
  private Updater updater;
  private YambaApplication yamba;

  DbHelper1 dbHelper; // <1>
  SQLiteDatabase db;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    this.yamba = (YambaApplication) getApplication();
    this.updater = new Updater();

    dbHelper = new DbHelper1(this); // <2>

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
    this.yamba.setServiceRunning(false);

    Log.d(TAG, "onDestroyed");
  }

  /**
   * Thread that performs the actual update from the online service
   */
  private class Updater extends Thread {
    List<Twitter.Status> timeline;

    public Updater() {
      super("UpdaterService-Updater");
    }

    @Override
    public void run() {
      UpdaterService1 updaterService = UpdaterService1.this;
      while (updaterService.runFlag) {
        Log.d(TAG, "Updater running");
        try {
          // Get the timeline from the cloud
          try {
            timeline = yamba.getTwitter().getFriendsTimeline(); // <3>
          } catch (TwitterException e) {
            Log.e(TAG, "Failed to connect to twitter service", e);
          }

          // Open the database for writing
          db = dbHelper.getWritableDatabase(); // <4>

          // Loop over the timeline and print it out
          ContentValues values = new ContentValues(); // <5>
          for (Twitter.Status status : timeline) { // <6>
            // Insert into database
            values.clear(); // <7>
            values.put(DbHelper1.C_ID, status.id);
            values.put(DbHelper1.C_CREATED_AT, status.createdAt.getTime());
            values.put(DbHelper1.C_SOURCE, status.source);
            values.put(DbHelper1.C_TEXT, status.text);
            values.put(DbHelper1.C_USER, status.user.name);
            db.insertOrThrow(DbHelper1.TABLE, null, values); // <8>

            Log.d(TAG, String.format("%s: %s", status.user.name, status.text));
          }

          // Close the database
          db.close(); // <9>

          Log.d(TAG, "Updater ran");
          Thread.sleep(DELAY);
        } catch (InterruptedException e) {
          updaterService.runFlag = false;
        }
      }
    }
  } // Updater

}
