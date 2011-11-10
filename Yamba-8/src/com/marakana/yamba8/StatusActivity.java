package com.marakana.yamba8;

import winterwell.jtwitter.Twitter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StatusActivity extends BaseActivity implements OnClickListener,
    TextWatcher, LocationListener { // <1>
  private static final String TAG = "StatusActivity";
  private static final long LOCATION_MIN_TIME = 3600000; // One hour
  private static final float LOCATION_MIN_DISTANCE = 1000; // One kilometer
  EditText editText;
  Button updateButton;
  TextView textCount;
  LocationManager locationManager; // <2>
  Location location;
  String provider;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.status);

    // Find views
    editText = (EditText) findViewById(R.id.editText);
    updateButton = (Button) findViewById(R.id.buttonUpdate);
    updateButton.setOnClickListener(this);

    textCount = (TextView) findViewById(R.id.textCount);
    textCount.setText(Integer.toString(140));
    textCount.setTextColor(Color.GREEN);
    editText.addTextChangedListener(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Setup location information
    provider = yamba.getProvider(); // <3>
    if (!YambaApplication.LOCATION_PROVIDER_NONE.equals(provider)) { // <4>
      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); // <5>
    }
    if (locationManager != null) {
      location = locationManager.getLastKnownLocation(provider); // <6>
      locationManager.requestLocationUpdates(provider, LOCATION_MIN_TIME,
          LOCATION_MIN_DISTANCE, this); // <7>
    }

  }

  @Override
  protected void onPause() {
    super.onPause();

    if (locationManager != null) {
      locationManager.removeUpdates(this);  // <8>
    }
  }

  // Called when button is clicked
  public void onClick(View v) {
    String status = editText.getText().toString();
    new PostToTwitter().execute(status);
    Log.d(TAG, "onClicked");
  }

  // Asynchronously posts to twitter
  class PostToTwitter extends AsyncTask<String, Integer, String> {
    // Called to initiate the background activity
    @Override
    protected String doInBackground(String... statuses) {
      try {
        // Check if we have the location
        if (location != null) { // <9>
          double latlong[] = {location.getLatitude(), location.getLongitude()};
          yamba.getTwitter().setMyLocation(latlong);
        }
        Twitter.Status status = yamba.getTwitter().updateStatus(statuses[0]);
        return status.text;
      } catch (RuntimeException e) {
        Log.e(TAG, "Failed to connect to twitter service", e);
        return "Failed to post";
      }
    }

    // Called once the background activity has completed
    @Override
    protected void onPostExecute(String result) {
      Toast.makeText(StatusActivity.this, result, Toast.LENGTH_LONG).show();
    }
  }

  // TextWatcher methods
  public void afterTextChanged(Editable statusText) {
    int count = 140 - statusText.length();
    textCount.setText(Integer.toString(count));
    textCount.setTextColor(Color.GREEN);
    if (count < 10)
      textCount.setTextColor(Color.YELLOW);
    if (count < 0)
      textCount.setTextColor(Color.RED);
  }

  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  }

  public void onTextChanged(CharSequence s, int start, int before, int count) {
  }

  // LocationListener methods
  public void onLocationChanged(Location location) { // <10>
    this.location = location;
  }

  public void onProviderDisabled(String provider) { // <11>
    if (this.provider.equals(provider))
      locationManager.removeUpdates(this);
  }

  public void onProviderEnabled(String provider) { // <12>
    if (this.provider.equals(provider))
      locationManager.requestLocationUpdates(this.provider, LOCATION_MIN_TIME,
          LOCATION_MIN_DISTANCE, this);
  }

  public void onStatusChanged(String provider, int status, Bundle extras) { // <13>
  }

}