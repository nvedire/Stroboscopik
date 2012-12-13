package com.cpsc434.stroboscopik;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

@TargetApi(11)
@SuppressLint("NewApi")
public class BTsetup extends Service {
	SharedPreferences settings;
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	private static final String TAG = "BTSetup";
    private static final boolean D = true;
    private int cluster_id_length;
    private int flag = 0;
    private long freq;
    private Handler mHandler = new Handler();
    public long time_stamp;
    
    
 // Member fields
    private BluetoothAdapter mAdapter;
   // private int mState;
    
    // Constants that indicate the current connection state
    public static final int STATE_DISC = 0;       // Discoverability enabled
    public static final int STATE_NoDISC = 1;     // Discoverability disabled
    public static final int STATE_ClusterSetUp = 3;  // Cluster ID obtained
    public static final int STATE_Synchrony = 4;  // Cluster ID obtained
    public static final long DEF_FREQ = 10;
    
    //Intent codes
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_SETUP = 2;
    public static final int REQUEST_TIMESTAMP = 3;
    public static final int EVOLVE_INTO_SUPER_NODE =4;
    public static final int SUPER_NODE_FOUND =5;
    public int state_setup;
    
    private final IBinder mBinder = new BTbinder();
    DeviceList dService;
    
    public class BTbinder extends Binder {
        BTsetup getService() {
            // Return this instance of LocalService so clients can call public methods
            return BTsetup.this;
     }
   
    //@Override
    public void onDestroy() {
        //super.onDestroy();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
   
    private Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
          Object path = message.obj;
          if (message.arg2==0 && message.arg1 == Activity.RESULT_OK) {
        	  formCluster(); 
        	  state_setup = SUPER_NODE_FOUND;
          } else if (message.arg2==0){
            state_setup = EVOLVE_INTO_SUPER_NODE ;
          }
          if (message.arg2==1 && message.arg1 == Activity.RESULT_OK) {
        	  Synchrony(); 
        	  state_setup = SUPER_NODE_FOUND;
        	  time_stamp = (Long) message.obj;
          } else if (message.arg2==1){
            state_setup = EVOLVE_INTO_SUPER_NODE ;
          }

        };
      };

    public void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
        // If the adapter is null, then Bluetooth is not supported
    }
    
    protected void Synchrony() {
   	 
  	    if(flag ==0){
  	    	mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, (int)5000/freq);
            flag = 1;
  	    }
  	     	
	}
   
    public Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
        	try {
        		formCluster();
        		flag=0;
  			} catch (Exception e) {
  			}
        	
            mHandler.postDelayed(this, 10000);
        }
    };
    }
  
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	
	public void ensureDiscoverable() {
	if(D) Log.d(TAG, "ensure discoverable");
    if (mAdapter.getScanMode() !=
        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(discoverableIntent);
        //mState=STATE_DISC;
    }
	}
	
	public void formCluster() {
		  settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
	      String Adname = settings.getString("C_ID",null);
	      cluster_id_length = (int) settings.getLong("C_ID_length",3);
	      freq = settings.getLong("freq",DEF_FREQ);
	  	  boolean b=mAdapter.setName(Adname);
	  	  
	  	  if(b&&Adname!=null){
	      Intent c_id_Intent = new Intent(this, DeviceList.class);
	      Messenger messenger1 = new Messenger(mHandler);
		  c_id_Intent.putExtra("MESSENGER", messenger1);
	      startService(c_id_Intent);
	      }else{
	    	  formCluster();
	      }
	}
	
	public int SetupCluster() {
		Intent c_id_Intent = new Intent(this, Setup_list.class);
		Messenger messenger = new Messenger(mHandler);
	    c_id_Intent.putExtra("MESSENGER", messenger);
        startService(c_id_Intent);
		return state_setup;
	}
	 
}
