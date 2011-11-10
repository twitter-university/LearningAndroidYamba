package com.marakana.yamba1;

import winterwell.jtwitter.Twitter;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class StatusActivity1 extends Activity implements OnClickListener { // <1>
  private static final String TAG = "StatusActivity";
  EditText editText;
  Button updateButton;
  Twitter twitter;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.status);

    // Find views
    editText = (EditText) findViewById(R.id.editText); // <2>
    updateButton = (Button) findViewById(R.id.buttonUpdate);

    updateButton.setOnClickListener(this); // <3>

    twitter = new Twitter("student", "password"); // <4>
    twitter.setAPIRootUrl("http://yamba.marakana.com/api");
  }

  // Called when button is clicked // <5>
  public void onClick(View v) {
    twitter.setStatus(editText.getText().toString()); // <6>
    Log.d(TAG, "onClicked");
  }
}