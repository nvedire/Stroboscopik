package com.cpsc434.stroboscopik;

import java.util.ArrayList;

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
import android.util.Log;

import android.widget.Toast;

@TargetApi(11)
@SuppressLint("NewApi")
public class BTsetup extends Service {
	SharedPreferences settings;
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	private ArrayList <String> devices_disc;
	private long time_stamp;
	private static final String TAG = "BTSetup";
    private static final boolean D = true;
    private int cluster_id_length;
    private int flag = 0;
    private long freq;
    private Handler mHandler = new Handler();
    
    
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
    
    private final IBinder mBinder = new BTbinder();
    DeviceList dService;
    
   


    public void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
        // If the adapter is null, then Bluetooth is not supported
    }
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
    public void Synchrony(Intent data) {
		String Adname = settings.getString("C_ID",null) + "System.currentTimeMillis()";
  	    boolean b=mAdapter.setName(Adname);
  	    devices_disc = data.getStringArrayListExtra (EXTRA_DEVICE_ADDRESS);
  	    if(b&&devices_disc.size()!=0){
  	    int i=devices_disc.size();
  	    long sum=0;
  	    for(int j=0;j<i;j++){
  	    	String adapName = devices_disc.get(j);
  	    	String time = adapName.substring(cluster_id_length,adapName.length());
  	    	sum = sum + Long.valueOf(time).longValue(); 
  	    	
  	    }
  	    time_stamp = sum;
  	    /*
  	     * Intent SyncIntent = new Intent(for sending to flashing algo);
                
           SyncIntent.putLong(avg_time, time_stamp);
           startActivity(intent);*/
  	    
  	    if(flag ==0){
  	    	mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, (int)5000/freq);
            flag = 1;
  	    }
  	     
  	  
  	    } else{
  	    	formCluster();
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
    
  
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        if(requestCode ==REQUEST_TIMESTAMP){
        	if (resultCode == Activity.RESULT_OK) {
        		//mState=STATE_Synchrony;
                Synchrony(data);
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "User Backout, contact server");
                formCluster();
            }
        }
        if(requestCode ==REQUEST_SETUP){
        	if (resultCode == Activity.RESULT_OK) {
        		//mState=STATE_ClusterSetUp;
                formCluster();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "cluster_ID not assigned, contact server");
                //function to contact server;
                formCluster();
            }
        }
        
    }
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
	      startService(c_id_Intent);
	      }else{
	    	  formCluster();
	      }
	}
	public void SetupCluster() {
		Intent c_id_Intent = new Intent(this, Setup_list.class);
        startService(c_id_Intent);
		
	}
	
	/* Include to handle return codes from services to setup and scan
	 * if(requestCode ==REQUEST_TIMESTAMP){
      	if (resultCode == Activity.RESULT_OK) {
              btService.Synchrony(data);
          } else {
              // User did not enable Bluetooth or an error occurred
              Log.d(TAG, "User Backout, contact server");
              btService.formCluster();
          }
      }
      if(requestCode ==REQUEST_SETUP){
      	if (resultCode == Activity.RESULT_OK) {
              btService.formCluster();
          } else {
              // User did not enable Bluetooth or an error occurred
              Log.d(TAG, "cluster_ID not assigned, contact server");
              //function to contact server;
              btService.formCluster();
          }
      }*/
	 
}
