package com.example.testaudiofocus;

import android.media.AudioManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity{
	
	public AudioManager mAudioManager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		Button button1 = (Button)this.findViewById(R.id.button1);
		button1.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mAudioManager.requestAudioFocus(mAudioFocusListener,
						AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
				Log.d("Test", "requestAudioFocus: onClick");
			}
		});
		
		Button button2 = (Button)this.findViewById(R.id.button2);
		button2.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mAudioManager.abandonAudioFocus(mAudioFocusListener);
				Log.d("Test", "abandonAudioFocus: onClick");
			}
		});
		
		
	}
    
    public AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
	    public void onAudioFocusChange(int focusChange) {
	        // AudioFocus is a new feature: focus updates are made verbose on purpose
	        switch (focusChange) {
	            case AudioManager.AUDIOFOCUS_LOSS:
	                Log.d("Test", "AudioFocus: received AUDIOFOCUS_LOSS");
	                break;
	            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	                Log.d("Test", "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
	                break;
	            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	                Log.d("Test", "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
	                break;
	            case AudioManager.AUDIOFOCUS_GAIN:
	                Log.d("Test", "AudioFocus: received AUDIOFOCUS_GAIN");
	                break;
	            default:
	                Log.d("Test", "Unknown audio focus change code");
	        }
	    }
	};
}
