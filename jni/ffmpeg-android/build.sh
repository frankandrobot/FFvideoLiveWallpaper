#!/bin/bash

if [ "$NDK" = "" ]; then
	echo NDK variable not set, assuming ${HOME}/android-ndk
	export NDK=${HOME}/android-ndk
fi

SYSROOT=$NDK/platforms/android-3/arch-arm
# Expand the prebuilt/* path into the correct one
TOOLCHAIN=`echo $NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/*-x86`
export PATH=$TOOLCHAIN/bin:$PATH

rm -rf build/ffmpeg
mkdir -p build/ffmpeg
cd ffmpeg

# Don't build any neon version for now
for version in armv5te armv7a
do

	DEST=../build/ffmpeg
	FLAGS="--target-os=linux --arch=arm"
	FLAGS="$FLAGS --cross-prefix=arm-linux-androideabi- --arch=arm"
	FLAGS="$FLAGS --sysroot=$SYSROOT"
	FLAGS="$FLAGS --soname-prefix=/data/data/ffvideolivewallpaper.frankandrobot.com/lib/"
	#FLAGS="$FLAGS --enable-version3 --enable-gpl"
	#FLAGS="$FLAGS --enable-version3 --enable-gpl --enable-nonfree"
	FLAGS="$FLAGS --disable-ffmpeg --disable-ffplay"
	FLAGS="$FLAGS --disable-ffserver --disable-ffprobe --disable-encoders"
	FLAGS="$FLAGS --disable-muxers --disable-devices --disable-protocols"
	FLAGS="$FLAGS --enable-protocol=file --enable-avfilter"
	FLAGS="$FLAGS --disable-network"
	FLAGS="$FLAGS --disable-avdevice --disable-asm"
	FLAGS="$FLAGS --enable-shared --disable-symver"
	FLAGS="$FLAGS --enable-small"
	FLAGS="$FLAGS --disable-doc"
	FLAGS="$FLAGS --optimization-flags=-O2"

	case "$version" in
		neon)
			EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=neon"
			EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
			# Runtime choosing neon vs non-neon requires
			# renamed files
			ABI="armeabi-v7a"
			;;
		armv7a)
			EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp"
			EXTRA_LDFLAGS=""
			ABI="armeabi-v7a"
			;;
		*)
			EXTRA_CFLAGS=""
			EXTRA_LDFLAGS=""
			ABI="armeabi"
			;;
	esac
	DEST="$DEST/$ABI"
	FLAGS="$FLAGS --prefix=$DEST"

	mkdir -p $DEST
	echo $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" > $DEST/info.txt
	./configure $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" | tee $DEST/configuration.txt
	[ $PIPESTATUS == 0 ] || exit 1
	make clean
	make -j4 || exit 1
	make install || exit 1

done

