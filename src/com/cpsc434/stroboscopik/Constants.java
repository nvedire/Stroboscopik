package com.cpsc434.stroboscopik;

import android.graphics.Color;

public class Constants {
	
	//Sender ID of Project. Required when initially registering with GCM
	static final String APP_SENDER_ID = "457186895027";
	
	//URL(s) of Server for GCM
	static final String APP_REG_URL = 	"http://mholkesvik.webfactional.com/register/";   //URL for registration after GCM confirmation
	static final String APP_UNREG_URL = "http://mholkesvik.webfactional.com/unregister/"; //URL for unregistration when uninstalling
	static final String APP_SUPER_URL = "http://mholkesvik.webfactional.com/supernode/";  //URL when declaring a supernode
	
	//Fields for Server Requests
	static final String APP_DATABASE_ID = 	"database_id"; //Use to identify yourself to the App Server
	static final String APP_REG_ID = 		"reg_id";      //Use to indicate a change in your Registration ID with GCM
	static final String APP_CLUSTER_ID = 	"cluster_id";  //Use to indicate the cluster you belong to
	
	//Fields for Server Responses
	static final String APP_GCM_FREQUENCY_KEY = "frequency"; //App Server responses put frequency with this key
	static final String APP_GCM_CLUSTER_KEY = 	"cluster";   //App Server responses put cluster with this key
	
	//SharedPreferences that store the App's settings
	static final String APP_SETTINGS =  		"com.cpsc434.stroboscopik.settings"; //Settings file for the App
	static final String APP_GCM_REGID_KEY = 	"GCM_REGID";						 //Registration ID of the App
	static final String APP_CLUSTER_KEY = 		"CLUSTER";							 //Cluster ID of the App
	static final String APP_FREQUENCY_KEY = 	"FREQUENCY";						 //Current Frequency of flashing

	//Other important Constants
	static final String APP_NO_CLUSTER =  "NONE";	//Node is not associated to any other cluster
	static final String APP_NEW_CLUSTER = "NEW";	//Node has decided to become a supernode
	static final int    APP_DEFAULT_FREQ = 1;		//Arbitrary new frequency of 10 Hz
	static final int 	APP_DEFAULT_FADE = 100;     //Fade time, in milliseconds
	
	static final int 	APP_STARTUP_LONG_WAIT = 10000; //wait max 10 seconds before becoming the supernode.
	static final int 	APP_STARTUP_SHORT_WAIT = 2000; //wait max 2 seconds before becoming promoted to the supernode.
	static final double APP_STARTUP_PROMOTION_THRESH = 0.4; // statistically 40% of devices get early promotion
	
	static final int    APP_GCM_TIMEOUT  = 5000; //5-second GCM timeout

	static final int    APP_COLOR_LIME   = Color.argb(255, 200, 255,   0);
	static final int    APP_COLOR_GREEN  = Color.argb(255, 127, 255,  36);
	static final int    APP_COLOR_BLUE   = Color.argb(255,  34, 141, 255);
	static final int    APP_COLOR_PURPLE = Color.argb(255, 186,   1, 255);
	static final int    APP_COLOR_PINK   = Color.argb(255, 255,   0, 146);
	static final int    APP_COLOR_RED    = Color.argb(255, 250,   2,  60);
	static final int    APP_COLOR_ORANGE = Color.argb(255, 255, 202,  27);
	static final int    APP_COLOR_YELLOW = Color.argb(255, 255, 247,  22);
	static final int    APP_COLOR_WHITE  = Color.argb(255, 255, 255, 255);
	
}