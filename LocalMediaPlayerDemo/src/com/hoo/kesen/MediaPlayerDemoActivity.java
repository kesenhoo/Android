package com.hoo.kesen;

import android.app.Activity;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * The MediaPlayer show that how to play local audio and video
 * 
 * @author hoo
 * 
 */
public class MediaPlayerDemoActivity extends Activity
{
	private SeekBar			skb_audio		= null;
	private Button			btn_start_audio	= null;
	private Button			btn_stop_audio	= null;

	private SeekBar			skb_video		= null;
	private Button			btn_start_video	= null;
	private Button			btn_stop_video	= null;

	private SurfaceView		surfaceView;
	private SurfaceHolder	surfaceHolder;

	private MediaPlayer		mPlayer		= null;
	private Timer			mTimer;
	private TimerTask		mTimerTask;

	private boolean			isSeeking		= false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mPlayer = new MediaPlayer();

		mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
		{
			@Override
			public void onCompletion(MediaPlayer arg0)
			{
				Toast.makeText(MediaPlayerDemoActivity.this, "播放结束", 1000).show();
				mPlayer.release();
			}
		});

		btn_start_audio = (Button) this.findViewById(R.id.Button01);
		btn_stop_audio = (Button) this.findViewById(R.id.Button02);
		btn_start_audio.setOnClickListener(new ClickEvent());
		btn_stop_audio.setOnClickListener(new ClickEvent());

		skb_audio = (SeekBar) this.findViewById(R.id.SeekBar01);
		skb_audio.setOnSeekBarChangeListener(new SeekBarChangeEvent());

		btn_start_video = (Button) this.findViewById(R.id.Button03);
		btn_stop_video = (Button) this.findViewById(R.id.Button04);
		btn_start_video.setOnClickListener(new ClickEvent());
		btn_stop_video.setOnClickListener(new ClickEvent());

		skb_video = (SeekBar) this.findViewById(R.id.SeekBar02);
		skb_video.setOnSeekBarChangeListener(new SeekBarChangeEvent());
		surfaceView = (SurfaceView) findViewById(R.id.SurfaceView01);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.setFixedSize(100, 100);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	public void setProgressTask()
	{

		mTimer = new Timer();
		mTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				if (isSeeking == true)
					return;
				if (mPlayer.getVideoHeight() == 0)
				{
					skb_audio.setProgress(mPlayer.getCurrentPosition());
				}
				else
				{
					skb_video.setProgress(mPlayer.getCurrentPosition());
				}
			}
		};
		mTimer.schedule(mTimerTask, 0, 100);
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
	}

	/**
	 * OnClick Listener
	 * @author hoo
	 *
	 */
	class ClickEvent implements View.OnClickListener
	{
		@Override
		public void onClick(View view)
		{
			if (view == btn_start_audio)
			{
				mPlayer.reset();
				mPlayer = MediaPlayer.create(MediaPlayerDemoActivity.this, R.raw.here_is_love);// 读取音频
				skb_audio.setMax(mPlayer.getDuration());
				mPlayer.start();

				setProgressTask();

			}
			else if (view == btn_stop_audio || view == btn_stop_video)
			{
				mPlayer.stop();
			}
			else if (view == btn_start_video)
			{
				mPlayer.reset();
				mPlayer = MediaPlayer.create(MediaPlayerDemoActivity.this, R.raw.video0001);
				skb_video.setMax(mPlayer.getDuration());
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDisplay(surfaceHolder);
				mPlayer.start();

				setProgressTask();
			}
		}
	}
	
	/**
	 * 
	 * @author hoo
	 *
	 */
	class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener
	{

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
		{
			// TODO Auto-generated method stub

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar)
		{
			isSeeking = true;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar)
		{
			mPlayer.seekTo(seekBar.getProgress());
			isSeeking = false;
		}
	}

}