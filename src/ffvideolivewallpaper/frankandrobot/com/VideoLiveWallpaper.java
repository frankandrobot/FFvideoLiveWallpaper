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

import net.rbgrn.android.glwallpaperservice.*;
import android.util.Log;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.io.IOException;
import java.util.Arrays;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.app.WallpaperManager;
import android.util.TypedValue;
import android.content.res.Resources;
import android.os.SystemClock;
/* for loading file */
import android.content.res.AssetManager;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import android.graphics.RectF;
/* for threads */
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.lang.Math; 
import android.view.WindowManager;
import android.view.Display;
/* shared preferences */
import android.content.SharedPreferences;
import android.content.Intent;
/* communicating with renderer */
import android.os.Handler;
import android.os.Message;
/* math */
import java.lang.Math;
/* opengl */
import android.opengl.GLSurfaceView;

public class VideoLiveWallpaper extends GLWallpaperService 
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    //preference variables
    public static final String SHARED_PREFS_NAME="videowallpapersettings";
    public static final String folder = "video";
    public static final String TAG = "VLW";
    public static final String defaultVideoName="sdcard/VIDEOWALL102344df@#%";
    String videoName=defaultVideoName, prevVideoName = defaultVideoName;
    boolean videoLoaded=false;

    //video variables
    long fps=33; //33ms corresponds to 30 fps
    static int oldxo=-1;
    //activity variables
    private SharedPreferences mPrefs;

    public VideoLiveWallpaper() { 
	super(); 
	if (MyDebug.VLW) Log.d(TAG,"constructor()");	
    }

    @Override
    public void onCreate() {
	if (MyDebug.VLW) Log.d(TAG,"onCreate()");
        super.onCreate();
	//load prefs
	mPrefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
	//register listener so that we know when user changed settings
	mPrefs.registerOnSharedPreferenceChangeListener(this);
	//load prefs 
	onSharedPreferenceChanged(mPrefs, null);
	//wait for card to mount
	while( ! android.os.Environment.getExternalStorageState()
	       .equals(android.os.Environment.MEDIA_MOUNTED)) {
	    try {
		if (MyDebug.VLW) Log.d(TAG,"Waiting for SD card...");
		Thread.sleep(250);
	    } catch (InterruptedException e) {}
	}
	if ( videoName.equals(defaultVideoName ) ) {
		//no video chosen so load default video
		//transfer video to sdcard
		if (MyDebug.VLW) 
		    Log.d(TAG,"transferring video asset to sdcard");
		copyVideoToCard();
		if (MyDebug.VLW) Log.d(TAG,"transferred");
	}
	NativeCalls.initVideo();
	if (MyDebug.VLW) Log.d(TAG,"Opening video");
	NativeCalls.loadVideo("file:/"+videoName);
	videoLoaded = true;
    }

    void copyVideoToCard() {
	//open file in assets
	AssetManager assetManager = getAssets();
	String[] videoAsset = null;
	try {
	    videoAsset = assetManager.list(folder);
	} catch (IOException e) {
	    //Log.e(TAG, e.getMessage());
	}
	InputStream in = null;
	OutputStream out = null;
	try {
	    in = assetManager.open(folder+"/"+videoAsset[0]);
	    out = new FileOutputStream("/sdcard/" + defaultVideoName);
	    //actually copy over file
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
		out.write(buffer, 0, read);
	    }
	    //close up everything
	    in.close();
	    in = null;
	    out.flush();
	    out.close();
	    out = null;
	    //assetManager.close();
	} catch(Exception e) {
	    //Log.e(TAG, e.getMessage());
	}
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, 
     					  String key) {
     	if (MyDebug.VLW) Log.d(TAG,"Preferences changed/Loading new");
     	SharedPreferences settings = 
     	    getSharedPreferences(SHARED_PREFS_NAME, 0);
	//boolean loopVideo = settings.getBoolean("loopVideo",true);
	//NativeCalls.setLoopVideo(loopVideo);
	final float actualFPS = (float) settings.getInt("fps",30);
	final boolean spanVideo = settings.getBoolean("spanVideo",true);
	setFPS(actualFPS);
	NativeCalls.setSpanVideo(spanVideo);
	if (MyDebug.VLW) Log.d(TAG,"span video :"+spanVideo);
	//file name
	prevVideoName = videoName;
	videoName = settings.getString("videoName",defaultVideoName);
	//if video already loaded and chose new video, then reload
	if ( !prevVideoName.equals(videoName) && videoLoaded ) {
	    //TODO
	    mEngine.queueEvent(new Runnable() {
	     	    @Override
	     		public void run() {
			if (MyDebug.VLW) Log.d(TAG,"Resetting video");
			NativeCalls.freeConversionStorage();
			NativeCalls.freeVideo();
			NativeCalls.loadVideo("file:/"+videoName);
			mEngine.renderer.process2();
			NativeCalls.initOpenGL();
		     }
		});
	}
    }
    
    @Override
    public void onDestroy() {
	if (MyDebug.VLW) Log.d(TAG,"onDestroy()");
        super.onDestroy();
	NativeCalls.closeVideo();
    }

    void setFPS(float actualFPS) {
	fps = (actualFPS==0) ? 255000 : (long) (1000f / actualFPS); 
	//ex: 1000 / 30fps = 33 ms
    }

    private CubeEngine mEngine=null;

    @Override
    public Engine onCreateEngine() {
	if (MyDebug.VLW) Log.d(TAG,"onCreateEngine()");
        mEngine = new CubeEngine();
	return mEngine;
    }

    //offsets changed
    static public float xStep = 0, yStep = 0;
    static public float xOffset = 0, yOffset = 0;

    class CubeEngine extends GLWallpaperService.GLEngine {
	int xs, ys, xo, yo;
	int noScreensX, noScreensY;
	//fps
	private long fpsTime;
	
        private boolean mVisible;
	
	public VideoRenderer renderer = null;

	private final Handler mHandler = new Handler();

	private final Runnable mDrawFrame = new Runnable() {
		@Override
		    public void run() {
		    drawFrame();
		}
	    };

	CubeEngine() { 
	    super();
	    if (MyDebug.VLW) Log.d(TAG,"CubeEngine CubeEngine()");
	    renderer = new VideoRenderer(VideoLiveWallpaper.this, 
					 this);
	    setRenderer(renderer);
	    setRenderMode(RENDERMODE_WHEN_DIRTY);
	    //setRenderMode(RENDERMODE_CONTINUOUSLY);
	}

	VideoRenderer getRenderer() { return renderer; }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            if (MyDebug.VLW) Log.d(TAG,"CubeEngine onCreate()");
	    super.onCreate(surfaceHolder);
        }

        @Override
        public void onDestroy() {
            if (MyDebug.VLW) Log.d(TAG,"CubeEngine onDestroy()");
	    if (renderer != null) {
		renderer.release();
	    }
	    renderer = null;
	    mVisible = false;
	    super.onDestroy();
        }

	//This is really the constructor/many things
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, 
				     int w, int h) {
	    if (MyDebug.VLW) Log.d(TAG, "CubeEngine onSurfaceChanged()");
            super.onSurfaceChanged(holder, format, w, h);
	    drawFrame();
	}

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
	    if (MyDebug.VLW) Log.d(TAG,"CubeEngine onSurfaceCreated()");
            super.onSurfaceCreated(holder);
	    drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
	    if (MyDebug.VLW) Log.d(TAG,"CubeEngine onSurfaceDestroyed");
            super.onSurfaceDestroyed(holder);
	    mVisible = false;
	    mHandler.removeCallbacks(mDrawFrame);
	}

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
	    if ( isPreview() ) return;
	    super.onOffsetsChanged(xOffset,yOffset,xStep,yStep,xPixels,yPixels);
	    if (MyDebug.VLW) 
		Log.d(TAG,"onoffsetschanged:"+String.valueOf(xOffset)
	     	  +","+String.valueOf(xStep)+","+String.valueOf(xPixels));
	    VideoLiveWallpaper.xOffset = xOffset;
	    VideoLiveWallpaper.yOffset = yOffset;
	    VideoLiveWallpaper.xStep = xStep;
	    VideoLiveWallpaper.yStep = yStep;
	    calculateWallpaperDimensions();
	    mapXYoffsets();
	    //now that we know where the screen is on the wall, update
	    //the video
	    NativeCalls
		.setWallDimensions(
				   getRenderer().wallWidth,
				   getRenderer().wallHeight);
	    NativeCalls
		.setVideoMargins(
				 getRenderer().marginX,
				 getRenderer().marginY);
	    NativeCalls.setOffsets(xo,yo);
	    NativeCalls.setSteps(xs,ys);
	    NativeCalls.updateVideoPosition();
	    drawFrame();
	    //requestRender();
	    //If ( renderer != null ) {
	    if ( oldxo != xo ) {
	    	oldxo = xo;
	    	// Log.d(TAG,"Wall dims: "+getRenderer().wallWidth+"x"
	    	//       +getRenderer().wallHeight);
	    	// Log.d(TAG,"Wall video margins: "
	    	//       +getRenderer().marginX
	    	//       +"x"+getRenderer().marginY);
	    	// Log.d(TAG,"No of screens: "+noScreensX
	    	//       +"x"+noScreensY);
	    	// Log.d(TAG,"xo,xs = "+String.valueOf(xo)
	    	//       +","+String.valueOf(xs));
	    }
	}
	
	public void calculateWallpaperDimensions() {
	    //calculate number of screens
	    noScreensX = ( Float.compare(xStep,0.0f) == 0 )
		? 1 : ((int) (1.0f/xStep)) + 1;
	    noScreensY = ( Float.compare(yStep,0.0f) == 0 ) 
		? 1 : ((int) (1.0f/yStep)) + 1;
	    //now calculate wallpaper dimensions
	    getRenderer().wallWidth = getRenderer().screenWidth * noScreensX;
	    getRenderer().wallHeight = getRenderer().screenHeight * noScreensY;
	    //why we cant do the above in onCreate() I don't know
	    //calculate video margin on wall
	    getRenderer().marginX = (getRenderer().wallWidth 
				     - getRenderer().wallVideoWidth) / 2;
	    getRenderer().marginY = (getRenderer().wallHeight 
				     - getRenderer().wallVideoHeight) / 2;
	}

	public void mapXYoffsets() {
	    //convert xoffset and xstep to pixels the problem is that
	    //xstep doesn't quite correspond to wall dimensions
	    float adjustedWidth = 
		(float) getRenderer().wallWidth - (float) getRenderer().wallWidth 
		/ (float) noScreensX;
	    float adjustedHeight = 
		(float) getRenderer().wallHeight - (float) getRenderer().wallHeight
		/ (float) noScreensY;
	    xo= ((int) (xOffset * (float) adjustedWidth));
	    xs = ((int) (xStep * (float) adjustedWidth));
	    yo= ((int) (yOffset * (float) adjustedHeight));
	    ys = ((int) (yStep * (float) adjustedHeight));
	}

	@Override
	public void onVisibilityChanged(boolean visible) {
	    if (MyDebug.VLW) Log.d(TAG,"CubeEngine onVisibilityChanged()");
	    super.onVisibilityChanged(visible);
	    mVisible = visible;
            if (visible) {
		drawFrame();
            } else {
		mHandler.removeCallbacks(mDrawFrame);
            }
	}

	// @Override
	//     public void onTouchEvent(MotionEvent event) {
	//     //VideoLiveWallpaper.this.stopSelf();
	// }

	public void drawFrame() {
	    mHandler.removeCallbacks(mDrawFrame);
	    if (mVisible) {
	    	requestRender();
	    	mHandler.postDelayed(mDrawFrame, fps);
	    }
	}
    }
} 