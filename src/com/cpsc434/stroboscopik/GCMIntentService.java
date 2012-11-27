package com.cpsc434.stroboscopik;

import com.google.android.gcm.GCMBaseIntentService;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class GCMIntentService extends GCMBaseIntentService {
	public GCMIntentService() {
		super("457186895027");
	}
	
	protected void onRegistered(Context context, String registrationId) {
		//TODO: Send a message to the server with a cluster ID as well as the registration ID
		//	In doing so, also unregister the old registration ID
	}
	
	protected void onError(Context context, String errorId) {
		//TODO: Error of server not available already dealt with. Need to deal with other errors
	}
	
	protected void onUnregistered(Context context, String errorId) {
		//TODO: Notify the server and dissociate from the cluster
	}
	
	protected void onMessage(Context context, Intent intent) {
		//TODO: Server has just sent a message to the cluster. Deal with it
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Binding not allowed");
	}
}
