package com.cpsc434.stroboscopik;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {

	//TAG for Local Log calls
	private static final String TAG = "GCMIntentService";
	
	//Public constructor for initializing the Intent Service
	public GCMIntentService() {
		super(Constants.SENDER_ID);
	}
	
	protected void onRegistered(Context context, String registrationId) {
		SharedPreferences settings = getSharedPreferences(Constants.APP_GCM_SETTINGS, MODE_PRIVATE);
		String oldId = settings.getString(Constants.APP_GCM_REGID_KEY, "");
		String cluster = settings.getString(Constants.APP_GCM_CLUSTER_KEY, Constants.APP_NO_CLUSTER);
		String regId = GCMRegistrar.getRegistrationId(getApplicationContext());
		
		//TODO: Perform registration and unregistration
		
		SharedPreferences.Editor ed = settings.edit();
		ed.putString(Constants.APP_GCM_REGID_KEY, regId);
		ed.commit();
	}
	
	protected void onError(Context context, String errorId) {
		//TODO: Error of server not available already dealt with. Need to deal with other errors
	}
	
	protected void onUnregistered(Context context, String errorId) {
		SharedPreferences settings = getSharedPreferences(Constants.APP_GCM_SETTINGS, MODE_PRIVATE);
		String oldId = settings.getString(Constants.APP_GCM_REGID_KEY, "");

		//TODO: Notify the server and dissociate from the cluster
		
		SharedPreferences.Editor ed = settings.edit();
		ed.putString(Constants.APP_GCM_REGID_KEY, "");
		ed.commit();
	}
	
	protected void onMessage(Context context, Intent intent) {
		//TODO: Server has just sent a message to the cluster. Deal with it
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Binding not allowed");
	}
}
