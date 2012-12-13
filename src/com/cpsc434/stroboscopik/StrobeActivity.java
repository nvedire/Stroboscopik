package com.cpsc434.stroboscopik;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.TargetApi;
import android.app.Activity;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.cpsc434.stroboscopik.BTsetup.BTbinder;
import com.cpsc434.stroboscopik.util.SystemUiHider;
import com.google.android.gcm.GCMRegistrar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class StrobeActivity extends Activity {
  private static final String TAG = "StrobeActivity";
  
  //default template vars
  private static final boolean AUTO_HIDE = true;
  private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
  private static final boolean TOGGLE_ON_CLICK = true;

  private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

  //pertinent variables for GCM to modify
  public static int freq = Constants.APP_DEFAULT_FREQ;
  public static int cluster = -1;
  public static String Cluster_ID=null;
  private static double flashPeriod = 150.0; //the period of time when the screen is "white" or in strobing state
  private static int restPeriod = 300; //the period of time when the screen is "black" or in resting state
  
  //define resting color
  private int restColor = Color.argb(0, 0, 0, 0);
  
  private static enum State {
    IDLE,
    ORPHAN,
    SUBNODE,
    SUPERNODE,
    IN_TRANSITION
  };
  
  //transition states - these could have been merged into state enum above, but I separated it for readability and easier-to-follow logic
  private static enum Transition {
    SUPERNODE_PENDING_SERVER_VALIDATION, //in case server can't be contacted
    SUBNODE_PENDING_SERVER_VALIDATION,
    SUPERNODE_PENDING_GCM,  //last step to becoming supernode
    SUBNODE_PENDING_GCM,
    FAILED_SUBNODE,
    FAILED_SUPERNODE,
    SEARCHING_FOR_SUPERNODE,
    NONE
  };
 
  private static int[] defaultColors = { 
    Constants.APP_COLOR_LIME,
    Constants.APP_COLOR_GREEN,
    Constants.APP_COLOR_BLUE,
    Constants.APP_COLOR_PURPLE,
    Constants.APP_COLOR_PINK,
    Constants.APP_COLOR_RED,
    Constants.APP_COLOR_ORANGE,
    Constants.APP_COLOR_YELLOW
  };
  
  public static State state = State.IDLE;
  public static Transition trans = Transition.NONE;
  public static Context context;

  private SystemUiHider mSystemUiHider;
  private Handler mHandler = new Handler(); //for dummy flashing
  public long flashTimeStamp = 0;
  private static	SharedPreferences settings;
  private boolean isSuperNode = false;
  private boolean searchingForSupernode = false; //make this a state
  
  
  //Bluetooth Stuff
  private BluetoothAdapter mAdapter;
  public static final int REQUEST_ENABLE_BT = 1;
  public static final int REQUEST_SETUP = 2;
  public static final int REQUEST_TIMESTAMP = 3;
  public static BTsetup btService;
  boolean mBound = false;
  public static int state_setup;

  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    

    //Bluetooth Availability check
    mAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (mAdapter == null) {
        Toast.makeText(this, "Bluetooth is not available. Exiting Application", Toast.LENGTH_LONG).show();
        finish();
        return;
    }else{
    //If adapter available, remember adapter name and write it into shared preferences
    settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
    SharedPreferences.Editor editor = settings.edit();
	String Adname = mAdapter.getName();
	editor.putString("OrigAdapName",Adname);
	editor.commit();
	
	Intent intent = new Intent(this, BTsetup.class);
    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    
    }
	//end of Bluetooth Availability check

    context = getApplicationContext(); //hack -Dave
    // Check for GCM Registration. Register if not registered
    GCMRegistrar.checkDevice(this);
    GCMRegistrar.checkManifest(this);
    String regId = GCMRegistrar.getRegistrationId(this);
    if (regId == "") {
      GCMRegistrar.register(this, Constants.APP_SENDER_ID);
    } else {
      Log.v(TAG, "Already Registered with ID: "+regId);
    }
    // End GCM Registration

    setContentView(R.layout.activity_strobe);
    
    final View controlsView = findViewById(R.id.fullscreen_content_controls);
    final View contentView = findViewById(R.id.fullscreen_content);

    mSystemUiHider = SystemUiHider.getInstance(this, contentView,
        HIDER_FLAGS);
    mSystemUiHider.setup();
    mSystemUiHider
    .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
      int mShortAnimTime;

      @Override
      @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
      public void onVisibilityChange(boolean visible) {
        if (state == State.IDLE) {
          controlsView.setAlpha(1.f);
          return;
        }
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
          if (mShortAnimTime == 0) {
            mShortAnimTime = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
          }
          controlsView
          .animate()
          .alpha(visible ? 1.f : 0.f)
          .setDuration(mShortAnimTime);
        } else {
          controlsView.setVisibility(visible ? View.VISIBLE
              : View.GONE);
        }

        if (visible && AUTO_HIDE) {
          delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
      }
    });

    contentView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (TOGGLE_ON_CLICK) {
          mSystemUiHider.toggle();
        } else {
          mSystemUiHider.show();
        }
      }
    });

    findViewById(R.id.dummy_button).setOnTouchListener(
        mDelayHideTouchListener);

    findViewById(R.id.dummy_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
    	btService.ensureDiscoverable();
        //onBecomeSubnode();
        state = State.ORPHAN;
        searchForSupernode();
      }
    });
  }
  

  //Bluetooth initialization and setup for handling on resume, on pause, on stop, on detroy
  @Override
  public void onStart() {
      super.onStart();
      Log.e(TAG, "On Start - Enabling BT");

      // If BT is not on, request that it be enabled.
      if (!mAdapter.isEnabled()) {
          Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      }else {
    	  btService.ensureDiscoverable();
          btService.SetupCluster();
      }
  }
  
  @Override
  public synchronized void onResume() {
      super.onResume();
      Log.e(TAG, "On Resume - Start BT");
      // If BT is not on, request that it be enabled.
      if (!mAdapter.isEnabled()) {
          Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      }else {
    	  btService.ensureDiscoverable();
          btService.SetupCluster();
      }
  }
  
  @Override
  public synchronized void onPause() {
      super.onPause();
      Log.e(TAG, "- ON PAUSE -");
      
      // Setting Adapter name to original Default
      String Adname = settings.getString("OrigAdapName","MyBT");
  	  boolean b=mAdapter.setName(Adname);
  	  
  	  // Stopping Discoverbale Mode
  	if(b){
  		Intent discoverableintent = new
  		Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
  		discoverableintent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
  		startActivity(discoverableintent);
  	}
  	
  }
  
  @Override
  public void onStop() {
      super.onStop();
      Log.e(TAG, "-- ON STOP --");
      
   // Setting Adapter name to original Default
      String Adname = settings.getString("OrigAdapName","My_BT");
  	boolean b=mAdapter.setName(Adname);
  	
   // Stopping Discoverbale Mode
  	if(b){
  		Intent discoverableintent = new
  		Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
  		discoverableintent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
  		startActivity(discoverableintent);
  	}
      
  }
  
  
  /** Defines callbacks for service binding, passed to bindService() */
  private ServiceConnection mConnection = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName className,
              IBinder service) {
          // We've bound to BTsetup, cast the IBinder and get BTsetup instance
          BTbinder binder = (BTbinder) service;
          btService = binder.getService();
          mBound = true;
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
          mBound = false;
      }
  };

//End of handling Bluetooth initalization and quitting

  
 
  //allow other services to call static methods in this class to raise Toasts.
  public static Handler hackMessageHandler = new Handler() {
    public void handleMessage(android.os.Message msg) {
        String message = (String)msg.obj;
    }
};
  
  private void searchForSupernode() {
    String uid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    long seed = new BigInteger(uid, 16).longValue() + System.currentTimeMillis();
    Random r = new Random(seed);

    double flip = r.nextDouble(); //weighted coin flip
    int wait = 0;
    
    state = State.IN_TRANSITION;
    trans = Transition.SEARCHING_FOR_SUPERNODE;

    if (flip > Constants.APP_STARTUP_PROMOTION_THRESH) {
      wait = (int) (r.nextDouble() * Constants.APP_STARTUP_LONG_WAIT);
      Log.d(TAG, "waiting " + Integer.toString(wait) + " milliseconds for a supernode.");
      state_setup = btService.SetupCluster();
      mHandler.postDelayed(evolveIntoSupernode, wait);
    } else { //favor some orphans
      wait = (int) (r.nextDouble() * Constants.APP_STARTUP_SHORT_WAIT);
      Log.d(TAG, "promoted; will become supernode soon.");
      state_setup = btService.SetupCluster();
      mHandler.postDelayed(evolveIntoSupernode, wait);
    }
  }

  private Runnable evolveIntoSupernode = new Runnable() {
    public void run() {
      if(state_setup==4){
      if (!(state == State.IN_TRANSITION && ( trans == Transition.SEARCHING_FOR_SUPERNODE || trans == Transition.SUPERNODE_PENDING_GCM ) ) ) return;
      SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_MULTI_PROCESS);
      String regId = settings.getString(Constants.APP_GCM_REGID_KEY, ""); //if "" then BAD
      if (regId == "") {
        Log.e(TAG, "evolveIntoSupernode: regId null");
        return;
      }
  
      List<NameValuePair> data = new ArrayList<NameValuePair>();
      data.add(new BasicNameValuePair(Constants.APP_DATABASE_ID, regId));
  
      HTTPRequestParams[] params = new HTTPRequestParams[1];
      params[0] = new HTTPRequestParams(Constants.APP_SUPER_URL, data,
          "Success! You are now a supernode.",
          "Failure: cannot contact servers");
      PostHTTPTask p = new PostHTTPTask();
      btService.formCluster();
      
      if (trans != Transition.SUPERNODE_PENDING_GCM) trans = Transition.SUPERNODE_PENDING_SERVER_VALIDATION; //don't change transition state on timeouts!
      
      p.execute(params);
    }
    else {
    	onFoundSubnode();
    }
    }
    };

  private void startFlashing() {
    mHandler.postDelayed(mFlashTask, 1000);
  }

  private Runnable mFlashTask = new Runnable() { //this is terrible
    public void run() {
      final View contentView = findViewById(R.id.fullscreen_content);
      contentView.setBackgroundColor(getFlashColor());
      flashTimeStamp = System.currentTimeMillis();

      mHandler.post(mFadeTask);
      mHandler.postDelayed(this, restPeriod);
    }
  };

  public static void updatePeriods() {
    restPeriod = 1000/freq; //1000 ms/freq in hz

    if ( flashPeriod > restPeriod ) { //normalize
      flashPeriod = restPeriod - 100;
    } else {
      flashPeriod = Constants.APP_DEFAULT_FADE;
    }
    Log.d(TAG, "new frequency: " + freq);
  }
  
  private Runnable morphIntoSubnode = new Runnable() {  
    public void run() {
    	btService.formCluster();
      //not sure if conditions below are 100% correct; but note that SEARCHING_FOR_SUPERNODE is a shared transition state in orphan's first step
      if (!(state == State.IN_TRANSITION && ( trans == Transition.SEARCHING_FOR_SUPERNODE ||
                                              trans == Transition.SUBNODE_PENDING_GCM ) ) ) return;
      SharedPreferences settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_MULTI_PROCESS);
      String regId = settings.getString(Constants.APP_GCM_REGID_KEY, ""); //if "" then bad
      if (regId == "") {
        Log.e(TAG, "regId null");
        return;
      }
  
      //Perform Registration and Unregistration
      List<NameValuePair> data = new ArrayList<NameValuePair>();
      data.add(new BasicNameValuePair(Constants.APP_DATABASE_ID, regId));
      data.add(new BasicNameValuePair(Constants.APP_REG_ID, regId));
      
      String cluster = settings.getString(Constants.APP_CLUSTER_KEY, Constants.APP_NO_CLUSTER);
      data.add(new BasicNameValuePair(Constants.APP_CLUSTER_ID, cluster));
  
      HTTPRequestParams[] params = new HTTPRequestParams[1];
      params[0] = new HTTPRequestParams(Constants.APP_REG_URL, data,
          "Success! You are now connected to a cluster.",
          "Failure: cannot contact servers");
  
      PostHTTPTask p = new PostHTTPTask();
      
      if (trans != Transition.SUBNODE_PENDING_GCM) trans = Transition.SUBNODE_PENDING_SERVER_VALIDATION; //don't change transition state on timeouts!
      
      p.execute(params);
    }
  };

  private void onFoundSubnode() {
    state = State.IN_TRANSITION;
    trans = Transition.SEARCHING_FOR_SUPERNODE;
    mHandler.post(morphIntoSubnode);
  }
 
  //do any post results cleanup if necessary; also retry if necessary
  private void postResultsCleanup()
  {
    Log.d(TAG, state.toString());
    Log.d(TAG, trans.toString());
    switch (trans) {
    case NONE:
      break;
    case SEARCHING_FOR_SUPERNODE:
      break;
    case SUBNODE_PENDING_SERVER_VALIDATION:
      startFlashing();
      trans = Transition.SUBNODE_PENDING_GCM;
      mHandler.postDelayed(morphIntoSubnode, Constants.APP_GCM_TIMEOUT);
      break;
    case SUBNODE_PENDING_GCM: //set another timeout
      mHandler.postDelayed(morphIntoSubnode, Constants.APP_GCM_TIMEOUT);
      break;
    case SUPERNODE_PENDING_SERVER_VALIDATION:
      startFlashing();
      trans = Transition.SUPERNODE_PENDING_GCM;
      mHandler.postDelayed(evolveIntoSupernode, Constants.APP_GCM_TIMEOUT);
      break;
    case SUPERNODE_PENDING_GCM: //set another timeout
      mHandler.postDelayed(evolveIntoSupernode, Constants.APP_GCM_TIMEOUT);
      break;
    case FAILED_SUBNODE:
      state = State.IN_TRANSITION;
      trans = Transition.SEARCHING_FOR_SUPERNODE;
      mHandler.post(morphIntoSubnode);
      break;
    case FAILED_SUPERNODE:
      state = State.IN_TRANSITION;
      trans = Transition.SEARCHING_FOR_SUPERNODE;
      mHandler.post(evolveIntoSupernode);
    default:
      break;
    }
  }
  
  public static void onReceivedGCMUPdate() {
    Log.d(TAG, state.toString());
    Log.d(TAG, trans.toString());
    switch (trans) {
    case SUBNODE_PENDING_GCM:
      state = State.SUBNODE;
      trans = Transition.NONE;
      Toast.makeText(StrobeActivity.context, "Your cluster is now verified!", Toast.LENGTH_SHORT).show();
      break;
    case SUPERNODE_PENDING_GCM:
      state = State.SUPERNODE;
      trans = Transition.NONE;
      Toast.makeText(StrobeActivity.context, "Your supernode status is now verified!", Toast.LENGTH_SHORT).show();
      break;
    case SUPERNODE_PENDING_SERVER_VALIDATION:
      Toast.makeText(StrobeActivity.context, "Your supernode status is now verified!", Toast.LENGTH_SHORT).show();
      SharedPreferences.Editor editor = settings.edit();
  	  String Adname = "C0"+ cluster;
  	  editor.putString("C_ID",Adname);
  	  editor.commit();
  	  Long c_id_length = (long) 3;
  	  editor.putLong("C_ID_length",3);
      btService.formCluster();
      break;
    case SUBNODE_PENDING_SERVER_VALIDATION:
      Toast.makeText(StrobeActivity.context, "Your cluster is now verified!", Toast.LENGTH_SHORT).show();
      break;
    }
  }

  private class PostHTTPTask extends AsyncTask<HTTPRequestParams, Integer, String> {
    private boolean failed = false;
    protected void onPostExecute(String result) {
      if (StrobeActivity.trans == Transition.SUPERNODE_PENDING_SERVER_VALIDATION ||
          StrobeActivity.trans == Transition.SUBNODE_PENDING_SERVER_VALIDATION) { //don't do this on timeouts
        Toast message = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT);
        message.show();
      }
      if (failed) {
        //retry
      }
      postResultsCleanup();
      
    } // end of onPostExecute

    @Override
    protected String doInBackground(HTTPRequestParams... params) {
      HTTPRequestParams p = params[0];

      String url = p.url;
      List<NameValuePair> data = p.data;

      String success = p.success;
      String failure = p.failure;

      Log.d(TAG, "Gathering HTTP POST message");
      
      //debug
      Log.d(TAG, "what I'm sending follows:");
      Iterator<NameValuePair> i = data.iterator();
      while (i.hasNext()) {
        NameValuePair pair = i.next();
        Log.d(TAG, pair.getName().toString() + ":" + pair.getValue());
      }
      Log.d(TAG, "end POST data");
      
      HttpClient httpclient = new DefaultHttpClient();
      HttpPost httppost = new HttpPost(url);

      try {
        Log.d(TAG, "posting to " + url + " ...");
        
        httppost.setEntity(new UrlEncodedFormEntity(data));

        //Execute the request
        HttpResponse response = httpclient.execute(httppost);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
          Log.e(TAG, "HTTP Response NOT OK " + Integer.toString(response.getStatusLine().getStatusCode()) +": " + response.getStatusLine().getReasonPhrase());
          return failure;
        }

      } catch (ClientProtocolException e) {
        Log.e(TAG, e.getMessage());
        return failure;
      } catch (IOException e) {
        Log.e(TAG, e.getMessage());
        return failure;
      }
      return success;
    }
  }

  private int getFlashColor() {
      if (state == State.SUPERNODE || state == State.SUBNODE) { 
        int index = cluster % defaultColors.length;
        if (index < 0) index += defaultColors.length;
        return defaultColors[index];
      } else {
        return Constants.APP_COLOR_WHITE;
      }
  }
  

  private Runnable mFadeTask = new Runnable() { //make this faster
    public void run() {
      final View contentView = findViewById(R.id.fullscreen_content);
      long elapsed = System.currentTimeMillis() - flashTimeStamp;
      int flashColor = getFlashColor();

      //interpolate
      double frac = (double) elapsed/flashPeriod;
      int r = (int) ((1 - frac) * Color.red(flashColor)) + (int)(frac * Color.red(restColor));
      int g = (int) ((1 - frac) * Color.green(flashColor)) + (int)(frac * Color.green(restColor));
      int b = (int) ((1 - frac) * Color.blue(flashColor)) + (int)(frac * Color.blue(restColor));

      //System.out.println(r + ", " + g + ", " + b);

      //normalize
      r = r < 0 ? 0 : r;
      g = g < 0 ? 0 : g;
      b = b < 0 ? 0 : b;

      contentView.setBackgroundColor(Color.argb(255, r, g, b));

      if (r != Color.red(restColor) || g != Color.green(restColor) || b != Color.blue(restColor)) {
        mHandler.postDelayed(this, 16);
      }
    }
  };

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    delayedHide(100);
  }


  /**
   * Touch listener to use for in-layout UI controls to delay hiding the
   * system UI. This is to prevent the jarring behavior of controls going away
   * while interacting with activity UI.
   */
  View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
      if (AUTO_HIDE) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
      }
      return false;
    }
  };

  Handler mHideHandler = new Handler();
  Runnable mHideRunnable = new Runnable() {
    @Override
    public void run() {
      mSystemUiHider.hide();
    }
  };

  /**
   * Schedules a call to hide() in [delay] milliseconds, canceling any
   * previously scheduled calls.
   */
  private void delayedHide(int delayMillis) {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }
  
  //bluetooth on - off intent result code handler
  
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if(requestCode ==REQUEST_ENABLE_BT){
          // When the request to enable Bluetooth returns
      	 
      	if (resultCode == Activity.RESULT_OK) {
              // Bluetooth is now enabled,ensure discoverability
          	btService.ensureDiscoverable();
          } else {
              // User did not enable Bluetooth or an error occurred
              Log.d(TAG, "BT not enabled");
              Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
              finish();
          }
      }
  }

}

