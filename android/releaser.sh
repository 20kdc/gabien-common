#!/bin/sh
# gabien-android - gabien backend for Android
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

# Just a boring releaser helper script.
# name, package, version-name, version-code, app-jar, icon, permissions

# Expects ANDROID_JAR_D8, ANDROID_JAR_AAPT and ANDROID_BT as environment variables
# Values should be something like:
# export ANDROID_BT=/home/20kdc/Android/Sdk/build-tools/30.0.3
# export ANDROID_JAR_D8=/home/20kdc/Android/Sdk/platforms/android-7/android.jar
# export ANDROID_JAR_AAPT=/home/20kdc/Android/Sdk/platforms/android-25/android.jar
# In a perfect world, these would all be SDK-relative paths.
# But Android keeps shuffling around stuff, so what works at one point may not work if, say, android-7/android.jar is dropped.

cp "$6" res/drawable/icon.png
lua compile-manifest.lua "$2" "$3" "$4" "$7" > AndroidManifest.xml
echo "<resources><string name=\"app_name\">$1</string></resources>" > res/values/strings.xml || exit

mkdir -p staging staging2 || exit
rm -rf staging staging2 || exit
mkdir -p staging staging2 || exit

# Extract JAR contents to staging directory
unzip -q -o $5 -d staging || exit
# Merge in everything, run d8
$ANDROID_BT/d8 --release --lib $ANDROID_JAR_D8 --output staging2 `find staging | grep '\.class$'` || exit
$ANDROID_BT/aapt p -f -I $ANDROID_JAR_AAPT -M AndroidManifest.xml -S res -A staging/assets -F result.apk || exit
cd staging2 || exit
$ANDROID_BT/aapt a ../result.apk classes.dex || exit
# Obviously, I'll move this stuff into a config file or something if I ever release to the real Play Store - and will change my keystore
# For making debug keys that'll probably live longer than me:
# keytool -genkeypair -keyalg RSA -validity 36500
# Need to override jarsigner breaking things for no reason
export JAVA_TOOL_OPTIONS="-Djava.security.properties=../java.security"
stripzip ../result.apk 1> /dev/null 2> /dev/null
jarsigner -sigalg SHA1withRSA -digestalg SHA1 -storepass "android" -sigFile CERT ../result.apk mykey || exit
echo "Okay"
