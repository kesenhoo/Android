package com.hoo.AsyncImageDecode;

import java.util.Iterator;
import java.util.Set;
import android.util.Log;

/**
 * Cached Music Picture Bitmap By Position 
 * @author hoo
 * @since 2012-09
 */
public class MemoryCacheMBitmapByPosition extends MemoryCacheMBitmap<Integer>
{
	private int					mPosition	= 0;
	private static final String	TAG			= "[AsyncImageDecodeDemo]";

	public MemoryCacheMBitmapByPosition(int count)
	{
		super(count);
		mPosition = 0;
	}

	public void setPosition(int position)
	{
		if (position < 0)
		{
			position = 0;
		}
		mPosition = position;
	}

	/**
	 * Remove the lowest priority item
	 */
	@Override
	protected boolean removeTheLowestPriorityItem(Integer key)
	{
		if (mItems.size() <= 0)
		{
			Log.e(TAG, "[removeTheLowestPriorityItem]List Size <=0");
			return true;
		}
			
		if (key == null)
		{
			Log.e(TAG, "[removeTheLowestPriorityItem]release item key is null");
			return false;
		}

		Set<Integer> keySet = mItems.keySet();
		Iterator<Integer> it = keySet.iterator();
		Integer lowestPriorityKey = null;
		Integer compareKey = null;
		int lowestDisparity = 0;
		int compareDisparity = 0;
		if (it.hasNext())
		{
			lowestPriorityKey = it.next();
			while (it.hasNext())
			{
				compareKey = it.next();
				if (lowestPriorityKey != null && compareKey != null)
				{
					lowestDisparity = Math.abs(lowestPriorityKey.intValue() - mPosition);
					compareDisparity = Math.abs(compareKey.intValue() - mPosition);
					if (compareDisparity > lowestDisparity)
					{
						lowestPriorityKey = compareKey;
					}
				}
			}
		}

		if (lowestPriorityKey == null)
		{
			return false;
		}

		// check if all items in cache have closer position
		int keyDisparity = 0;
		lowestDisparity = Math.abs(lowestPriorityKey.intValue() - mPosition);
		keyDisparity = Math.abs(key.intValue() - mPosition);
		if (keyDisparity > lowestDisparity)
		{
			return false;
		}

		MBitmap item = mItems.remove(lowestPriorityKey);
		Log.d(TAG, "release item: " + lowestPriorityKey.toString());

		if (item != null)
		{
			releaseItemResource(item);
		}
		return true;
	}

	/**
	 * Push to Cache
	 * @param key
	 * @param bitmap
	 * @return
	 */
	public boolean push(int key, MBitmap bitmap)
	{
		return super.push(key, bitmap);
	}

	/**
	 * Returns a set of the keys contained in cache.
	 * @return a set of the keys.
	 */
	public Set<Integer> keySet()
	{
		if (mItems == null)
		{
			return null;
		}
		return mItems.keySet();
	}

	/**
	 * Update the position (key) of existing bitmaps. If there is a bitmap
	 * associated with new key, this bitmap will be removed from the cache.
	 * 
	 * @param oldKey
	 * @param newKey
	 */
	public void updateKey(int oldKey, int newKey)
	{
		mLock.lock();
		try
		{
			if (mItems != null)
			{
				MBitmap bitmap = pull(oldKey);
				if (bitmap != null)
				{
					remove(oldKey);
					push(newKey, bitmap);
				}
			}
		}
		finally
		{
			mLock.unlock();
		}
	}
}
