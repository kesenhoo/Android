package com.hoo.AsyncImageDecode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hoo.AysncImageDecode.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * (1)Test Listview Async Decode Image (2)Loading More Action While Drop to
 * LisView Bottom
 * 
 * @author kesenhoo
 * @since 2012-09-12
 * 
 */
public class SongListActivity extends Activity implements OnScrollListener
{
	// Define Request Online Song Value
	private static final int				REQUEST_SONG_COUNT	= 20;
	private static final int				THE_SONG_NUM		= 100;

	private static final String				TAG					= "[AsyncImageDecodeDemo]";
	private int								mStartIdx			= 0;
	private ListView						mTrackListView		= null;
	private TrackListAdapter				mAdapter			= null;
	private ArrayList<WrapperSong>			mTrackListData		= new ArrayList<WrapperSong>();
	private ArrayList<WrapperSong>			mSongListData		= null;
	private boolean							shouldRefresh		= false;
	private boolean							isActivityResumed	= false;
	private boolean							isRefreshing		= false;
	private boolean							mIsEmptyList		= true;
	private boolean							mLoadingViewIsShown	= false;
	private boolean							mPrepareGettingMore	= false;
	private ProgressDialog					mProgressDialog		= null;
	private MemoryCacheMBitmapByPosition	mMemoryCacheBmp		= new MemoryCacheMBitmapByPosition(100);

	protected void onCreate(Bundle paramBundle)
	{
		Log.i(TAG, "This is onCreate + Begin");
		super.onCreate(paramBundle);
		setContentView(R.layout.activity_main);

		// Non-UI thread...
		HandlerThread thread = new HandlerThread("SongListActivity", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mNonUILooper = thread.getLooper();
		mNonUIHandler = new NonUIHandler(mNonUILooper);

		mTrackListView = (ListView) this.findViewById(R.id.songlistview);
		mTrackListView.setTextFilterEnabled(false);
		mTrackListView.setFastScrollEnabled(true);
		mTrackListView.setOnScrollListener(this);

		if (mAdapter == null)
		{
			mAdapter = new TrackListAdapter(this, this);
		}

		mTrackListView.setAdapter(mAdapter);
		mProgressDialog = new ProgressDialog(this);
		Log.i(TAG, "This is onCreate + End");
	}

	/**
	 * Quit Activity and Clear all value
	 */
	protected void onDestroy()
	{
		Log.i(TAG, "This is onDestroy + Begin");
		super.onDestroy();

		if (mAdapter != null && mAdapter.mDecoder != null)
		{
			mAdapter.mDecoder.quit();
		}
		mTrackListView.setAdapter(null);
		mAdapter = null;

		if (null != mTrackListUiHandler)
		{
			mTrackListUiHandler.removeCallbacksAndMessages(null);
		}

		// Non-UI thread... begin
		if (mNonUIHandler != null)
		{
			mNonUIHandler.removeCallbacksAndMessages(null);
		}
		else
		{
			Log.e(TAG, "[onDestroy] mNonUIHandler is null!");
		}

		if (mNonUILooper != null)
		{
			mNonUILooper.quit();
		}
		else
		{
			Log.e(TAG, "[onDestroy] mNonUILooper is null!");
		}
		// Non-UI thread... end

		if (mTrackListData != null)
		{
			mTrackListData.clear();
		}
		mNonUIHandler = null;
		mTrackListData = null;
		mSongListData = null;
		
		if(null != mMemoryCacheBmp)
		{
			mMemoryCacheBmp.clear();
        }
		Log.i(TAG, "This is onDestroy + End");
	}

	/**
	 * flag this activity status whether is resume
	 * 
	 * @return
	 */
	protected boolean isActivityResumed()
	{
		return isActivityResumed;
	}

	protected void onResume()
	{
		Log.i(TAG, "This is onResume + Begin");
		super.onResume();
		isActivityResumed = true;
		if (mAdapter != null)
		{
			if (mAdapter.mDecoder != null)
			{
				mAdapter.mDecoder.resumeDecode();
			}
		}

		if (mNonUIHandler != null)
		{
			Log.d(TAG, "[onResume] sendEmptyMessage GET_SONG_LIST");
			mNonUIHandler.sendEmptyMessage(GET_SONG_LIST);
		}
		Log.i(TAG, "This is onResume + End");
	}

	@Override
	protected void onPause()
	{
		Log.i(TAG, "This is onPause + Begin");
		super.onPause();
		isActivityResumed = false;
		// Pause Decode
		if (mAdapter != null)
		{
			if (mAdapter.mDecoder != null)
			{
				mAdapter.mDecoder.clear();
				mAdapter.mDecoder.pauseDecode();
			}
		}
		super.onPause();
		Log.i(TAG, "This is onPause + End");
	}

	protected ArrayList<WrapperSong> getTrackList()
	{
		try
		{
			ArrayList<WrapperSong> songlist = this.getRecommendSong(mStartIdx, REQUEST_SONG_COUNT);
			Log.d(TAG, "Get Song Size:" + songlist.size());
			// Modify the next get start position
			mStartIdx += songlist.size();
			return songlist;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get Recommend Song
	 * 
	 * @param start
	 * @param limit
	 * @return ArrayList<WrapperSong>
	 * @throws Exception
	 */
	public ArrayList<WrapperSong> getRecommendSong(int start, int limit)
	{
		if (start < 0 || limit >= 100)
		{
			Log.e(TAG, "parameter error: start<=0 or lim>=100");
			return null;
		}

		ArrayList<WrapperSong> mRecommendSongArraylist = new ArrayList<WrapperSong>();
		try
		{
			DefaultHttpClient mDefaultHttpClient = new DefaultHttpClient();
			mDefaultHttpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 6000);
			mDefaultHttpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 6000);
			// The Test API
			String strUrl = "http://music.weibo.com/yueku/cooperate/htc/get_wpp_songs.php";
			Log.e(TAG, "HttpString:" + strUrl.toString());

			HttpGet localHttpGet = new HttpGet(strUrl);
			HttpResponse localHttpResponse = mDefaultHttpClient.execute(localHttpGet);
			StringBuilder mStringBuilder = new StringBuilder();
			if (localHttpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				BufferedReader mBufferedReader = new BufferedReader(new InputStreamReader(localHttpResponse
						.getEntity().getContent()));
				for (String str = mBufferedReader.readLine(); str != null; str = mBufferedReader.readLine())
				{
					mStringBuilder.append(str);
				}
			}

			JSONObject mJSONObject = new JSONObject(mStringBuilder.toString());
			int retCode = mJSONObject.getInt("retCode");
			if (retCode == 0)
			{
				Log.i(TAG, "Get Songlist Successful");

				int total = mJSONObject.getInt("total");
				Log.d(TAG, "total:" + total);

				JSONArray JsonArray = mJSONObject.getJSONArray("Content");
				if (JsonArray.length() > 0)
				{
					for (int i = 0; i < JsonArray.length(); i++)
					{
						JSONObject mSongJSONObject = JsonArray.optJSONObject(i);
						WrapperSong mWrapperSong = new WrapperSong();

						mWrapperSong.setTotalSong(total);

						int id = mSongJSONObject.getInt("id");
						mWrapperSong.setSongId(id);

						String name = mSongJSONObject.getString("name");
						mWrapperSong.setSongName(name);

						String artist = mSongJSONObject.getString("artist");
						mWrapperSong.setSingerName(artist);

						String description = mSongJSONObject.getString("description");
						mWrapperSong.setSongDescription(description);

						String image = mSongJSONObject.getString("image");
						mWrapperSong.setSongImage(image);

						String playUrl = mSongJSONObject.getString("playUrl");
						mWrapperSong.setSongPlayUrl(playUrl);

						String detail_url = mSongJSONObject.getString("detail_url");
						mWrapperSong.setSongDetailUrl(detail_url);

						mRecommendSongArraylist.add(mWrapperSong);
					}
				}
				else
				{
					Log.e(TAG, "Server Return : No match Data");
					return null;
				}
			}
			else if (retCode == -1)
			{
				Log.e(TAG, "Server Return : parameter error");
				return null;
			}
			else if (retCode == -2)
			{
				Log.e(TAG, "Server Return : user not login");
				return null;
			}
			else if (retCode == -3)
			{
				Log.e(TAG, "Server Return : Get Songlist failed");
				return null;
			}
		}
		catch (ClientProtocolException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mRecommendSongArraylist;
	}

	protected boolean haveMore()
	{
		return mStartIdx < THE_SONG_NUM;
	}

	private static final int	INVALIDATE_VIEWS			= 1;
	private static final int	NOTIFY_DATASET_CHANGED		= 2;
	private static final int	HIDE_LOADING_MESSAGE		= 3;
	private static final int	SHOW_LOADING_MESSAGE		= 4;
	private static final int	NOTIFY_SONG_LIST_UPDATED	= 5;

	protected Handler			mTrackListUiHandler			= new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case INVALIDATE_VIEWS:
					if (mTrackListView != null)
					{
						mTrackListView.invalidateViews();
					}
					isRefreshing = false;
					break;
				case NOTIFY_DATASET_CHANGED:
					if (mAdapter != null)
					{
						mAdapter.notifyDataSetChanged();
					}
					isRefreshing = false;
					break;
				case HIDE_LOADING_MESSAGE:
					mProgressDialog.hide();
					isRefreshing = false;
					break;
				case SHOW_LOADING_MESSAGE:
					mProgressDialog.show();
					isRefreshing = true;
					break;
				case NOTIFY_SONG_LIST_UPDATED:
					showData();
					break;
			}
		}
	};

	private void getMoreList()
	{
		if (!isRefreshing && mNonUIHandler != null)
		{
			isRefreshing = true;
			mNonUIHandler.sendEmptyMessage(GET_SONG_LIST);
		}
	}

	private void showData()
	{
		if (mSongListData != null && mSongListData.size() > 0)
		{
			if (mTrackListData != null)
			{
				mTrackListData.addAll(mSongListData);
				mSongListData.clear();
				if (mAdapter != null)
				{
					mAdapter.notifyDataSetChanged();
				}
				if (mIsEmptyList)
				{
					if (mProgressDialog != null)
					{
						mProgressDialog.dismiss();
					}
					mIsEmptyList = false;
				}
			}
		}
		else
		{
			if (mIsEmptyList)
			{
				if (mProgressDialog != null)
				{
					mProgressDialog.dismiss();
				}
				mIsEmptyList = false;
			}
			else if (mLoadingViewIsShown)
			{
				if (mTrackListView != null)
				{
					// after failed getting data, need set this flag to hide
					// loading view
					// at the end of the list view
					mPrepareGettingMore = false;
					mTrackListView.invalidateViews();
				}
				mLoadingViewIsShown = false;
				Log.d(TAG, "[showData] get no data!!!");
			}
		}
		isRefreshing = false;
	}

	private static final int	GET_SONG_LIST	= 2;
	private Looper				mNonUILooper;
	private NonUIHandler		mNonUIHandler;

	class NonUIHandler extends Handler
	{
		public NonUIHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case GET_SONG_LIST:
					try
					{
						if (mTrackListData == null || mTrackListData.size() == 0)
						{
							Log.d(TAG, "Show loading message");
							mTrackListUiHandler.sendEmptyMessage(SHOW_LOADING_MESSAGE);
							mIsEmptyList = true;
						}
						if (!haveMore())
						{
							if (mIsEmptyList)
							{
								mTrackListUiHandler.sendEmptyMessage(HIDE_LOADING_MESSAGE);
								mIsEmptyList = false;
							}
							return;
						}
						mSongListData = getTrackList();
						mTrackListUiHandler.sendEmptyMessage(NOTIFY_SONG_LIST_UPDATED);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					break;
				default:
					break;
			}
		}
	};

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		shouldRefresh = false;

		if (firstVisibleItem + visibleItemCount == totalItemCount)
		{
			if (haveMore())
			{
				if (!mPrepareGettingMore)
				{
					mPrepareGettingMore = true;
				}
				shouldRefresh = true;
			}
		}

		int start = firstVisibleItem;
		int end = firstVisibleItem + visibleItemCount;
		if (mAdapter != null && mAdapter.mDecoder != null)
		{
			mAdapter.mDecoder.setVisibleRange(start, end);
		}
        
        if (mMemoryCacheBmp != null) 
        {
        	mMemoryCacheBmp.setPosition((start + end) / 2);
        }
	}

	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		if (shouldRefresh && scrollState == OnScrollListener.SCROLL_STATE_IDLE)
		{
			getMoreList();
		}

		switch (scrollState)
		{
			case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
				if (mAdapter != null && mAdapter.mDecoder != null && isActivityResumed())
				{
					mAdapter.mDecoder.resumeDecode();
				}
				break;
			case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				if (mAdapter != null && mAdapter.mDecoder != null && isActivityResumed())
				{
					mAdapter.mDecoder.resumeDecode();
				}
				break;
			case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
				if (mAdapter != null && mAdapter.mDecoder != null)
				{
					mAdapter.mDecoder.pauseDecode();
				}
				break;
		}
	}

	class TrackListAdapter extends BaseAdapter implements AsyncImageDecoder.IImageDecodeListener
	{
		private SongListActivity	mParentActivity		= null;
		private LayoutInflater		mInflater			= null;
		private View				mLoadingView		= null;
		private Bitmap				mDefaultBmp			= null;
		private AsyncImageDecoder	mDecoder			= null;
		private static final String	FLAG_LOADING_ITEM	= "loadingItem";

		public TrackListAdapter(Context context, SongListActivity activity)
		{
			mParentActivity = activity;
			mInflater = LayoutInflater.from(context);
			mDefaultBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
			mDecoder = new AsyncImageDecoder(context, true, this, 120, 120, true);
		}

		@Override
		public int getCount()
		{
			if (mTrackListData != null)
			{
				int size = mTrackListData.size();
				if (haveMore() && size != 0 && mPrepareGettingMore)
				{
					size++;
				}
				return size;
			}
			return 0;
		}

		@Override
		public Object getItem(int position)
		{
			if (mTrackListData != null && mTrackListData.size() > position)
			{
				return mTrackListData.get(position);
			}
			return null;
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public boolean isEnabled(int position)
		{
			return (mTrackListData != null && mTrackListData.size() > position);
		}

		class ViewHolder
		{
			public ImageView	listImageView	= null;
			public TextView		listTextView1	= null;
			public TextView		listTextView2	= null;
			public TextView		listTextView3	= null;
		}

		public View getView(int pos, View convertView, ViewGroup parent)
		{
			Log.e(TAG, "[getView] Begin + This is position:" + pos);
			final SongListActivity tempActivity = mParentActivity;
			if (tempActivity == null)
			{
				return convertView;
			}
			if (mTrackListData == null)
			{
				return convertView;
			}

			WrapperSong curSong = null;
			if (mTrackListData.size() > pos)
			{
				curSong = mTrackListData.get(pos);
			}
			else
			{
				if (mLoadingView == null)
				{
					mLoadingView = mInflater.inflate(R.layout.loading_item, null);
					mLoadingView.setTag(FLAG_LOADING_ITEM);
				}
				mPrepareGettingMore = false;
				mLoadingViewIsShown = true;
				return mLoadingView;
			}
			return getViewWithPhoto(convertView, curSong, parent, pos);
		}

		// get list view with four text lines
		View getViewWithPhoto(View convertView, WrapperSong curSong, ViewGroup parent, int pos)
		{
			if (curSong == null)
			{
				Log.e(TAG, "[getViewWithDescription] curSong cannot be null!");
				return convertView;
			}

			ViewHolder holder;
			if (convertView == null || !convertView.getTag().getClass().equals(ViewHolder.class))
			{
				Log.i(TAG, "convertView == null,Then inflate and new holder");
				convertView = mInflater.inflate(R.layout.list_item, parent, false);
				holder = new ViewHolder();
				holder.listImageView = (ImageView) convertView.findViewById(R.id.ImageView);
				holder.listTextView1 = (TextView) convertView.findViewById(R.id.TextView01);
				holder.listTextView2 = (TextView) convertView.findViewById(R.id.TextView02);
				holder.listTextView3 = (TextView) convertView.findViewById(R.id.TextView03);
				convertView.setTag(holder);
			}
			else
			{
				Log.d(TAG, "convertView != null,Then get Holder");
				holder = (ViewHolder) convertView.getTag();
			}

			Log.i(TAG, "convertView != null,Then SetValue");

			if (holder != null && holder.listImageView != null)
			{
				holder.listImageView.setTag(new Integer(pos));

				String name = curSong.getSongName();
				holder.listTextView1.setText("[name]:" + name);
				String artist = curSong.getSingerName();
				holder.listTextView2.setText("[artist]:" + artist);
				String desc = curSong.getSongDescription();
				holder.listTextView3.setText("[description]:" + desc);
			}

			/*
			 * TODO:If need add cache , you can do something before add to
			 * decode The bitmap and decodePath will get from cache
			 */
			String songImageURL = curSong.getSongImage();
			MBitmap item = mMemoryCacheBmp.pull(pos);
			Bitmap bitmap = (item == null) ? null : item.getBitmap();
			String decodePath = (item == null) ? null : item.getPath();
			if (bitmap == null || bitmap.isRecycled()
					|| (bitmap != null && !TextUtils.equals(decodePath, songImageURL)))
			{
				Log.d(TAG, "bitmap is null.then add to decode queue");
				if (mDecoder != null)
				{
					Log.d(TAG, "pos:" + pos + ",songImageURL:" + songImageURL);
					mDecoder.add(true, pos, songImageURL, pos, songImageURL);
				}
				else
				{
					Log.e(TAG, "[getViewWithDescription] mDecoder cannot be null!");
				}
				if (holder != null && holder.listImageView != null)
				{
					Log.e(TAG, "[getViewWithPhoto] Set Default Album Icon");
					holder.listImageView.setImageBitmap(mDefaultBmp);
				}
			}
			else
			{
				if (holder != null && holder.listImageView != null)
				{
					Log.e(TAG, "[getViewWithPhoto] Set Image From Cache");
					holder.listImageView.setImageBitmap(bitmap);
				}
			}
			Log.e(TAG, "[getView] end + This is position:" + pos);
			return convertView;
		}

		@Override
		public void onImageDecoded(int position, Bitmap bitmap, long timeStamp, Object... varargs)
		{
			Log.d(TAG, "[onImageDecoded] + [Begin]");
			if (bitmap == null)
			{
				Log.e(TAG, "Bitmap is null");
				return;
			}

			ListView listView = mParentActivity.mTrackListView;
			int childCount = listView.getChildCount();
			if (childCount <= 0)
			{
				Log.e(TAG, "No List Item Need Update");
				bitmap.recycle();
				return;
			}
			
			
			String path = null;
            if (varargs != null && varargs.length > 0 && varargs[0] != null && varargs[0] instanceof String) 
            {
            	path = (String)varargs[0];
            }
            MBitmap bmp = new MBitmap(bitmap, path);
            boolean addToCache = mMemoryCacheBmp.push(position, bmp);
            if (addToCache) 
            {
            	for (int i = 0; i < childCount; i++) 
            	{
            		final View ll = listView.getChildAt(i);
            		
            		if (ll != null) 
            		{
            			final ImageView localImageView = (ImageView) ll.findViewById(R.id.ImageView);
    					if (localImageView == null)
    					{
    						Log.e(TAG, "[onImageDecoded] imageView is null");
    						return;
    					}
    					Integer integer = (Integer) localImageView.getTag();
    					if (integer != null && integer.intValue() == position)
    					{
    						Log.e(TAG, "[onImageDecoded] Update the imageView with online decode bitmap");
    						localImageView.setImageBitmap(bitmap);
    					}
            		}
            	}                            	
            }
			Log.d(TAG, "[onImageDecoded] + [End]");
		}
	}
}
