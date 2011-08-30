// Copyright 2011 Uriel Avalos and Frank and Robot Productions

// This software uses libraries from FFmpeg licensed under the LGLv2.1.

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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.view.SurfaceHolder;
import java.io.File;
import java.io.FileInputStream;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import net.rbgrn.android.glwallpaperservice.*;
import android.opengl.GLU;
import java.nio.FloatBuffer;
import java.nio.Buffer;
import java.io.BufferedInputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.os.SystemClock;
import android.util.Log;
/* threads */
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.Thread;
import java.lang.InterruptedException;
/* screen dimensions */
import android.view.Display;
import android.content.Context;
import android.view.WindowManager;
import android.app.WallpaperManager;

// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers
public class VideoRenderer implements GLWallpaperService.Renderer {
    static private String TAG="Renderer>>>>>>>>>>>>";
    static boolean runOnce = false;
    //screen variables
    public int screenWidth=50,screenHeight=50;
    boolean isScreenPortrait=true;
    int drawWidth, drawHeight; //dimensions of fit-to-screen video
    int paddingX, paddingY; //padding for fit-to-screen-video
    //wallpaper variables
    public volatile int wallWidth,wallHeight;
    public volatile int marginX, marginY;
    //video variables
    int tWidth, tHeight;
    int wallVideoWidth=50,wallVideoHeight=50;
    int videoWidth,videoHeight;
    boolean isPreview=true;
    boolean videoWideScreen=false;
    long fps=33; //33ms corresponds to 30 fps
    //texture variables
    int powWidth,powHeight;

    float scaleFactor;
    long fpsTime;
    //pointers
    VideoLiveWallpaper mParent;
    VideoLiveWallpaper.CubeEngine mParentEngine;
    //static public boolean killed = false;
    //lock
    static public Object lock = new Object();

    public VideoRenderer() { 
	super();
	if (MyDebug.VR) Log.d(TAG,"Constructor()");
    }

    public VideoRenderer(VideoLiveWallpaper p, 
			   VideoLiveWallpaper.CubeEngine e) {
	super();
	mParent = p;
	mParentEngine = e;
	//VideoRenderer.killed = false;
	if (MyDebug.VR) Log.d(TAG,"constructor()");
    }

    public VideoRenderer(VideoLiveWallpaper p, 
			   VideoLiveWallpaper.CubeEngine e,
			   int w,
			   int h) {
	super();
	mParent = p;
	mParentEngine = e;
	videoWidth = w;
	videoHeight = h;
	//VideoRenderer.killed = false;
	if (MyDebug.VR) Log.d(TAG,"constructor()");
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	if (MyDebug.VR) Log.d(TAG, "onSurfaceCreated()");
    }

    public void process(int width, int height) {
	setScreenDimensions( width, height );
	//kill previous opengl (probably not necessary)
	if (MyDebug.VR) Log.d(TAG,"Killing texture");
	NativeCalls.closeOpenGL();
	//set video dimensions
	videoWidth = NativeCalls.getVideoWidth();
	videoHeight = NativeCalls.getVideoHeight();
	videoWideScreen = ( videoWidth > videoHeight ) ? true : false;
	isScreenPortrait = 
	     ( screenWidth > screenHeight ) ? false : true;
	NativeCalls.setOrientation( isScreenPortrait );
	//span video
	isPreview = mParentEngine.isPreview();
	NativeCalls.setPreviewMode( isPreview );
	//spanVideo = (mParentEngine.isPreview()) ? false : true;
	//NativeCalls.setSpanVideo(spanVideo);
	//Log.d(TAG,"spanVideo: " + spanVideo);
	//set wallpaper video dimensions
	setWallVideoDimensions( videoWidth, videoHeight );
	//set texture
	setTextureDimensions( screenWidth, screenHeight );
	setFitToScreenDimensions( videoWidth, videoHeight );
	if ( !runOnce ) {
	    if (MyDebug.VR) Log.d(TAG,"Preparing frame");
	    NativeCalls.prepareStorageFrame();
	}
	NativeCalls.initOpenGL();
	runOnce = true;
	if ( mParentEngine.isPreview() ) return;
	mParentEngine.onOffsetsChanged(
				       VideoLiveWallpaper.xOffset, 
				       VideoLiveWallpaper.yOffset,
				       VideoLiveWallpaper.xStep, 
				       VideoLiveWallpaper.yStep,
				       0,0);
    }

    //This gets called whenever you preview the wallpaper or set the
    //wallpaper
    public void onSurfaceChanged(GL10 gl, int width, int height) {
	if (MyDebug.VR) Log.d(TAG,"onSurfaceChanged()");
	 synchronized(lock) {
	     process(width, height);
	 }
    }
    
    public void onDrawFrame(GL10 gl) {
	synchronized(lock) {
	    //Log.d(TAG,"Drawing ....");
	    NativeCalls.getFrame(); // from video
	    NativeCalls.drawFrame(); // using openGL
	    if (MyDebug.showFPS) {
		final float fpsRate;
		    fpsRate = 1000f/((float) (SystemClock.uptimeMillis() 
					      - fpsTime) );
		    fpsTime = SystemClock.uptimeMillis();
		    if (MyDebug.VR) 
		    Log.d(TAG, 
			  TAG+"drawFrame(): fps: "
			  +String.valueOf(fpsRate)
			  );
	    }
	}
    }
    
    @Override
	public void finalize() throws Throwable {
	//VideoRenderer.killed = true;
	super.finalize();
    }

    public void process2() {
	synchronized(lock) {
	    setScreenDimensions( screenWidth, screenHeight );
	    //set video dimensions
	    videoWidth = NativeCalls.getVideoWidth();
	    videoHeight = NativeCalls.getVideoHeight();
	    videoWideScreen = ( videoWidth > videoHeight ) ? true : false;
	    isScreenPortrait = 
		( screenWidth > screenHeight ) ? false : true;
	    NativeCalls.setOrientation( isScreenPortrait );
	    //span video
	    //isPreview = true;//mParentEngine.isPreview();
	    //NativeCalls.setPreviewMode( isPreview );
	    //spanVideo = false;//(mParentEngine.isPreview()) ? false : true;
	    //NativeCalls.setSpanVideo(spanVideo);
	    //Log.d(TAG,"spanVideo: " + spanVideo);
	    //set wallpaper video dimensions
	    setWallVideoDimensions( videoWidth, videoHeight );
	    //set texture
	    setTextureDimensions( screenWidth, screenHeight );
	    setFitToScreenDimensions( videoWidth, videoHeight );
	    if (MyDebug.VR) Log.d(TAG,"Preparing frame");
	    NativeCalls.prepareStorageFrame();
	    //NativeCalls.initOpenGL();
	}
    }

    /**
     * Called when the engine is destroyed. Do any necessary clean up because
     * at this point your renderer instance is now done for.
     */
    public void release() {
	if (MyDebug.VR) 
	    Log.d(TAG, 
	      "Released"
	      );
    }

    public void setScreenDimensions(int w, int h) {
	screenWidth = w;
	screenHeight = h;
	NativeCalls.setScreenDimensions( screenWidth,
					 screenHeight );
	if (MyDebug.VR) 
	    Log.d(TAG,"New screen dimensions:"+screenWidth+"x"+screenHeight);
    }

    //set video dimensions based on screen height (wallpaper
    //height)	
    //INPUT: video dimensions
    public void setWallVideoDimensions(int w, int h) {
	int[] newdims = scaleBasedOnHeight(w, h,
					   screenWidth, screenHeight);
	wallVideoWidth = newdims[0]; 
	wallVideoHeight = newdims[1];
	NativeCalls.
	    setWallVideoDimensions( wallVideoWidth,
				    wallVideoHeight) ;
	if (MyDebug.VR) 
	Log.d(TAG,"New wallpaper video dimensions:"+wallVideoWidth+"x"+wallVideoHeight);
    }

    //set texture dimensions 
    //set nearest power of 2 dimensions for
    //texture based on either screen dimensions OR video
    public void setTextureDimensions(int width, int height) {
	int s = Math.max( width, height );
	powWidth = getNextHighestPO2( s ) / 2;
	powHeight = getNextHighestPO2( s ) / 2;
	NativeCalls.setTextureDimensions( powWidth,
					  powHeight );
	if (MyDebug.VR) 
	    Log.d(TAG,"New texture dimensions:"+powWidth+"x"+powHeight);
    }

    //set dimensions for fit-to-screen video
    //INPUT: video dimensions
    public void setFitToScreenDimensions(int w, int h) {
	int[] newdims = scaleBasedOnScreen(w, h,
					   screenWidth, screenHeight);
	drawWidth = newdims[0]; 
	drawHeight = newdims[1];
	NativeCalls.setDrawDimensions(drawWidth,drawHeight);
	if (MyDebug.VR) 
	Log.d(TAG,"setupVideoParameters: fit-to-screen video:"+drawWidth+"x"+drawHeight);
	//set video padding
	paddingX = (int) ((float) (screenWidth - drawWidth) / 2.0f);
	paddingY = (int) ((float) (screenHeight - drawHeight) / 2.0f);
	NativeCalls.setScreenPadding(paddingX,paddingY);
	//set wallpaper dimensions in onOffsetChanged
	//why this can't be done here I have no idea
    }

    public static int getNextHighestPO2( int n ) {
	n -= 1;
	n = n | (n >> 1);
	n = n | (n >> 2);
	n = n | (n >> 4);
	n = n | (n >> 8);
	n = n | (n >> 16);
	n = n | (n >> 32);
	return n + 1;
    }

    public static int[] scaleBasedOnHeight(int iw, int ih, int sw, int sh) {
	int[] newdims = new int[2];
	float sf = (float) iw / (float) ih;
	newdims[0] = (int) ((float) sh * sf);
	newdims[1] = sh;
	return newdims;
    }

    public static int[] scaleBasedOnScreen(int iw, int ih, int sw, int sh) {
	int[] newdims = new int[2];
	//if ( videoWideScreen ) { int t=sw; sw=sh; sh=t; }
	float sf = (float) iw / (float) ih;
	newdims[0] = sw;
	newdims[1] = (int) ((float) sw / sf);
	if ( newdims[1] > sh ) { // new dims too big
	    newdims[0] = (int) ((float) sh * sf);
	    newdims[1] = sh;
	}
	return newdims;
    }	    
}