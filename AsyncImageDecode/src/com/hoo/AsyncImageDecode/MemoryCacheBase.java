package com.hoo.AsyncImageDecode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

/**
 * Memory Cache Base Class
 * @author hoo
 * @since 2012-09
 * @param <KEY>
 * @param <T>
 */
public class MemoryCacheBase<KEY, T>
{
	// ConcurrentHashMap Cache pool.
	protected ConcurrentHashMap<KEY, T>	mItems	= new ConcurrentHashMap<KEY, T>();
	// Reentrant Lock
	protected ReentrantLock				mLock	= new ReentrantLock(true);
	
	public static final String			TAG		= "[AsyncImageDecodeDemo]";

	/**
	 * Release Cache Item(Need Override)
	 * @param item
	 */
	protected void releaseItemResource(T item)
	{
	}

	/**
	 * Push item to cache pool.
	 * @param key
	 * @param item
	 * @return boolean to success or fail.
	 */
	public boolean push(KEY key, T item)
	{
		if (key == null || item == null)
		{
			Log.e(TAG, "[push] The Key or Item is null");
			return false;
		}

		try
		{
			mLock.lock();
			if (mItems.containsKey(key))
			{
				T t = mItems.remove(key);
				if (t != null)
				{
					releaseItemResource(t);
				}
				Log.d(TAG, "release item resource " + key.toString() + " in push.");
			}

			// Keep check capacity to free spaces.
			if (!checkCapacity(item))
			{
				boolean removed = removeTheLowestPriorityItem(key);
				if (!removed)
				{
					// all items in the cache have higher priority.
					// release this item if required since cannot add to
					// cache.
					releaseItemResource(item);
					Log.d(TAG, "all items in the cache have higher priority.");
					return false;
				}
			}
			mItems.put(key, item);
		}
		finally
		{
			mLock.unlock();
		}
		return true;
	}

	/**
	 * Pull item from cache pool.
	 * 
	 * @param key
	 * @return Item
	 */
	public T pull(KEY key)
	{
		try
		{
			mLock.lock();
			return mItems.get(key);
		}
		finally
		{
			mLock.unlock();
		}
	}

	/**
	 * Clear cache pool.
	 */
	public void clear()
	{
		mItems.clear();
	}

	/**
	 * Remove Item from cache pool
	 * 
	 * @param key
	 */
	public void remove(KEY key)
	{
		mItems.remove(key);
	}

	/**
	 * Whether Contain this item
	 * @param key
	 * @return
	 */
	public boolean containsKey(KEY key)
	{
		return mItems.containsKey(key);
	}

	/**
	 * Implement this function for capacity restriction.
	 * @return true to have access to add or false to no access.
	 * @throws OutOfCapacityException
	 * @see removeTheLowestPriorityItem
	 */
	protected boolean checkCapacity(T item)
	{
		return true;
	}

	/**
	 * Remove the lowest priority item from cache pool.
	 * @TODO: add 
	 * @return true to successfully removed or false to remove fail.
	 */
	protected boolean removeTheLowestPriorityItem(KEY k)
	{
		return true;
	}
}
