package com.hoo.AsyncImageDecode;



import android.os.Parcel;
import android.os.Parcelable;

/**
 * The Song entity
 * @author kesenhoo
 * @since 2012-09-12
 */
public class WrapperSong implements Parcelable
{
	//
	private int SongId;
	//
	private int SingerId;
	//
	private int AlbumId;
	//
	private int TotalSong;
	//
	private String SingerName;
	//
	private String SongName;
	//
	private String AlbumName;
	//
	private String SongImage;
	//
	private String SongDescription;
	//
	private String SongPlayTime;
	//
	private String SongPlayUrl;
	//
	private String SongDetailUrl;
	
	
	public static final Parcelable.Creator<WrapperSong> CREATOR = new Creator<WrapperSong>()
	{
		public WrapperSong createFromParcel(Parcel paramParcel)
		{
			WrapperSong mWrapperSong = new WrapperSong();
			mWrapperSong.readFromParcel(paramParcel);
			return mWrapperSong;
		}

		@Override
		public WrapperSong[] newArray(int arg0)
		{
			// TODO Auto-generated method stub
			return new WrapperSong[arg0];
		}
	};
	
	public int getTotalSong()
	{
		return TotalSong;
	}
	public void setTotalSong(int totalSong)
	{
		TotalSong = totalSong;
	}
	
	public int getSongId()
	{
		return SongId;
	}
	public void setSongId(int songId)
	{
		SongId = songId;
	}
	public int getSingerId()
	{
		return SingerId;
	}
	public void setSingerId(int singerId)
	{
		SingerId = singerId;
	}
	public String getSingerName()
	{
		return SingerName;
	}
	public void setSingerName(String singerName)
	{
		SingerName = singerName;
	}
	public String getSongName()
	{
		return SongName;
	}
	public void setSongName(String songName)
	{
		SongName = songName;
	}
	public int getAlbumId()
	{
		return AlbumId;
	}
	public void setAlbumId(int albumId)
	{
		AlbumId = albumId;
	}
	public String getAlbumName()
	{
		return AlbumName;
	}
	public void setAlbumName(String albumName)
	{
		AlbumName = albumName;
	}
	public String getSongImage()
	{
		return SongImage;
	}
	public void setSongImage(String songImage)
	{
		SongImage = songImage;
	}
	public String getSongDescription()
	{
		return SongDescription;
	}
	public void setSongDescription(String songDescription)
	{
		SongDescription = songDescription;
	}
	public String getSongPlayTime()
	{
		return SongPlayTime;
	}
	public void setSongPlayTime(String songPlayTime)
	{
		SongPlayTime = songPlayTime;
	}
	public String getSongPlayUrl()
	{
		return SongPlayUrl;
	}
	public void setSongPlayUrl(String songPlayUrl)
	{
		SongPlayUrl = songPlayUrl;
	}
	public String getSongDetailUrl()
	{
		return SongDetailUrl;
	}
	public void setSongDetailUrl(String songdetailUrl)
	{
		SongDetailUrl = songdetailUrl;
	}
	
	@Override
	public int describeContents()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected void readFromParcel(Parcel paramParcel)
	{
		// TODO Auto-generated method stub
		this.SongId = paramParcel.readInt();
		this.AlbumId = paramParcel.readInt();
		this.SingerName = paramParcel.readString();
		this.SongName = paramParcel.readString();
		this.AlbumName = paramParcel.readString();
		this.SongImage = paramParcel.readString();
		this.SongDescription = paramParcel.readString();
		this.SongPlayTime = paramParcel.readString();
		this.SongPlayUrl = paramParcel.readString();
		this.SongDetailUrl = paramParcel.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		// TODO Auto-generated method stub
		dest.writeInt(SongId);
		dest.writeInt(SingerId);
		dest.writeInt(AlbumId);
		dest.writeString(SingerName);
		dest.writeString(SongName);
		dest.writeString(AlbumName);
		dest.writeString(SongImage);
		dest.writeString(SongDescription);
		dest.writeString(SongPlayTime);
		dest.writeString(SongPlayUrl);
		dest.writeString(SongDetailUrl);
	}
}
