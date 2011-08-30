#include $(all-subdir-makefiles)

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
MY_LIB_PATH := ffmpeg-android/build/ffmpeg/armeabi/lib
LOCAL_MODULE := bambuser-libavcore
LOCAL_SRC_FILES := $(MY_LIB_PATH)/libavcore.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := bambuser-libavformat
LOCAL_SRC_FILES := $(MY_LIB_PATH)/libavformat.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := bambuser-libavcodec
LOCAL_SRC_FILES := $(MY_LIB_PATH)/libavcodec.so
include $(PREBUILT_SHARED_LIBRARY)

# include $(CLEAR_VARS)
# LOCAL_MODULE := bambuser-libavdevice
# LOCAL_SRC_FILES := $(MY_LIB_PATH)/libavdevice.so
# include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := bambuser-libavfilter
LOCAL_SRC_FILES := $(MY_LIB_PATH)/libavfilter.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := bambuser-libavutil
LOCAL_SRC_FILES := $(MY_LIB_PATH)/libavutil.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := bambuser-libswscale
LOCAL_SRC_FILES := $(MY_LIB_PATH)/libswscale.so
include $(PREBUILT_SHARED_LIBRARY)


#local_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS := -DANDROID_NDK \
                -DDISABLE_IMPORTGL

LOCAL_MODULE    := video
LOCAL_SRC_FILES := video.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/ffmpeg-android/ffmpeg 
LOCAL_LDLIBS := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib -L$(LOCAL_PATH) -L$(LOCAL_PATH)/ffmpeg-android/build/ffmpeg/armeabi/lib/ -lGLESv1_CM -ldl -lavformat -lavcodec -lavfilter -lavutil -lswscale -llog -lz -lm

include $(BUILD_SHARED_LIBRARY)
