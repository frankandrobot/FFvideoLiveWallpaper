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
import java.util.Map;
import java.util.HashMap;
import android.os.Handler;
import android.os.Message;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import android.widget.ListView;
/* scrolling detection */
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
/* selection */
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
   
public class VideoWallpaperSettings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    static public String TAG="VideoSettings";
    static public String blankVideo="RANDOM_NAME102344df@@#%";
    private Cursor mCursor;
    private ContentResolver cr;
    //private VideoSettingsAdapter mAdapter;
    /* detecting scrolling */
    //private BooleanLock isFlinging = new BooleanLock(false);
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
	//set default layout
	//setContentView(R.layout.settings);
	//load preferences
        getPreferenceManager().setSharedPreferencesName(
                VideoLiveWallpaper.SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.video_settings);
	//set listener so that the wallpaper knows when settings changed
	getPreferenceManager().getSharedPreferences()
	    .registerOnSharedPreferenceChangeListener(this);
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
}
