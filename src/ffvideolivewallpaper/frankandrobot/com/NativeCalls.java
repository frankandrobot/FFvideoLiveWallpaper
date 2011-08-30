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

public class NativeCalls {
    //ffmpeg
    public static native void initVideo();
    public static native void loadVideo(String fileName); //
    public static native void prepareStorageFrame();
    public static native void getFrame(); //
    public static native void freeConversionStorage();
    public static native void closeVideo();//
    public static native void freeVideo();//
    //opengl
    public static native void initPreOpenGL(); //
    public static native void initOpenGL(); //
    public static native void drawFrame(); //
    public static native void closeOpenGL(); //
    public static native void closePostOpenGL();//
    //wallpaper
    public static native void updateVideoPosition();
    public static native void setSpanVideo(boolean b);
    //getters
    public static native int getVideoHeight();
    public static native int getVideoWidth();
    //setters
    public static native void setWallVideoDimensions(int w,int h);
    public static native void setWallDimensions(int w,int h);
    public static native void setScreenPadding(int w,int h);
    public static native void setVideoMargins(int w,int h);
    public static native void setDrawDimensions(int drawWidth,int drawHeight);
    public static native void setOffsets(int x,int y);
    public static native void setSteps(int xs,int ys);
    public static native void setScreenDimensions(int w, int h);
    public static native void setTextureDimensions(int tx,
						   int ty );
    public static native void setOrientation(boolean b);
    public static native void setPreviewMode(boolean b);
    public static native void toggleGetFrame(boolean b);
    //fps
    public static native void setLoopVideo(boolean b);

    static {  
	System.loadLibrary("avcore");
	System.loadLibrary("avformat");
	System.loadLibrary("avcodec");
	//System.loadLibrary("avdevice");
	System.loadLibrary("avfilter");
	System.loadLibrary("avutil");
	System.loadLibrary("swscale");
	System.loadLibrary("video");  
    }  

}
