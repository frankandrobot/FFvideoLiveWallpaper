/* Copyright 2011 Uriel Avalos and Frank and Robot Productions */

/* This software uses libraries from FFmpeg licensed under the LGLv2.1. */

/* This software uses GLWallpaperService licensed under the Apache v2. */

/* This file is part of FFvideo Live Wallpaper. */

/* FFvideo Live Wallpaper is free software: you can redistribute it */
/* and/or modify it under the terms of the GNU General Public License as */
/* published by the Free Software Foundation, either version 3 of the */
/* License, or (at your option) any later version. */

/* FFvideo Live Wallpaper is distributed in the hope that it will be */
/* useful, but WITHOUT ANY WARRANTY; without even the implied warranty of */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU */
/* General Public License for more details. */

/* You should have received a copy of the GNU General Public License */
/* along with FFvideo Live Wallpaper.  If not, see <http://www.gnu.org/licenses/>. */

#include <GLES/gl.h>
#include <GLES/glext.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>

#include <jni.h>  
#include <string.h>  
#include <stdio.h>
#include <android/log.h>  

//ffmpeg video variables
int      initializedVideo=0;
int      initializedFrame=0;
AVFormatContext *pFormatCtx=NULL;
int             videoStream;
AVCodecContext  *pCodecCtx=NULL;
AVCodec         *pCodec=NULL;
AVFrame         *pFrame=NULL;
AVPacket        packet;
int             frameFinished;
float           aspect_ratio;

//ffmpeg video conversion variables
AVFrame         *pFrameConverted=NULL;
int             numBytes;
uint8_t         *bufferConverted=NULL;

//opengl
int textureFormat=PIX_FMT_RGBA;//PIX_FMT_RGB24;
int textureWidth=256;
int textureHeight=256;
int nTextureHeight=-256;
int textureL=0, textureR=0, textureW=0;
//GLuint textureConverted=0;
GLuint texturesConverted[2] = { 0,1 };
static int len=0;

//screen dimensions
int screenWidth = 50;
int screenHeight= 50;
int screenL=0, screenR=0, screenW=0;
int dPaddingX=0,dPaddingY=0;
int drawWidth=50,drawHeight=50;

//wallpaper
int wallWidth = 50;
int wallHeight = 50;
int xOffSet, yOffSet;
int xStep, yStep;
jboolean spanVideo = JNI_TRUE;

//video dimensions
int wallVideoWidth = 0;
int wallVideoHeight = 0;
int marginX, marginY;
jboolean isScreenPortrait = JNI_TRUE;
jboolean isPreview = JNI_TRUE;
jboolean loopVideo = JNI_TRUE;
jboolean isGetFrame = JNI_TRUE;

//file
const char * szFileName;

#define max( a, b ) ( ((a) > (b)) ? (a) : (b) )
#define min( a, b ) ( ((a) < (b)) ? (a) : (b) )

//test variables
#define RGBA8(r, g, b)  (((r) << (24)) | ((g) << (16)) | ((b) << (8)) | 255)
int sPixelsInited=JNI_FALSE;
uint32_t *s_pixels=NULL;
int s_pixels_size() { 
  return (sizeof(uint32_t) * textureWidth * textureHeight * 5); 
}
void render_pixels1(uint32_t *pixels, uint32_t c) {
	int x, y;
	/* fill in a square of 5 x 5 at s_x, s_y */
	for (y = 0; y < textureHeight; y++) {
		for (x = 0; x < textureWidth; x++) {
			int idx = x + y * textureWidth;
			pixels[idx++] = RGBA8(255, 255, 0);
		}
	}
}
void render_pixels2(uint32_t *pixels, uint32_t c) {
	int x, y;
	/* fill in a square of 5 x 5 at s_x, s_y */
	for (y = 0; y < textureHeight; y++) {
		for (x = 0; x < textureWidth; x++) {
			int idx = x + y * textureWidth;
			pixels[idx++] = RGBA8(0, 0, 255);
		}
	}
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_initVideo
(JNIEnv * env, jobject this)  {
  initializedVideo = 0;
  initializedFrame = 0;
}

/* list of things that get loaded: */
/* buffer */
/* pFrameConverted */
/* pFrame */
/* pCodecCtx */
/* pFormatCtx */
void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_loadVideo
(JNIEnv * env, jobject this, jstring fileName)  {
  jboolean isCopy;  
  szFileName = (*env)->GetStringUTFChars(env, fileName, &isCopy);  
  //debug
  __android_log_print(ANDROID_LOG_DEBUG, "NDK: ", "NDK:LC: [%s]", szFileName); 
  // Register all formats and codecs
  av_register_all();
  // Open video file
  if(av_open_input_file(&pFormatCtx, szFileName, NULL, 0, NULL)!=0) {
    __android_log_print(ANDROID_LOG_DEBUG, 
			"video.c", 
			"NDK: Couldn't open file");
    return;
  }
  __android_log_print(ANDROID_LOG_DEBUG, 
		      "video.c", 
		      "NDK: Succesfully loaded file");
  // Retrieve stream information */
  if(av_find_stream_info(pFormatCtx)<0) {
    __android_log_print(ANDROID_LOG_DEBUG, 
			"video.c", 
			"NDK: Couldn't find stream information");
    return;
  }
  __android_log_print(ANDROID_LOG_DEBUG, 
		      "video.c", 
		      "NDK: Found stream info");
  // Find the first video stream
  videoStream=-1;
  int i;
  for(i=0; i<pFormatCtx->nb_streams; i++)
    if(pFormatCtx->streams[i]->codec->codec_type==CODEC_TYPE_VIDEO) {
      videoStream=i;
      break;
    }
  if(videoStream==-1) {
    __android_log_print(ANDROID_LOG_DEBUG, 
			"video.c", 
			"NDK: Didn't find a video stream");
    return;
  }
  __android_log_print(ANDROID_LOG_DEBUG, 
		      "video.c", 
		      "NDK: Found video stream");
  // Get a pointer to the codec contetx for the video stream
  pCodecCtx=pFormatCtx->streams[videoStream]->codec;
  // Find the decoder for the video stream
  pCodec=avcodec_find_decoder(pCodecCtx->codec_id);
  if(pCodec==NULL) {
    __android_log_print(ANDROID_LOG_DEBUG, 
			"video.c", 
			"NDK: Unsupported codec");
    return;
  }
  // Open codec
  if(avcodec_open(pCodecCtx, pCodec)<0) {
    __android_log_print(ANDROID_LOG_DEBUG, 
			"video.c", 
			"NDK: Could not open codec");
    return;
  }
  // Allocate video frame (decoded pre-conversion frame)
  pFrame=avcodec_alloc_frame();
  // keep track of initialization
  initializedVideo = 1;
  __android_log_print(ANDROID_LOG_DEBUG, 
		      "video.c", 
		      "NDK: Finished loading video");
}

//for this to work, you need to set the scaled video dimensions first
void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_prepareStorageFrame
(JNIEnv * env, jobject this)  {
  // Allocate an AVFrame structure
  pFrameConverted=avcodec_alloc_frame();
  // Determine required buffer size and allocate buffer
  numBytes=avpicture_get_size(textureFormat, 
			      textureWidth,
			      textureHeight);
  bufferConverted=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));
  if ( pFrameConverted == NULL || bufferConverted == NULL ) 
    __android_log_print(ANDROID_LOG_DEBUG, 
			"prepareStorage>>>>", 
			"Out of memory");
  // Assign appropriate parts of buffer to image planes in pFrameRGB
  // Note that pFrameRGB is an AVFrame, but AVFrame is a superset
  // of AVPicture
  avpicture_fill((AVPicture *)pFrameConverted, 
		 bufferConverted, 
		 textureFormat,
		 textureWidth, 
		 textureHeight);
  __android_log_print(ANDROID_LOG_DEBUG, "prepareStorage>>>>", "Created frame");
  __android_log_print(ANDROID_LOG_DEBUG,
  		      "prepareStorage>>>>",
  		      "texture dimensions: %dx%d", 
		      textureWidth, textureHeight
  		      );
    initializedFrame = 1;
}

jint Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_getVideoWidth
(JNIEnv * env, jobject this)  {
  return pCodecCtx->width;
}

jint Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_getVideoHeight
(JNIEnv * env, jobject this)  {
  return pCodecCtx->height;
}
 
void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_getFrame
(JNIEnv * env, jobject this)  {
  // keep reading packets until we hit the end or find a video packet
  while(av_read_frame(pFormatCtx, &packet)>=0) {
    static struct SwsContext *img_convert_ctx;
    // Is this a packet from the video stream?
    if(packet.stream_index==videoStream) {
      // Decode video frame
      /* __android_log_print(ANDROID_LOG_DEBUG,  */
      /* 			  "video.c",  */
      /* 			  "getFrame: Try to decode frame" */
      /* 			  ); */
      avcodec_decode_video(pCodecCtx, 
			   pFrame, 
			   &frameFinished, 
			   packet.data, 
			   packet.size);
      // Did we get a video frame?
      if(frameFinished) {
	if(img_convert_ctx == NULL) {
	  /* get/set the scaling context */
	  int w = pCodecCtx->width;
	  int h = pCodecCtx->height;
	  img_convert_ctx = 
	    sws_getContext(
			   w, h, //source
			   pCodecCtx->pix_fmt,
			   textureWidth,textureHeight,
			   //w, h, //destination
			   textureFormat, 
			   //SWS_BICUBIC,
			   //SWS_POINT,
			   //SWS_X,
			   //SWS_CPU_CAPS_MMX2,
			   SWS_FAST_BILINEAR,
			   NULL, NULL, NULL
			   );
	  if(img_convert_ctx == NULL) {
	    /* __android_log_print(ANDROID_LOG_DEBUG,  */
	    /* 			"video.c",  */
	    /* 			"NDK: Cannot initialize the conversion context!" */
	    /* 			); */
	    return;
	  }
	} /* if img convert null */
	/* finally scale the image */
	/* __android_log_print(ANDROID_LOG_DEBUG,  */
	/* 			"video.c",  */
	/* 			"getFrame: Try to scale the image" */
	/* 			); */
	sws_scale(img_convert_ctx,
		  pFrame->data,
		  pFrame->linesize, 
		  0, pCodecCtx->height,
		  pFrameConverted->data, 
		  pFrameConverted->linesize);
	/* do something with pFrameConverted */
	/* ... see drawFrame() */
	/* We found a video frame, did something with it, no free up
	   packet and return */
	av_free_packet(&packet);
	return;
      } /* if frame finished */
    } /* if packet video stream */ 
    // Free the packet that was allocated by av_read_frame
    av_free_packet(&packet);
  } /* while */
  //reload video when you get to the end
  av_seek_frame(pFormatCtx,videoStream,0,AVSEEK_FLAG_ANY);
}
  
void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setLoopVideo
(JNIEnv * env, jobject this, jboolean b) {
  loopVideo = b;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_closeVideo
(JNIEnv * env, jobject this) {  
  if ( initializedFrame == 1 ) {
    // Free the converted image
    av_free(bufferConverted); 
    av_free(pFrameConverted); 
    initializedFrame = 0;
  __android_log_print(ANDROID_LOG_DEBUG, "closeVideo>>>>", "Freed converted image");
  }
  if ( initializedVideo == 1 ) {
    /* // Free the YUV frame */
    av_free(pFrame); 
    /* // Close the codec */
    avcodec_close(pCodecCtx); 
    // Close the video file
    av_close_input_file(pFormatCtx); 
    initializedVideo = 0;
  __android_log_print(ANDROID_LOG_DEBUG, "closeVideo>>>>", "Freed video structures");
  }
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_freeVideo
(JNIEnv * env, jobject this) {  
  if ( initializedVideo == 1 ) {
    /* // Free the YUV frame */
    av_free(pFrame); 
    /* // Close the codec */
    avcodec_close(pCodecCtx); 
    // Close the video file
    av_close_input_file(pFormatCtx); 
  __android_log_print(ANDROID_LOG_DEBUG, "closeVideo>>>>", "Freed video structures");
    initializedVideo = 0;
  }
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_freeConversionStorage
(JNIEnv * env, jobject this) {  
  if ( initializedFrame == 1 ) {
    // Free the converted image
    av_free(bufferConverted); 
    av_freep(pFrameConverted); 
    initializedFrame = 0;
    }
}

/*--- END OF VIDEO ----*/

/* disable these capabilities. */
static GLuint s_disable_options[] = {
	GL_FOG,
	GL_LIGHTING,
	GL_CULL_FACE,
	GL_ALPHA_TEST,
	GL_BLEND,
	GL_COLOR_LOGIC_OP,
	GL_DITHER,
	GL_STENCIL_TEST,
	GL_DEPTH_TEST,
	GL_COLOR_MATERIAL,
	0
};

// For stuff that opengl needs to work with,
// like the bitmap containing the texture
void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_initPreOpenGL
(JNIEnv * env, jobject this)  { }

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_initOpenGL
(JNIEnv * env, jobject this)  {
  /* s_pixels = malloc(s_pixels_size()); */
  /* if ( isPreview == JNI_TRUE )  */
  /*   render_pixels1(s_pixels, ( isPreview == JNI_TRUE ) ? 255 : 0 ); */
  /* else */
  /*   render_pixels2(s_pixels, ( isPreview == JNI_TRUE ) ? 255 : 0 ); */
  //memset(s_pixels, 255, s_pixels_size());
  __android_log_print(ANDROID_LOG_DEBUG,
		      "NDK:",
		      "initOpenGL()"
		      );
  __android_log_print(ANDROID_LOG_DEBUG,
  		      "NDK initOpenGL()",
  		      "texture dimensions: [%d]x[%d]", 
		      textureWidth, textureHeight
  		      );
  //This is actually the oncreate code
  glDeleteTextures(1, &texturesConverted[len]); 
  //Disable stuff
  __android_log_print(ANDROID_LOG_DEBUG,
  		      "NDK initOpenGL()",
  		      "disabling some opengl options"
  		      );
  GLuint *start = s_disable_options;
  while (*start) glDisable(*start++);
  //setup textures
  __android_log_print(ANDROID_LOG_DEBUG,
		      "NDK initOpenGL()",
		      "enabling and generating textures"
		      );
  glEnable(GL_TEXTURE_2D);
  len = ( len == 0 ) ? 1 : 0;
  glGenTextures(1, &texturesConverted[len]);
  glBindTexture(GL_TEXTURE_2D,texturesConverted[len]);
  //...and bind it to our array
  __android_log_print(ANDROID_LOG_DEBUG,
		      "NDK initOpenGL()",
		      "binded texture"
		      );
  //Create Nearest Filtered Texture
  /* if ( spanVideo == JNI_TRUE ) { */
    glTexParameterf(GL_TEXTURE_2D,
  		    GL_TEXTURE_MIN_FILTER,
  		    GL_LINEAR);
    glTexParameterf(GL_TEXTURE_2D,
  		    GL_TEXTURE_MAG_FILTER,
  		    GL_LINEAR);
  /* } */
  /* else { */
    /* glTexParameterf(GL_TEXTURE_2D,  */
    /* 		    GL_TEXTURE_MIN_FILTER,  */
    /* 		    GL_NEAREST); */
    /* glTexParameterf(GL_TEXTURE_2D,  */
    /* 		    GL_TEXTURE_MAG_FILTER,  */
    /* 		    GL_NEAREST); */
    //}
  //Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
  glTexParameterf(GL_TEXTURE_2D, 
		  GL_TEXTURE_WRAP_S, 
		  GL_CLAMP_TO_EDGE);
  //GL_REPEAT);
  glTexParameterf(GL_TEXTURE_2D, 
		  GL_TEXTURE_WRAP_T, 
		  GL_CLAMP_TO_EDGE);
  //GL_REPEAT);
  glTexImage2D(GL_TEXTURE_2D,		/* target */
	       0,			/* level */
	       GL_RGBA,			/* internal format */
	       textureWidth,		/* width */
	       textureHeight,		/* height */
	       0,			/* border */
	       GL_RGBA,			/* format */
	       GL_UNSIGNED_BYTE,/* type */
	       NULL);
  //setup simple shading
  glShadeModel(GL_FLAT);
  //check_gl_error("glShadeModel");
  glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
}

int screenR,videoR;

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_updateVideoPosition
(JNIEnv * env, jobject this)  {
  if ( spanVideo == JNI_FALSE || isPreview == JNI_TRUE ) return;
  screenR = xOffSet + xStep;
  videoR = marginX + wallVideoWidth;
  if ( ( screenR > marginX ) &&
       ( xOffSet < videoR ) ) {
    //find % of video to display
    int left = max(xOffSet,marginX);
    int right = min(screenR,videoR);
    textureL = ((float) (left - marginX) / (float) wallVideoWidth) 
      * textureWidth ;
    textureR = ((float) (right - marginX) / (float) wallVideoWidth)
      * textureWidth;
    textureW = textureR - textureL;
    //find % of screen to draw
    screenL = left - xOffSet;
    screenR = right - xOffSet;
    screenW = screenR - screenL;
  }
  else {
    textureL = 0;
    textureW = 0;
    screenL = 0;
    screenW = 0;
  }
  /* __android_log_print(ANDROID_LOG_DEBUG, "NDK: ", "video dims: %dx%d", wallVideoWidth,wallVideoHeight);  */
  /* __android_log_print(ANDROID_LOG_DEBUG, "NDK: ", "texture dims: [%d,%d) %dx%d", textureL,textureW,textureWidth,textureHeight);  */
  /* __android_log_print(ANDROID_LOG_DEBUG, "NDK: ", "screen dims: [%d,%d)", screenL,screenW);  */
  /* __android_log_print(ANDROID_LOG_DEBUG, "NDK: ", "right margin: %d", marginX);  */
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_drawFrame
(JNIEnv * env, jobject this)  {
  glClear(GL_COLOR_BUFFER_BIT);
  glBindTexture(GL_TEXTURE_2D,texturesConverted[len]);
  //__android_log_print(ANDROID_LOG_DEBUG, "NDK: ", "spanVideo: %d, isPreview: %d", spanVideo, isPreview); 
  if ( spanVideo == JNI_TRUE && isPreview == JNI_FALSE ) {
    //set the texture crop rectangle which determines the part of the
    //texture mapped to the drawn rectangle. Notice how it gets inverted
    int rect[4] = {textureL, textureHeight, textureW, nTextureHeight};
    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, rect);
    //controls the part of the texture that will be updated
    //Reference: 
    //http://old.siggraph.org/publications/2006cn/course16/KhronosSpecs/gl_egl_ref_1.1.20041110/glTexSubImage2D.html
    glTexSubImage2D(GL_TEXTURE_2D, /* target */
		    0,		/* level */
		    0,	/* xoffset */
		    0,	/* yoffset */
		    textureWidth,
		    textureHeight,
		    GL_RGBA,	/* format */
		    GL_UNSIGNED_BYTE, /* type */
		    pFrameConverted->data[0]);
    //check_gl_error("glTexImage2D");
    //Reference:
    //http://old.siggraph.org/publications/2006cn/course16/KhronosSpecs/gl_egl_ref_1.1.20041110/glDrawTex.html
    glDrawTexiOES(screenL, 0, 0, screenW, screenHeight);
    //glDrawTexiOES(dPaddingX, dPaddingY, 0, drawWidth, drawHeight);
    //check_gl_error("glDrawTexiOES");
  }
  else {
    int rect[4] = {0, textureHeight, textureWidth, nTextureHeight};
    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, rect);
    glTexSubImage2D(GL_TEXTURE_2D, /* target */
    		    0,		/* level */
    		    0,	/* xoffset */
    		    0,	/* yoffset */
    		    textureWidth,
    		    textureHeight,
    		    GL_RGBA,	/* format */
    		    GL_UNSIGNED_BYTE, /* type */
    		    //s_pixels);
		    pFrameConverted->data[0]);
    glDrawTexiOES(dPaddingX, dPaddingY, 0, drawWidth, drawHeight);
  }
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_closeOpenGL
(JNIEnv *env, jobject this) {
  glDeleteTextures(1, &texturesConverted[len]); 
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_closePostOpenGL
(JNIEnv *env, jobject this) {
  //free(s_pixels);
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setScreenDimensions
(JNIEnv *env, jobject this, jint w, jint h) {
  screenWidth = w;
  screenHeight = h;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setTextureDimensions
(JNIEnv *env, jobject this, jint px, jint py) {
  textureWidth = px;
  textureHeight = py;
  nTextureHeight = -1*py;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setWallVideoDimensions
(JNIEnv *env, jobject this, jint w, jint h) {
  wallVideoWidth = w;
  wallVideoHeight = h;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setWallDimensions
(JNIEnv *env, jobject this, jint w, jint h) {
  wallWidth = w;
  wallHeight = h;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setVideoMargins
(JNIEnv *env, jobject this, jint w, jint h) {
  marginX = w;
  marginY = h;
}
  
void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setOffsets
(JNIEnv *env, jobject this, jint x, jint y) {
  xOffSet = x;
  yOffSet = y;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setSteps
(JNIEnv *env, jobject this, jint xs, jint ys) {
  xStep = xs;
  yStep = ys;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setSpanVideo
(JNIEnv *env, jobject this, jboolean b) {
  spanVideo = b;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setDrawDimensions
(JNIEnv *env, jobject this, jint w, jint h) {
  drawWidth = w;
  drawHeight = h;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setScreenPadding
(JNIEnv *env, jobject this, jint w, jint h) {
  dPaddingX = w;
  dPaddingY = h;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setOrientation
(JNIEnv *env, jobject this, jboolean b) {
  isScreenPortrait = b;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_setPreviewMode
(JNIEnv *env, jobject this, jboolean b) {
  isPreview = b;
}

void Java_ffvideolivewallpaper_frankandrobot_com_NativeCalls_toggleGetFrame
(JNIEnv *env, jobject this, jboolean b) {
  isGetFrame = b;
}
