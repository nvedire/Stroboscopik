package com.cpsc434.stroboscopik;

public class Constants {
	
	//Sender ID of Project. Required when initially registering with GCM
	static final String SENDER_ID = "457186895027";
	
	//SharedPreferences to determine registered or unregistered
	static final String APP_GCM_SETTINGS =  	"com.cpsc434.stroboscopik.gcm";
	static final String APP_GCM_REGID_KEY = 	"GCM_REGID";
	static final String APP_GCM_CLUSTER_KEY = 	"GCM_CLUSTER";

	//Other important Constants
	static final String APP_NO_CLUSTER = "NONE";
	static final String APP_NEW_CLUSTER = "NEW";
}