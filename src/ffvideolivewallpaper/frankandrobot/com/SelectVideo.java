// Copyright 2011 Uriel Avalos and Frank and Robot Productions

// This software uses libraries from FFmpeg licensed under the LGLv2.1.

// This software uses GLWallpaperService licensed under the Apache v2.

// This file is part of FFvideo Live Wallpaper.

// FFvideo Live Wallpaper is free software: you can redistribute it
// and/or modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.

// FFvideo Live Wallpaper is distributed in the hope that it will be
// useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with FFvideo Live Wallpaper.  If not, see <http://www.gnu.org/licenses/>.

package ffvideolivewallpaper.frankandrobot.com;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
/* cursor stuff */
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore.Video.Media;
import android.provider.MediaStore.Video.Thumbnails;
import android.net.Uri;
import android.content.ContentUris;
import android.widget.SimpleCursorAdapter;
import android.content.Context;
/* thumbnail */
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
/* logging */
import android.util.Log;
/* threading */
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import android.os.Handler;
import android.widget.ListView;
/* scrolling detection */
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
/* selection */
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
   
public class SelectVideo extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener,
	       ListView.OnScrollListener {

    static public String TAG="VideoSettings";
    private Cursor mCursor;
    private ContentResolver cr;
    private VideoSettingsAdapter mAdapter;
    /* detecting scrolling */
    private volatile boolean flinging = false;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
	//set default layout
	setContentView(R.layout.settings);
	//get cursor
	cr = getContentResolver();
	//Use android.provider.MediaStore.Video.EXTERNAL_CONTENT_URI as
	//per docs MediaStore.Video.Media.html
	//We want DISPLAY_NAME and _ID for now
	//If we ever upgrade we'll probably also want TITLE 
	String[] projection = new String[] {
	    Media.DISPLAY_NAME,
	    Media._ID };
	mCursor = cr.query(Media.EXTERNAL_CONTENT_URI,
			   projection, //with these fields
			   null, //get all videos
			   null, //
			   null
			   );
	startManagingCursor(mCursor);
	//setup adapter
	mAdapter = new VideoSettingsAdapter(
					    getApplicationContext(),
					    R.layout.settings_row,
					    mCursor,
					    new String[] {
						Media.DISPLAY_NAME },
					    new int[] {R.id.video_name}
					    );
	//set pointer to ListView
	mAdapter.setListView(getListView());
	//set pointer to adapter
	setListAdapter(mAdapter);
	//load preferences
        getPreferenceManager().setSharedPreferencesName(
                VideoLiveWallpaper.SHARED_PREFS_NAME);
	//set listener so that the wallpaper knows when we pick a
	//video
	getPreferenceManager().getSharedPreferences()
	    .registerOnSharedPreferenceChangeListener(this);
	//setup scrolling detection
	getListView().setOnScrollListener(this);
	//setup selection
	getListView().setOnItemClickListener(new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, 
					View view,
					int position, 
					long lid) {
		    //all we care about is the position
		    //get id
		    mCursor.moveToPosition(position);
		    int video_column_index = 
			mCursor.getColumnIndex(Media._ID);
		    int id = mCursor.getInt(video_column_index);
		    //now get filename
		    Uri videoUri = ContentUris
			.withAppendedId(Media.EXTERNAL_CONTENT_URI, 
					Long.valueOf(id));
		    Cursor cursor = cr.query(videoUri, 
					     new String[] {Media.DATA},
					     null, null, null);
		    video_column_index = cursor.getColumnIndex(Media.DATA);
		    cursor.moveToFirst();
		    String cvideoName = cursor.getString(video_column_index);
		    Log.d(TAG,
			  "Selected video filename: "
			  + cvideoName
			  );
		    //save the filename to prefs file
		    SharedPreferences settings = 
			getSharedPreferences(VideoLiveWallpaper
					     .SHARED_PREFS_NAME, 0);
		    SharedPreferences.Editor editor = settings.edit();
		    editor.putString("videoName", cvideoName);
		    // Commit the edits!
		    editor.commit();
		    finish(); //bye bye
		}
	    });
	//debugging info
	if (MyDebug.videoSettings) {
	    mCursor.moveToFirst();
	    String displayName, displayTitle, magicID, mid; 
	    int dnc = mCursor.getColumnIndex(Media.DISPLAY_NAME);
	    int idc = mCursor.getColumnIndex(Media._ID);
	    do {
		// Get the field values
		displayName = mCursor.getString(dnc);
		mid = mCursor.getString(idc);
		// Do something with the values. 
		Log.d(TAG,
		      "videos? "+displayName+" "
		      +mid);
	    } while (mCursor.moveToNext());
	}
    }

    public void onScroll(AbsListView view, int f, int v, int t) {}

    public void onScrollStateChanged(AbsListView view, int scrollState) {
	switch ( scrollState) {
	case OnScrollListener.SCROLL_STATE_IDLE:
	    flinging = false;
	    break;
	case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
	    flinging = false;
	    break;
	case OnScrollListener.SCROLL_STATE_FLING:
	    flinging = true;
	    break;
	}
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences()
	    .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged
	(SharedPreferences sharedPreferences,
	 String key) {
	
    }

    public class VideoSettingsAdapter extends SimpleCursorAdapter {
	/* cursor variables */
	int video_column_index;
	int id;
	/* listview display variables */
	String videoName;
	Bitmap thumb;
	ImageView iv;
	TextView tv;
	/* thread variables */
	ConcurrentMap<Integer, Bitmap> drawableMap;
	BlockingQueue<Runnable> threadPool;
	AtomicInteger lastPosition = new AtomicInteger(1);
	Thread handler;
	/* pointers to parents */
	Context context;
	ListView parent;

	public VideoSettingsAdapter(Context con, 
				    int layout, 
				    Cursor c, 
				    String[] from, 
				    int[] to) {
	    super(con,layout,c,from,to);
	    this.context=con;
	    // Setup the shared variables
	    drawableMap = new ConcurrentHashMap<Integer, Bitmap>();
	    threadPool = new LinkedBlockingQueue<Runnable>(10);
	    // Start the thread
	    //manager = new ThreadPoolManager();
	    handler = new Thread(new Runnable(){
		    @Override
			public void run() {
			while (true) {
			    if ( threadPool.peek() != null ) {
				Runnable downloader =
				    (Runnable) threadPool.poll();
				downloader.run();
			    }
			    //wait
			}
		    }
		});
	    handler.start();
	}

	public void setListView(ListView parent) {
	    this.parent = parent;
	}

	public View getView(int position,
			    View convertView,
			    ViewGroup parent) {
	    //inflate row
	    View row=convertView;
	    if ( row==null ) {
		LayoutInflater inflater=
		    (LayoutInflater) context
		    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		row=inflater.inflate(R.layout.settings_row,null);
	    }
	    //init cursor
	    mCursor.moveToPosition(position);
	    //get filename
	    video_column_index = mCursor
		.getColumnIndex(Media.DISPLAY_NAME);
	    videoName = mCursor.getString(video_column_index);
	    if (MyDebug.videoSettings) Log.d(TAG,"video name: "
					     +videoName);
	    //set filename
	    tv = (TextView) row.findViewById(R.id.video_name);
	    tv.setText(videoName);
	    //get thumb ID
	    video_column_index = mCursor
		.getColumnIndex(Media._ID);
	    id = mCursor.getInt(video_column_index);
	    if (MyDebug.videoSettings) Log.d(TAG,"video id: "
					     +String.valueOf(id));
	    //get thumbnail
	    iv = (ImageView) row.findViewById(R.id.video_thumb);
	    thumb=getBitmapThumb(id,position);
	    //set thumbnail
	    if (thumb==null) iv.setImageResource(R.drawable.movie);
	    else iv.setImageBitmap(thumb);
	    //return
	    if (MyDebug.videoSettings) 
		Log.d(TAG,"Loading position "+String.valueOf(position));
	    return (row);
	}

	private Bitmap getBitmapThumbOnThread(Integer id) {
	    Bitmap thumb=Thumbnails
		.getThumbnail(cr,
			      id,
			      Thumbnails.MICRO_KIND,
			      null);
	    //put it in the hashmap
	    //but before we do that, do some simple memory management
	    if (drawableMap.size() > 200 ) drawableMap.clear();
	    drawableMap.putIfAbsent(id, thumb);
	    return thumb;
        }

	private Bitmap 
	    getBitmapThumb(final Integer id, 
			   final int position) {
	    //check to see if it was previously loaded
	    if (drawableMap.containsKey(id)) {
		return drawableMap.get(id);
	    }
	    //empty the thread queue if the last known position is out
	    //of range
	    if ( lastPosition.get() <= parent.getFirstVisiblePosition()
		 ||
		 lastPosition.get() >= parent.getLastVisiblePosition() ) {
		threadPool.clear();
		if (MyDebug.videoSettings) 
		Log.d(TAG,"Emptying queue: "
		      +lastPosition.get()+" "
		      +parent.getFirstVisiblePosition()+" "
		      +parent.getLastVisiblePosition());
	    }
	    if ( threadPool.remainingCapacity() > 0 ) {
		lastPosition.set(position);
	    }
	    //otherwise load it.
	    if (MyDebug.videoSettings)
	    Log.d(TAG,"Adding to queue bitmap position: "
		  + String.valueOf(position));
	    threadPool.offer(new Runnable() {
		    @Override
			public void run() {
			//load the image
			Bitmap thumb = getBitmapThumbOnThread(id);
			//tell the GUI that we're done
			runOnUiThread(new Runnable() {
				public void run() {
				    parent.invalidateViews();
				}
			    });
		    }
		});
	    return null;
	}
    }

    // public class BooleanLock {
   
    // 	private final ReentrantReadWriteLock readWriteLock = 
    // 	    new ReentrantReadWriteLock();
    // 	private final Lock read  = readWriteLock.readLock(); 
    // 	private final Lock write = readWriteLock.writeLock();
    // 	private boolean value;
    // 	private boolean firstSetReady=false;
    // 	private boolean secondSetReady=false;
    // 	private boolean[] frameAvail;

    // 	public BooleanLock(boolean v) {
    // 	    value = v;
    // 	}

    // 	public void set(final boolean value) {
    // 	    write.lock();
    // 	    try {
    // 		this.value = value;
    // 	    } finally {
    // 		write.unlock();
    // 	    }
    // 	}

    // 	public boolean get() {
    // 	    read.lock();
    // 	    try{
    // 		return value;
    // 	    } finally {
    // 		read.unlock();
    // 	    }
    // 	}
    // }
}
