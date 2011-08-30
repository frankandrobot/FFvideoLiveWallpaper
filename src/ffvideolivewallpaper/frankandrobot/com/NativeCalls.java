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
