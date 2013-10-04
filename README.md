# Compiling FFMPEG

I included pre-compiled binaries with this project but just in case
you need to compile your own, here are the instructions:

1. Download NDK 5. It's now archived. As of 10/2013, this link works:
 - http://dl.google.com/android/ndk/android-ndk-r5-linux-x86.tar.bz2
 - I'm not sure where to find versions for Windows or Mac
 - Make sure that you install it to `$HOME/android-ndk`.
2. Download a version of FFmpeg that works with this project. 
   We're using [bambuser's](http://bambuser.com/opensource). Download
   `client version 1.3.7 to 1.3.10`. 
3. Extract the archive.
4. Copy `extract.sh` and the `ffmpeg-*.tar.gz` file into the 
   `JNI/ffmpeg-android` folder. 
4. Run `extract.sh`
5. Change `build.sh` to your liking. Hint: run `configure --help` from 
   `ffmpeg` to find options to enable or disable.
 - Make sure to enable the `file protocol`. You need that to open files on the phone.
 - Enabling the non-free codecs gives more supported formats but it may not be compatible for the license of your project.
6. Rerun `build.sh`.
7. Run `clean-build.sh`.

See also: http://ikaruga2.wordpress.com/2011/06/15/video-live-wallpaper-part-1/