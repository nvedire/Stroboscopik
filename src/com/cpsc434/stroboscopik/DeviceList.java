package com.cpsc434.stroboscopik;

import java.util.ArrayList;



import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;


/**
 * It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
@SuppressLint("NewApi")
public class DeviceList extends IntentService {
	
	SharedPreferences settings;
	// Debugging
    private static final String TAG = "DeviceList";
    private static final boolean D = true;
    
 // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayList<String> discovered_devices = new ArrayList<String>();
    private String cluster_id;
    private int c_id_length;
    private long startedAt;
    private int result = Activity.RESULT_CANCELED;
    public long time_stamp;
    private Intent intent;
    
    public DeviceList() {
        super("DeviceList");
      }
    
    @Override
    protected void onHandleIntent(Intent intent) {
    	
    	 
        startedAt = System.currentTimeMillis();
        if ((System.currentTimeMillis() - startedAt) > 10*1000) {
            outData();
            stopSelf();
          }

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

                doDiscovery();
            
                // Register for broadcasts when a device is discovered
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                this.registerReceiver(mReceiver, filter);
                // Register for broadcasts when discovery has finished
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                this.registerReceiver(mReceiver, filter);
        
                
                settings = getSharedPreferences(Constants.APP_SETTINGS, MODE_PRIVATE);
                cluster_id = settings.getString("C_ID","C01");
                c_id_length = (int) settings.getLong("C_ID_length",3);
     
        // Sucessful finished
    }
    
    public void outData() {
        // Cancel discovery because it's costly and we're about to connect
        mBtAdapter.cancelDiscovery();
        

        // Set result and finish this Activity
        if (discovered_devices.size() != 0){
        	String Adname = settings.getString("C_ID",null) + "System.currentTimeMillis()";
      	    boolean b=mBtAdapter.setName(Adname);
      	    if(b){
        	    int i=discovered_devices.size();
        	    long sum=0;
        	    for(int j=0;j<i;j++){
        	    	String adapName = discovered_devices.get(j);
        	    	String time = adapName.substring(c_id_length,adapName.length());
        	    	sum = sum + Long.valueOf(time).longValue(); 
        	    	
        	    }
        	    time_stamp = sum;
      	    }
        	result = Activity.RESULT_OK;
        }
        else {
        	result = Activity.RESULT_CANCELED;
        }
        
        Bundle extras = intent.getExtras();
        if (extras != null) {
          Messenger messenger = (Messenger) extras.get("MESSENGER");
          Message msg = Message.obtain();
          msg.arg1 = result;
          msg.arg2 = 1;
          msg.obj = time_stamp;
          try {
            messenger.send(msg);
          } catch (android.os.RemoteException e1) {
            Log.w(getClass().getName(), "Exception sending message", e1);
          }
        }
        
    }  

   // @Override
    public void onDestroy() {
     //   super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }
    
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery() - deviceList");

        
        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }
    
 // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                	String dev_name = device.getName();
                    if(dev_name.substring(0, c_id_length) == cluster_id){
                	discovered_devices.add(device.getName());}
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                outData();
            }
        }
    };


}