package com.hoo.AsyncImageDecode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * Async Image Decoder
 * 
 * @author Kesen
 * @since 2012-09-12
 */
public class AsyncImageDecoder
{
	//The DecodeTask List
	private List<DecodeTask> mList = new ArrayList<DecodeTask>();

	//Decode Listener
	private IImageDecodeListener mListener = null;

	//The Decode UI Thread
	private HandlerThread mHandlerThread = null;

	//The Do Decode Thread
	private NonUiHandler mNonUiHandler = null;

	//Define TAG
	private static final String TAG = "[AsyncImageDecodeDemo]";

	//Define Thread Name
	private static final String THREAD_NAME = "AsyncImageDecoderThread";

	//Message Code
	private static final int DECODE = 1;
	private static final int DECODED = 2;
	private static final int QUIT = 3;

	//BitmapFactoryOptions
	private BitmapFactory.Options mBitmapOptions = null;

	//Async Decode Object
	private Object mSyncObject = new Object();

	//Decode Context
	private Context mContext = null;

	//Record Current Decode Status Flag
	private boolean mPause = false;
	
	//Flag: Whether Online Task
	private boolean mIsOnline = false;

	//Target Bitmap Size
	private int mWidth = 0;
	private int mHeight = 0;

	//Whether need scale bitmap
	private boolean mScale = true;

	//Decode Options
	private boolean mFilter = false;

	//Whewher need update the file modify date
	private boolean mTouchFile = false;

	//Flag:Decode failed
	private boolean mReportError = false;

	//Local Album Art Uri
	private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

	//Decode start and end position
	private int mStart = 0;
	private int mEnd = 0;

	//
	private boolean mDecodeVisibleFirst = false;

	//Init Merge Image Value
	private boolean mMergeAfterDecoded = false;
	private int mMergeImageId = -1;
	private Bitmap mMergeImage = null;
	private int mMergeWidth = 0;
	private int mMergeHeight = 0;
	private int mMergeOffsetX = 0;
	private int mMergeOffsetY = 0;

	//Whether need do recycle while onPause
	private boolean mRecycleOnPause = true;

	/**
	 * Constructor of AsyncImageDecoder.
	 * 
	 * @param context
	 *            application context.
	 * @param listener
	 *            callbacks while image decoded.
	 */
	public AsyncImageDecoder(Context context, boolean isOnline, IImageDecodeListener listener)
	{
		mContext = context;
		mIsOnline = isOnline;
		mFilter = false;
		mScale = false;
		mTouchFile = false;
		mListener = listener;
		init();
	}

	/**
	 * Constructor of AsyncImageDecoder.
	 * 
	 * @param context
	 *            application context.
	 * @param listener
	 *            callbacks while image decoded.
	 * @param width
	 *            desired bitmap width.
	 * @param height
	 *            desired bitmap height.
	 */
	public AsyncImageDecoder(Context context, boolean isOnline, IImageDecodeListener listener, int width, int height)
	{
		mContext = context;
		mIsOnline = isOnline;
		mWidth = width;
		mHeight = height;
		mFilter = false;
		mTouchFile = false;
		if (width == 0 || height == 0)
		{
			mScale = false;
		}
		mListener = listener;
		init();
	}

	/**
	 * Constructor of AsyncImageDecoder.
	 * 
	 * @param context
	 *            application context.
	 * @param listener
	 *            callbacks while image decoded.
	 * @param width
	 *            desired bitmap width.
	 * @param height
	 *            desired bitmap height.
	 * @param filter
	 *            boolean used in {@link createScaledBitmap}.
	 */
	public AsyncImageDecoder(Context context, boolean isOnline, IImageDecodeListener listener, int width, int height, boolean filter)
	{
		mContext = context;
		mIsOnline = isOnline;
		mWidth = width;
		mHeight = height;
		mFilter = filter;
		mTouchFile = false;
		if (width == 0 || height == 0)
		{
			mScale = false;
		}
		mListener = listener;
		init();
	}

	/**
	 * Constructor of AsyncImageDecoder.
	 * 
	 * @param context
	 *            application context.
	 * @param listener
	 *            callbacks while image decoded.
	 * @param width
	 *            desired bitmap width.
	 * @param height
	 *            desired bitmap height.
	 * @param filter
	 *            boolean used in {@link createScaledBitmap}.
	 * @param touchFile
	 *            set file's last modified time after bitmaps decode succeed.
	 */
	public AsyncImageDecoder(Context context, boolean isOnline, IImageDecodeListener listener, int width, int height,	boolean filter, boolean touchFile)
	{
		mContext = context;
		mWidth = width;
		mHeight = height;
		mFilter = filter;
		mTouchFile = touchFile;
		if (width == 0 || height == 0)
		{
			mScale = false;
		}
		mListener = listener;
		init();
	}

	/**
	 * Init Decode Options and HandlerThread
	 */
	private void init()
	{
		mBitmapOptions = new BitmapFactory.Options();
		mBitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
		mBitmapOptions.inDither = false;

		mHandlerThread = new HandlerThread(THREAD_NAME);
		mHandlerThread.setPriority(Thread.NORM_PRIORITY);
		mHandlerThread.start();
		mNonUiHandler = new NonUiHandler(mHandlerThread.getLooper());
	}
	
	public void setReportError(boolean reportError)
	{
		mReportError = reportError;
	}

	public void setRecycleOnPause(boolean recycleOnPause)
	{
		mRecycleOnPause = recycleOnPause;
	}

	/**
	 * Sets image resource id used to merge the decode image, and related information.
	 * 
	 * @param resourceId
	 * @param width
	 * @param height
	 * @param offsetX
	 * @param offsetY
	 */
	public void setMergeImageResource(int resourceId, int width, int height, int offsetX, int offsetY)
	{
		if (width <= 0 || height <= 0 || offsetX < 0 || offsetY < 0)
		{
			mMergeAfterDecoded = false;
			return;
		}
		mMergeImageId = resourceId;
		mMergeWidth = width;
		mMergeHeight = height;
		mMergeOffsetX = offsetX;
		mMergeOffsetY = offsetY;
		mMergeAfterDecoded = true;
	}

	/**
	 * Sets visible range when it changed. Images in visible range will decode first.
	 * 
	 * @param start
	 * @param end
	 */
	public void setVisibleRange(int start, int end)
	{
		if (start < 0 || end < 0 || end < start)
		{
			mDecodeVisibleFirst = false;
		}

		synchronized (mSyncObject)
		{
			mStart = start;
			mEnd = end;
			mDecodeVisibleFirst = true;
			Log.d(TAG, "[setVisibleRange] mStart: " + mStart + ", mEnd: " + mEnd);
		}
	}

	private void notifyError(int position)
	{
		if (mReportError)
		{
			if (mListener != null && !mPause)
			{
				mListener.onImageDecoded(position, null, 0, 0);
			}
		}
	}

	/**
	 * Adds item into decode list.
	 * 
	 * @param key
	 *            the index will be return in {@link onImageDecoded}
	 * @param path
	 *            the path of the image.
	 * @param position
	 *            used in getting visible items
	 * @param varargs
	 *            the argument will be return in {@link onImageDecoded}
	 */
	public void add(int key, String path, int position, Object... varargs)
	{
		if (path == null || path.length() <= 0)
		{
			notifyError(position);
			return;
		}

		DecodeTask task = new DecodeTask(key, path, position, varargs);
		this.add(task);

		if (mNonUiHandler != null)
		{
			if (mNonUiHandler.hasMessages(DECODE))
			{
				Log.e(TAG, "Message DECODE is exist, return");
				return;
			}
			mNonUiHandler.sendEmptyMessageDelayed(DECODE, 50);
		}
	}

	/**
	 * Adds item into decode list.
	 * 
	 * @param bOnlineDecode
	 *            whether decoding online bitmap
	 * @param key
	 *            the index will be return in {@link onImageDecoded}
	 * @param path
	 *            the path of the image.
	 * @param position
	 *            used in getting visible items
	 * @param varargs
	 *            the argument will be return in {@link onImageDecoded}
	 */
	public void add(boolean bOnlineDecode, int key, String path, int position, Object... varargs)
	{
		if (bOnlineDecode)
		{
			Log.d(TAG, "isOnline Decode Action");
			if (path == null || path.length() <= 0)
			{
				notifyError(position);
				return;
			}

			DecodeTask task = new DecodeTask(key, path, position, varargs);
			this.add(task);
			Log.d(TAG, "[add] task = "+task.toString());

			if (mNonUiHandler != null)
			{
				if (mNonUiHandler.hasMessages(DECODE))
				{
					Log.e(TAG, "Message DECODE is exist, return");
					return;
				}
				this.setRecycleOnPause(false);
				Log.e(TAG, "Sent DECODE Message");
				mNonUiHandler.sendEmptyMessageDelayed(DECODE, 50);
			}
		}
		else
		{
			this.add(key, path, position, varargs);
		}
	}

	/**
	 * Adds item into decode list.
	 * 
	 * @param key
	 *            the index will be return in {@link onImageDecoded}
	 * @param path
	 *            the path of the image.
	 * @param position
	 *            used in getting visible items
	 * @param notScale
	 *            force decode with original size if not exceed MAX size
	 * @param varargs
	 *            the argument will be return in {@link onImageDecoded}
	 */
	public void add(int key, String path, int position, boolean notScale, Object... varargs)
	{
		if (path == null || path.length() <= 0)
		{
			notifyError(position);
			return;
		}

		DecodeTask task = new DecodeTask(key, path, position, notScale, varargs);
		this.add(task);

		if (mNonUiHandler != null)
		{
			if (mNonUiHandler.hasMessages(DECODE))
			{
				Log.e(TAG, "Message DECODE is exist, return");
				return;
			}
			mNonUiHandler.sendEmptyMessageDelayed(DECODE, 50);
		}
	}

	/**
	 * Adds item into decode list.
	 * 
	 * @param key
	 *            the index will be return in {@link onImageDecoded}
	 * @param albumId
	 *            the album id of the image
	 * @param position
	 *            used in getting visible items
	 * @param varargs
	 *            the argument will be return in {@link onImageDecoded}
	 */
	public void add(int key, int albumId, int position, Object... varargs)
	{
		if (albumId < 0)
		{
			notifyError(position);
			return;
		}

		DecodeTask task = new DecodeTask(key, albumId, position, varargs);
		this.add(task);

		if (mNonUiHandler != null)
		{
			if (mNonUiHandler.hasMessages(DECODE))
			{
				Log.e(TAG, "Message DECODE is exist, return");
				return;
			}
			mNonUiHandler.sendEmptyMessageDelayed(DECODE, 50);
		}
	}

	/**
	 * Adds item into decode list.
	 * 
	 * @param key
	 *            the index will be return in {@link onImageDecoded}
	 * @param afd
	 *            The file descriptor containing the bitmap data to decode
	 * @param position
	 *            used in getting visible items
	 * @param varargs
	 *            the argument will be return in {@link onImageDecoded}
	 */
	public void add(int key, AssetFileDescriptor afd, int position, Object... varargs)
	{
		if (afd == null)
		{
			notifyError(position);
			return;
		}

		DecodeTask task = new DecodeTask(key, afd, position, varargs);
		this.add(task);

		if (mNonUiHandler != null)
		{
			if (mNonUiHandler.hasMessages(DECODE))
			{
				Log.e(TAG, "Message DECODE is exist, return");
				return;
			}
			mNonUiHandler.sendEmptyMessageDelayed(DECODE, 50);
		}
	}

	/**
	 * Both path, and album id will be used to decode bitmaps. Path is the first
	 * priority, and albumId is the second.
	 * 
	 * @param key
	 *            the index will be return in {@link onImageDecoded}
	 * @param albumId
	 *            the album id of the image
	 * @param path
	 *            the path of the image
	 * @param position
	 *            used in getting visible items
	 * @param varargs
	 *            the argument will be return in {@link onImageDecoded}
	 */
	public void add(int key, String path, int albumId, int position, Object... varargs)
	{
		if (path == null && albumId < 0)
		{
			notifyError(position);
			return;
		}

		DecodeTask task = new DecodeTask(key, path, albumId, position, varargs);
		this.add(task);

		if (mNonUiHandler != null)
		{
			if (mNonUiHandler.hasMessages(DECODE))
			{
				Log.e(TAG, "Message DECODE is exist, return");
				return;
			}
			mNonUiHandler.sendEmptyMessageDelayed(DECODE, 50);
		}
	}

	/**
	 * Adds the decode task into the task list. If the task is existed, update
	 * the time stamp of the task.
	 * 
	 * @param task
	 */
	private void add(DecodeTask task)
	{
		Log.d(TAG, "[add] DecodeTask task");
		synchronized (mSyncObject)
		{
			int indexOfTask = mList.indexOf(task);
			if (indexOfTask > -1)
			{
				Log.d(TAG, "[add] set list");
				mList.set(indexOfTask, task);
			}
			else
			{
				Log.d(TAG, "[add] add list");
				mList.add(task);
			}
		}
	}

	private void add(int pos, DecodeTask task)
	{
		synchronized (mSyncObject)
		{
			if (mList == null)
			{
				return;
			}
			mList.add(pos, task);
		}
	}

	/**
	 * Check task whether exist
	 * @param task
	 * @return
	 */
	private boolean isTaskExists(DecodeTask task)
	{
		if (task == null)
		{
			Log.e(TAG, "[isTaskExists] task is null");
			return false;
		}
			
		synchronized (mSyncObject)
		{
			DecodeTask temp = null;
			for (int i = 0; i < mList.size(); i++)
			{
				temp = mList.get(i);
				if (temp != null && temp.equals(task))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Uninitialize decoder.
	 */
	public void quit()
	{
		mListener = null;
		clear();
		if (mNonUiHandler != null)
		{
			mNonUiHandler.removeCallbacksAndMessages(null);
			// to notify handler thread if it is in wait state.
			resumeDecode();
			if (mHandlerThread != null)
			{
				mHandlerThread.getLooper().quit();
				mHandlerThread = null;
				mNonUiHandler = null;
			}
		}
		if (mUiHandler != null)
		{
			mUiHandler.removeCallbacksAndMessages(null);
		}
	}

	/**
	 * Pauses decoder.
	 */
	public void pauseDecode()
	{
		if (mPause == true)
		{
			Log.d(TAG, "mPause is true, return");
			return;
		}
		Log.d(TAG, "do PauseDecode");
		mPause = true;
	}

	/**
	 * Resume decoder.
	 */
	public void resumeDecode()
	{
		if (mPause == false)
		{
			return;
		}
		Log.d(TAG, "do resumeDecode");
		mPause = false;
		synchronized (mSyncObject)
		{
			mSyncObject.notify();
		}
	}

	/**
	 * Clear all decode task in the queue
	 */
	public void clear()
	{
		synchronized (mSyncObject)
		{
			mList.clear();
		}
	}

	/**
	 * Helper function to get album art path by album Id
	 * 
	 * @param albumId
	 *            the album id in database
	 * @return album art path
	 */
	private String getPathByAlbumId(int albumId)
	{
		if (albumId < 0)
		{
			Log.e(TAG, "[getPathByAlbumId]albumId is null");
			return null;
		}
			
		ContentResolver resolver = mContext.getContentResolver();
		if (resolver == null)
		{
			Log.e(TAG, "[getPathByAlbumId]ContentResolver is null");
			return null;
		}
			
		Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);
		if (uri == null)
		{
			Log.e(TAG, "[getPathByAlbumId]The AlbumArt Path is null");
			return null;
		}
			
		Cursor mCursor = resolver.query(uri, new String[] { "_data" }, null, null, null);
		int count = (mCursor != null) ? mCursor.getCount() : 0;
		//Incorrect case
		if (count != 1)
		{
			//Close Cursor
			if (mCursor != null)
			{
				mCursor.close();
			}
			//Query No Result
			if (count == 0)
			{
				Log.e(TAG, "[getPathByAlbumId]Query Result Count is 0");
				return null;
			}
			return null;
		}

		mCursor.moveToFirst();
		int i = mCursor.getColumnIndex("_data");
		String albumArtPath = (i >= 0 ? mCursor.getString(i) : null);
		mCursor.close();
		return albumArtPath;
	}

	private DecodeTask removeFirstVisibleTask()
	{
		Log.d(TAG, "[removeFirstVisibleTask] + Begin");
		if (mList.size() <= 0)
		{
			Log.e(TAG, "[removeFirstVisibleTask] DecodeTask List size <= 0;");
			return null;
		}
			
		int start = -1;
		int end = -1;

		if (!mDecodeVisibleFirst || mStart < 0 || mEnd < 0 || mEnd < mStart)
		{
			Log.e(TAG, "[removeFirstVisibleTask] Parameter: start or end is Wrong");
			return null;
		}
			
		start = mStart;
		end = mEnd;

		// find first item in visible range
		int firstVisible = -1;
		DecodeTask task = null;
		for (int i = 0; i < mList.size(); i++)
		{
			task = mList.get(i);
			if (task != null)
			{
				if (start <= task.mPosition && task.mPosition <= end)
				{
					firstVisible = i;
					break;
				}
			}
			task = null;
		}

		if (firstVisible >= 0)
		{
			Log.d(TAG, "[removeFirstVisibleTask] + End: firstVisible = " + firstVisible);
			return mList.remove(firstVisible);
		}
		else
		{
			Log.e(TAG, "[removeFirstVisibleTask] firstVisible position is wrong");
			return null;
		}
	}

	/**
	 * Decode By AlbumId
	 * @param albumId : The Id that exits at DB
	 * @param notScale : The Flag that whether scale
	 * @return
	 */
	private Bitmap decodeByAlbumId(int albumId, boolean notScale)
	{
		ContentResolver res = mContext.getContentResolver();
		Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);
		if (uri != null)
		{
			ParcelFileDescriptor fd = null;
			try
			{
				fd = res.openFileDescriptor(uri, "r");
				int sampleSize = 1;

				if (mScale)
				{
					mBitmapOptions.inSampleSize = 1;
					mBitmapOptions.inJustDecodeBounds = true;
					BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, mBitmapOptions);
					int nextWidth = mBitmapOptions.outWidth >> 1;
					int nextHeight = mBitmapOptions.outHeight >> 1;
					while (nextWidth > mWidth && nextHeight > mHeight)
					{
						sampleSize <<= 1;
						nextWidth >>= 1;
						nextHeight >>= 1;
					}
				}

				mBitmapOptions.inSampleSize = sampleSize;
				mBitmapOptions.inJustDecodeBounds = false;
				Bitmap bmp = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, mBitmapOptions);

				if (bmp != null && mScale && !notScale)
				{
					if (mBitmapOptions.outWidth != mWidth || mBitmapOptions.outHeight != mHeight)
					{
						Bitmap tmp = Bitmap.createScaledBitmap(bmp, mWidth, mHeight, mFilter);
						bmp.recycle();
						bmp = tmp;
					}
				}
				return bmp;
			}
			catch (FileNotFoundException e)
			{
				Log.d(TAG, "decodeByAlbumId failed albumId = " + albumId + ", " + e);
			}
			catch (Exception e)
			{
				Log.d(TAG, "decodeByAlbumId failed albumId = " + albumId + ", " + e);
			}
			finally
			{
				try
				{
					if (fd != null)
					{
						fd.close();
					}
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
		}
		return null;
	}

	/**
	 * Decode By File Descriptor
	 * @param assetFileDes
	 * @return
	 */
	private Bitmap decodeByFileDescriptor(AssetFileDescriptor assetFileDes)
	{
		if (assetFileDes == null)
		{
			Log.e(TAG, "assetFileDes is NULL");
			return null;
		}
		try
		{
			int sampleSize = 1;
			if (mScale)
			{
				mBitmapOptions.inSampleSize = 1;
				mBitmapOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeFileDescriptor(assetFileDes.getFileDescriptor(), null, mBitmapOptions);
				int nextWidth = mBitmapOptions.outWidth >> 1;
				int nextHeight = mBitmapOptions.outHeight >> 1;
				while (nextWidth > mWidth && nextHeight > mHeight)
				{
					sampleSize <<= 1;
					nextWidth >>= 1;
					nextHeight >>= 1;
				}
			}

			mBitmapOptions.inSampleSize = sampleSize;
			mBitmapOptions.inJustDecodeBounds = false;
			Bitmap bmp = BitmapFactory.decodeFileDescriptor(assetFileDes.getFileDescriptor(), null, mBitmapOptions);
			if (bmp != null && mScale)
			{
				if (mBitmapOptions.outWidth != mWidth || mBitmapOptions.outHeight != mHeight)
				{
					Bitmap tmp = Bitmap.createScaledBitmap(bmp, mWidth, mHeight, mFilter);
					bmp.recycle();
					bmp = tmp;
				}
			}
			return bmp;
		}
		catch (Exception ex)
		{
			Log.d(TAG, "decodeByFileDescriptor failed, " + ex);
		}
		finally
		{
			try
			{
				if (assetFileDes != null)
				{
					assetFileDes.close();
				}
			}
			catch (IOException e)
			{
				Log.e(TAG, "IO ex catched.");
			}
			catch (NullPointerException e)
			{
				Log.e(TAG, "NP ex inside AssetFileDescriptor catched.");
			}
		}
		return null;
	}

	/**
	 * Decode By Image File Path
	 * @param path
	 * @return
	 */
	private Bitmap decodeByPath(String path)
	{
		if (path == null || path.length() == 0)
		{
			Log.e(TAG, "File Path is NULL");
			return null;
		}
		try
		{
			int sampleSize = 1;
			if (mScale)
			{
				mBitmapOptions.inSampleSize = 1;
				mBitmapOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, mBitmapOptions);
				int nextWidth = mBitmapOptions.outWidth >> 1;
				int nextHeight = mBitmapOptions.outHeight >> 1;
				while (nextWidth > mWidth && nextHeight > mHeight)
				{
					sampleSize <<= 1;
					nextWidth >>= 1;
					nextHeight >>= 1;
				}
			}

			mBitmapOptions.inSampleSize = sampleSize;
			mBitmapOptions.inJustDecodeBounds = false;
			Bitmap bmp = BitmapFactory.decodeFile(path, mBitmapOptions);

			if (bmp != null && mScale)
			{
				if (mBitmapOptions.outWidth != mWidth || mBitmapOptions.outHeight != mHeight)
				{
					Bitmap tmp = Bitmap.createScaledBitmap(bmp, mWidth, mHeight, mFilter);
					bmp.recycle();
					bmp = tmp;
				}
			}
			return bmp;
		}
		catch (Exception ex)
		{
			Log.d(TAG, "decodeBypath failed, " + ex);
		}
		return null;
	}

	/**
	 * Decode By Online URL
	 * @param URLPath
	 * @return
	 */
	private Bitmap decodeByURL(String UriPath)
	{
		Log.d(TAG, "[decodeByURL] + Begin");
		URL Uri = null;
		URLConnection UriConn = null;
		if (UriPath == null || UriPath.length() == 0)
		{
			Log.e(TAG, "UriPath is NULL");
			return null;
		}
		try
		{
			Uri = new URL(UriPath);
			UriConn = Uri.openConnection();
			UriConn.connect();
			InputStream is = UriConn.getInputStream();
			
	    	Bitmap bmp = BitmapFactory.decodeStream(is);
	    	is.close();
	    	if (bmp != null && mScale) 
	    	{
                if (bmp.getWidth() != mWidth || bmp.getHeight() != mHeight) 
                {
                	Log.e(TAG, "[decodeByURL] createScaledBitmap");
                    Bitmap tmp = Bitmap.createScaledBitmap(bmp, mWidth, mHeight, mFilter);
                    bmp.recycle();
                    bmp = tmp;
                }
            }
	    	Log.d(TAG, "[decodeByURL] Scaled : width="+bmp.getWidth()+",height="+bmp.getHeight());
			Log.d(TAG, "[decodeByURL] + End, return bmp:" + bmp);
			return bmp;
		}
		catch (MalformedURLException e)
		{
			Log.d(TAG, "decodeByURL failed UriPath = " + UriPath + ", " + e);
			e.printStackTrace();
		}
		catch (IOException e)
		{
			Log.d(TAG, "decodeByURL failed UriPath = " + UriPath + ", " + e);
			e.printStackTrace();
		}
		Log.d(TAG, "[decodeByURL] + End, return null");
		return null;
	}
	
	private final Handler mUiHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case DECODED:
				{ 	// In order to make call back on UI thread
					DecodeTask task = (DecodeTask) msg.obj;
					if (task != null && (null != task.mBitmap || mReportError))
					{
						if (mRecycleOnPause)
						{
							if (mListener != null && !mPause)
							{
								Log.d(TAG, "[handleMessage], is Pause ,task: " + task.toString());
								mListener.onImageDecoded(task.mKey, task.mBitmap, task.mTimeStamp, task.mVarargs);
							}
							else
							{
								Log.e(TAG, "[handleMessage]Decoder paused, recycle current bitmap: " + task.toString());
								if (null != task.mBitmap)
								{
									task.mBitmap.recycle();
									task.mBitmap = null;
								}
								add(0, task);
							}
						}
						else
						{
							if (mListener != null)
							{
								Log.d(TAG, "[handleMessage] not Pause. task: " + task.toString());
								mListener.onImageDecoded(task.mKey, task.mBitmap, task.mTimeStamp, task.mVarargs);
							}
							else
							{
								Log.e(TAG, "[handleMessage]onImageDecoded mListener == null, recycle bitmap");
								if (null != task.mBitmap)
								{
									task.mBitmap.recycle();
									task.mBitmap = null;
								}
							}
						}
					}
				}
				break;

				case QUIT:
				{
					Log.d(TAG, "Quit The Decoder");
					quit();
				}
				break;

				default:
				{
					Log.e(TAG, "Something wrong in mUiHandler.handleMessage()");
				}
				break;
			}
		}
	};

	/**
	 * The NonUiHandler to do decode action
	 * @author hoo
	 *
	 */
	private final class NonUiHandler extends Handler
	{
		public NonUiHandler(Looper looper)
		{
			super(looper);
			Log.d(TAG, "[NonUiHandler] Constructor");
		}

		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case DECODE:
					Log.d(TAG, "[handleMessage] -- DECODE");
					doDecode();
					break;
				default:
					Log.d(TAG, "Something wrong in handleMessage()");
					break;
			}
		}
			
		private void doDecode()
		{
			Log.d(TAG, "[doDecode] + Begin");
			DecodeTask task = null;
			synchronized (mSyncObject)
			{
				Log.d(TAG, "[doDecode] + Begin");
				task = removeFirstVisibleTask();
				if (task == null)
				{
					// remove the first decode task in the list
					if (mList.size() > 0)
					{
						task = mList.remove(0);
					}
				}
				Log.d(TAG, "[doDecode] first Visible task = "+ task.toString());
			}

			Bitmap bitmap = null;
			if (task != null)
			{
				if (mIsOnline)
				{
					Log.d(TAG, "[doDecode] is Online Task");
					if (task.mPath != null)
					{
						bitmap = decodeByURL(task.mPath);
					}
				}
				else
				{
					Log.d(TAG, "[doDecode] is Local Task");
					if (task.mPath != null)
					{
						bitmap = decodeByPath(task.mPath);
						// try to decode by album id
						if (bitmap == null && task.mAlbumId > -1)
						{
							bitmap = decodeByAlbumId(task.mAlbumId, task.mNotScale);
						}
					}
					else if (task.mAlbumId > -1)
					{
						bitmap = decodeByAlbumId(task.mAlbumId, task.mNotScale);
					}
					else if (task.mDescriptor != null)
					{
						bitmap = decodeByFileDescriptor(task.mDescriptor);
					}
				}
				
				if (bitmap != null)
				{
					// touch file to update last modified time in sdCard
					if (mTouchFile == true && task != null && task.mPath != null && task.mPath.length() > 0)
					{
						File file = new File(task.mPath);
						if (file.exists() && file.isFile())
						{
							file.setLastModified(System.currentTimeMillis());
						}
						file = null;
					}

					// merge decoded image
					if (mMergeAfterDecoded)
					{
						if (mMergeImage == null)
						{
							mMergeImage = BitmapFactory.decodeResource(mContext.getResources(), mMergeImageId);
						}
						if (mMergeImage != null)
						{
							Bitmap cleanImage = Bitmap.createBitmap(mMergeWidth, mMergeHeight,Bitmap.Config.ARGB_8888);
							Canvas canvas = new Canvas();
							canvas.setBitmap(cleanImage);
							canvas.drawBitmap(mMergeImage, 0, 0, null);
							canvas.drawBitmap(bitmap, mMergeOffsetX, mMergeOffsetY, null);
							bitmap.recycle();
							bitmap = cleanImage;
						}
					}

					task.mBitmap = bitmap;
					if (mListener != null && mUiHandler != null)
					{
						Log.d(TAG, "[doDecode] send DECODED message");
						Message message = obtainMessage(DECODED, 0, 0, null);
						message.obj = task;
						mUiHandler.sendMessageDelayed(message, 0);
					}
					else
					{
						if (task.mBitmap != null && !task.mBitmap.isRecycled())
						{
							task.mBitmap.recycle();
							task.mBitmap = null;
						}
					}
				}
				else if (mReportError)
				{
					task.mBitmap = null;
					if (mListener != null && mUiHandler != null)
					{
						Message message = obtainMessage(DECODED, 0, 0, null);
						message.obj = task;
						mUiHandler.sendMessageDelayed(message, 0);
					}
				}
			}

			synchronized (mSyncObject)
			{
				while (mPause)
				{
					try
					{
						mSyncObject.wait();
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
						if (mUiHandler != null)
						{
							if (mUiHandler.hasMessages(QUIT))
							{
								mUiHandler.removeMessages(QUIT);
							}
							mUiHandler.sendEmptyMessageDelayed(QUIT, 100);
						}
					}
				}
			}

			if (mList.size() > 0)
			{
				if (!hasMessages(DECODE))
				{
					sendEmptyMessageDelayed(DECODE, 0);
				}
			}
			else
			{
				removeMessages(DECODE);
			}
			Log.d(TAG, "[doDecode] + End");
		}
	}
	
	/**
	 * The Decode Listener
	 * @author hoo
	 *
	 */
	public interface IImageDecodeListener
	{
		/**
		 * Callbacks while an image decoded
		 * 
		 * @param position
		 *            the first parameter in add function
		 * @param bitmap
		 *            the decoded bitmap
		 * @param timeStamp
		 *            the request time of this bitmap
		 * @param arg0
		 *            the argument of this decoded image
		 */
		public void onImageDecoded(int position, Bitmap bitmap, long timeStamp, Object... varargs);
	}
	
	/**
	 * The Decode Task Class
	 * @author hoo
	 *
	 */
	private class DecodeTask
	{
		private int mKey = 0;
		private int mPosition = 0;
		private int mAlbumId = -1;
		private long mTimeStamp = 0;
		private boolean mNotScale = false;
		private String mPath = null;
		private Bitmap mBitmap = null;
		private Object[] mVarargs = null;
		private AssetFileDescriptor mDescriptor = null;
		
		public DecodeTask(int key, String path, int position, Object... arg0)
		{
			mKey = key;
			mPath = path;
			mTimeStamp = System.currentTimeMillis();
			mPosition = position;
			mVarargs = arg0;
		}

		public DecodeTask(int key, String path, int position, boolean notScale, Object... arg0)
		{
			mKey = key;
			mPath = path;
			mTimeStamp = System.currentTimeMillis();
			mPosition = position;
			mNotScale = notScale;
			mVarargs = arg0;
		}

		public DecodeTask(int key, int albumId, int position, Object... arg0)
		{
			mKey = key;
			mAlbumId = albumId;
			mTimeStamp = System.currentTimeMillis();
			mPosition = position;
			mVarargs = arg0;
		}

		public DecodeTask(int key, AssetFileDescriptor afd, int position, Object... arg0)
		{
			mKey = key;
			mDescriptor = afd;
			mTimeStamp = System.currentTimeMillis();
			mPosition = position;
			mVarargs = arg0;
		}
		
		public DecodeTask(int key, String path, int albumId, int position, Object... arg0)
		{
			mKey = key;
			mPath = path;
			mAlbumId = albumId;
			mTimeStamp = System.currentTimeMillis();
			mPosition = position;
			mVarargs = arg0;
		}

		public DecodeTask(DecodeTask task)
		{
			if (task == null)
			{
				return;
			}
			mKey = task.mKey;
			mPath = task.mPath;
			mAlbumId = task.mAlbumId;
			mDescriptor = task.mDescriptor;
			mTimeStamp = System.currentTimeMillis();
			mPosition = task.mPosition;
			mVarargs = task.mVarargs;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof DecodeTask))
			{
				return false;
			}
			DecodeTask task = (DecodeTask) o;
			return mKey == task.mKey;
		}

		@Override
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("mKey: ");
			builder.append(mKey);
			builder.append(", mRequestTime: ");
			builder.append(mTimeStamp);
			return builder.toString();
		}
	}
}
