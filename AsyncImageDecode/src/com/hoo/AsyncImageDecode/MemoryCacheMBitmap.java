package com.hoo.AsyncImageDecode;

import java.util.Iterator;
import java.util.Set;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Memory Cache: used to cache Music bitmap
 * @author hoo
 * @param <K>
 * @since 2012-09
 * @param <K>
 */
public class MemoryCacheMBitmap<K> extends MemoryCacheBase<K, MBitmap>
{
	protected int				mSize       = 0;
	protected int				mRefCount	= 0;
	private static final String	TAG			= "[AsyncImageDecodeDemo]";

	/**
	 * Constructor
	 * @param count : Cache Size
	 */
	public MemoryCacheMBitmap(int size)
	{
		mSize = size;
		mRefCount = 0;
	}

	@Override
	protected void releaseItemResource(MBitmap item)
	{
		if (item != null)
		{
			Bitmap bitmap = item.getBitmap();
			if (bitmap != null && !bitmap.isRecycled())
			{
				bitmap.recycle();
			}
		}
	}

	/**
	 * Get Cache Capacity
	 * @return
	 */
	public int getCapacity()
	{
		return mSize;
	}

	/**
	 * Check Current Cache whether overloading
	 */
	@Override
	protected boolean checkCapacity(MBitmap item)
	{
		return (mItems.size() >= mSize) ? false : true;
	}

	/**
	 * Clear Cache Pool
	 */
	@Override
	public void clear()
	{
		try
		{
			mLock.lock();
			Set<K> keys = mItems.keySet();
			Iterator<K> it = keys.iterator();
			while (it.hasNext())
			{
				MBitmap bmp = mItems.get(it.next());
				releaseItemResource(bmp);
			}
			super.clear();
		}
		finally
		{
			mLock.unlock();
		}
	}

	/**
	 * Increase reference count by one. Used for activities to share the same memory cache.
	 */
	public void onCreate()
	{
		mRefCount++;
		Log.d(TAG, "mRefCount: " + mRefCount);
	}

	/**
	 * Decrease reference count by one. Used for activities to share the same memory cache.
	 */
	public void onDestroy()
	{
		mRefCount--;
		Log.d(TAG, "mRefCount: " + mRefCount);
		if (mRefCount <= 0)
		{
			clear();
		}
	}
}
