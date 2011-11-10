package com.marakana.yamba4;

import winterwell.jtwitter.Twitter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StatusActivity extends Activity implements OnClickListener,
    TextWatcher {
  private static final String TAG = "StatusActivity";
  YambaApplication yamba;
  EditText editText;
  Button updateButton;
  TextView textCount;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.status);

    yamba = (YambaApplication) getApplication();

    // Find views
    editText = (EditText) findViewById(R.id.editText);
    updateButton = (Button) findViewById(R.id.buttonUpdate);
    updateButton.setOnClickListener(this);

    textCount = (TextView) findViewById(R.id.textCount);
    textCount.setText(Integer.toString(140));
    textCount.setTextColor(Color.GREEN);
    editText.addTextChangedListener(this);
  }

  // Called when button is clicked
  public void onClick(View v) {
    String status = editText.getText().toString();
    new PostToTwitter().execute(status);
    Log.d(TAG, "onClicked");
  }

  // Called first time user clicks on the menu button
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return true;
  }

  // Called when an options item is clicked
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.itemServiceStart:
      startService(new Intent(this, UpdaterService.class));
      break;
    case R.id.itemServiceStop:
      stopService(new Intent(this, UpdaterService.class));
      break;
    case R.id.itemPrefs:
      startActivity(new Intent(this, PrefsActivity.class));
      break;
    }

    return true;
  }

  // Asynchronously posts to twitter
  class PostToTwitter extends AsyncTask<String, Integer, String> {
    // Called to initiate the background activity
    @Override
    protected String doInBackground(String... statuses) {
      try {
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

}