package com.cpsc434.stroboscopik;

import java.io.File;


import com.cpsc434.stroboscopik.util.SystemUiHider;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;



/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class StrobeActivity extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	
    protected InitTask _initTask;
    
	private SystemUiHider mSystemUiHider;
	public AudioRecord audioRecord; 
    public int mSamplesRead; //how many samples read 
    public int recordingState;
    public int buffersizebytes; 
    public int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; 
    public int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; 
    public static short[] buffer; //+-32767 
    public static final int SAMPPERSEC = 44100; //samp per sec 8000, 11025, 22050 44100 or 48000

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_strobe);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startDummyRecording();
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);
	}
	
	private void startDummyRecording() {
        //buffersizebytes = AudioRecord.getMinBufferSize(SAMPPERSEC,channelConfiguration,audioEncoding); //4096 on ion 
        //buffer = new short[buffersizebytes]; 
        //audioRecord = new AudioRecord(android.media.MediaRecorder.AudioSource.MIC,SAMPPERSEC,channelConfiguration,audioEncoding,buffersizebytes); //constructor 

        //_initTask = new InitTask();
        //_initTask.execute( this );
       
        try {
	        WavFile dummy = WavFile.openWavFile( new File( Environment.getExternalStorageDirectory().getAbsolutePath() + "/SHM-DontYouWorryChild.wav" ) );
	        
	        int numChannels = dummy.getNumChannels();
	        
	        double[] buffer = new double[100 * numChannels];
	        
	        int framesRead;
	        double min = Double.MAX_VALUE;
	        double max = Double.MIN_VALUE;
	        
	        do
	         {
	            // Read frames into buffer
	            framesRead = dummy.readFrames(buffer, 100);

	            // Loop through frames and look for minimum and maximum value
	            for (int s=0 ; s<framesRead * numChannels ; s++)
	            {
	               if (buffer[s] > max) max = buffer[s];
	               if (buffer[s] < min) min = buffer[s];
	            }
	         }
	         while (framesRead != 0);
	        
	         DoubleFFT_1D fft = new DoubleFFT_1D(1024); // 1024 is size of array
	         fft.realForward( buffer );

	         // Close the wavFile
	         dummy.close();
	         
	        
        } catch ( Exception e ) {
        	e.printStackTrace();
        }
    }
	
    protected class InitTask extends AsyncTask<Context, Integer, String>
    {
        // -- run intensive processes here
        // -- notice that the datatype of the first param in the class definition matches the param passed to this method 
        // -- and that the datatype of the last param in the class definition matches the return type of this mehtod
                @Override
                protected String doInBackground( Context... params ) 
                {
                        //-- on every iteration
                        //-- runs a while loop that causes the thread to sleep for 50 milliseconds 
                        //-- publishes the progress - calls the onProgressUpdate handler defined below
                        //-- and increments the counter variable i by one
                        //int i = 0;

                    audioRecord.startRecording();

                        while( true ) 
                        {
                                try{
                                        mSamplesRead = audioRecord.read(buffer, 0, buffersizebytes);

                                        int amp;

                                        for(int i = 0; i < buffersizebytes - 1; i++){
                                            amp = (int)buffer[i];
                                            publishProgress( amp );
                                        }

                                } catch( Exception e ){                        
                                }
                        }
                }

                // -- gets called just before thread begins
                @Override
                protected void onPreExecute() 
                {
                        //Log.i( "makemachine", "onPreExecute()" );
                        super.onPreExecute();

                }

                // -- called from the publish progress 
                // -- notice that the datatype of the second param gets passed to this method
                @Override
                protected void onProgressUpdate(Integer... values) 
                {
                        super.onProgressUpdate(values);
                        //Log.i( "makemachine", "onProgressUpdate(): " +  String.valueOf( values[0] ) );
                        Log.d( "audioInput", String.valueOf(values[0]) );
                }

                // -- called as soon as doInBackground method completes
                // -- notice that the third param gets passed to this method
                @Override
                protected void onPostExecute( String result ) 
                {
                        super.onPostExecute(result);
                        //Log.i( "makemachine", "onPostExecute(): " + result );
                }   


     }

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
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
}
