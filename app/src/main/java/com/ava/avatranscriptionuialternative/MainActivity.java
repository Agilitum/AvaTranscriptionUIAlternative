package com.ava.avatranscriptionuialternative;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

	private final String LOG_TAG = MainActivity.class.getName();

	private final String PUBNUB_PUBLISH_KEY = "pub-c-6590f75c-b2bb-4acc-9922-d5fe5aa8dec9";
	private final String PUBNUB_SUBSCRIBE_KEY = "sub-c-897a7150-da55-11e5-9ce2-0619f8945a4f";
	private final String AVA_STATIC_CHANNEL = "00001743";

	private Pubnub pubnub;

	private String transcript;

	private boolean fabClicked;
	private boolean fabMenuOpen;
	private String speakerId;

	private HashMap<String, Integer> personColour;

	@Bind(R.id.transcipt)
	TextView transcriptTextView;

	@Bind(R.id.fab_menu)
	FloatingActionMenu floatingActionMenu;

	@Bind(R.id.circleBackgroundTranscript)
	FrameLayout circleBackgroundTranscript;

	@Bind(R.id.personIcon)
	ImageView personIcon;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ButterKnife.bind(this);

		personColour = new HashMap<>();

		pubnubStaticChannelSubscribe();

		// enable microphone on normal click
		floatingActionMenu.setOnMenuButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Snackbar snackbar = Snackbar.make(v, "Recording", Snackbar.LENGTH_INDEFINITE);
				if(fabMenuOpen){
					floatingActionMenu.close(true);
					floatingActionMenu.getMenuIconView().setImageResource(R.drawable.microphone_off_fab);
					floatingActionMenu.setMenuButtonColorNormal(Color.WHITE);
					fabMenuOpen = false;
				} else if(fabClicked){
					floatingActionMenu.getMenuIconView().setImageResource(R.drawable.microphone_off_fab);
					floatingActionMenu.setMenuButtonColorNormal(Color.WHITE);
					//TODO: somewhow dismiss() is buggy
					snackbar.dismiss();
					fabClicked = false;
				} else {
					floatingActionMenu.getMenuIconView().setImageResource(R.drawable.microphone_on_fab);
					floatingActionMenu.setMenuButtonColorNormal(Color.RED);
					snackbar.show();
					fabClicked = true;
				}

			}
		});

		// open fab menu for additional options
		floatingActionMenu.setOnMenuButtonLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				floatingActionMenu.open(true);
				floatingActionMenu.getMenuIconView().setImageResource(R.drawable.ic_close);
				floatingActionMenu.setMenuButtonColorNormal(Color.argb(255, 40,109, 206));
				fabMenuOpen = true;
				return true;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}


	public void pubnubStaticChannelSubscribe(){

		// init pubnub
		pubnub = new Pubnub(PUBNUB_PUBLISH_KEY, PUBNUB_SUBSCRIBE_KEY);

		try {
			pubnub.subscribe(AVA_STATIC_CHANNEL, new Callback() {

					@Override
					public void connectCallback(String channel, Object message) {
						Log.d(LOG_TAG, "SUBSCRIBE : CONNECT on channel:" + channel
							+ " : " + message.getClass() + " : "
							+ message.toString());
					}

					@Override
					public void disconnectCallback(String channel, Object message) {
						Log.d(LOG_TAG, "SUBSCRIBE : DISCONNECT on channel:" + channel
							+ " : " + message.getClass() + " : "
							+ message.toString());
					}

					public void reconnectCallback(String channel, Object message) {
						Log.d(LOG_TAG, "SUBSCRIBE : RECONNECT on channel:" + channel
							+ " : " + message.getClass() + " : "
							+ message.toString());
					}

					@Override
					public void successCallback(String channel, Object message) {
						try {
							JSONObject jsonObject = new JSONObject(message.toString());
							if(!jsonObject.isNull("transcript")) {
								transcript = jsonObject.getString("transcript");
							}

							if(!jsonObject.isNull("speakerId")){
								speakerId = jsonObject.getString("speakerId");
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}

						updateUIonNewTranscriptReceived();
					}

					@Override
					public void errorCallback(String channel, PubnubError error) {
						Log.e(LOG_TAG, "SUBSCRIBE : ERROR on channel " + channel
							+ " : " + error.toString());
					}
				}
			);
		} catch (PubnubException e) {
			e.printStackTrace();
		}
	}

	public void updateUIonNewTranscriptReceived(){
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// persistent background colour for each different person
				Drawable background = circleBackgroundTranscript.getBackground();
				if (background instanceof ShapeDrawable) {
					((ShapeDrawable)background).getPaint().setColor(getPersonColour(speakerId));
				} else if (background instanceof GradientDrawable) {
					((GradientDrawable)background).setColor(getPersonColour(speakerId));
				} else if (background instanceof ColorDrawable) {
					((ColorDrawable)background).setColor(getPersonColour(speakerId));
				}

				// show person image as small icon
				personIcon.setVisibility(View.VISIBLE);
				personIcon.setImageResource(getPersonIcon(speakerId));

				// show last transcript from that person
				transcriptTextView.setMovementMethod(new ScrollingMovementMethod());
				transcriptTextView.setText(transcript);
			}
		});
	}

	private int getPersonIcon(String speakerId) {
		//TODO: get personsIcon from some source by the the persons ID
		return R.drawable.default_person;
	}

	private int getPersonColour(String speakerId) {
		if(personColour.containsKey(speakerId)){
			return personColour.get(speakerId);
		} else {
			Random rnd = new Random();
			int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
			personColour.put(speakerId, color);
		}
		return 0;
	}
}
