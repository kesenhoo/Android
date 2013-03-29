package com.hoo.AsyncImageDecode;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Used to keep the decoding path, and the bitmap. MBitmap stands for Music Bitmap.
 * @author hoo
 * @since 2012-09
 */
public class MBitmap
{
	//The Bitmap
	private Bitmap	mBitmap	= null;
	//The Bitmap Resource Path(Online:Url,Local:Path)
	private String	mPath	= null;

	/**
	 * Constructor
	 * @param bitmap
	 * @param path
	 */
	public MBitmap(Bitmap bitmap, String path)
	{
		mBitmap = bitmap;
		mPath = path;
	}

	public boolean isRecycled()
	{
		if (mBitmap == null)
		{
			Log.e("MBitmap", "[isRecycled] mBitmap is null");
			return true;
		}
		return mBitmap.isRecycled();
	}

	public Bitmap getBitmap()
	{
		return mBitmap;
	}

	public String getPath()
	{
		return mPath;
	}
}
