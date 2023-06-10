#!/bin/sh
# gabien-android - gabien backend for Android
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

# Just a boring releaser helper script.
# name, package, version-name, version-code, app-staging, icon, permissions

# Expects ANDROID_JAR and ANDROID_BT as environment variables

cp "$6" res/drawable/icon.png
lua compile-manifest.lua "$2" "$3" "$4" "$7" > AndroidManifest.xml
echo "<resources><string name=\"app_name\">$1</string></resources>" > res/values/strings.xml &&

mkdir -p staging staging2 &&
rm -rf staging staging2 &&
mkdir -p staging staging2 &&

# Extract JAR contents to staging directory
cd staging &&
unzip -o ../target/gabien-android-0.666-SNAPSHOT.jar &&
cd .. &&
# Merge in everything, run d8
cp -r "${5:-/dev/null}"/* staging/ &&
$ANDROID_BT/d8 --release --lib $ANDROID_JAR --output staging2 `find staging | grep '\.class$'` &&
$ANDROID_BT/aapt p -f -I $ANDROID_JAR -M AndroidManifest.xml -S res -A staging/assets -F result.apk &&
cd staging2 &&
$ANDROID_BT/aapt a ../result.apk classes.dex &&
# Obviously, I'll move this stuff into a config file or something if I ever release to the real Play Store - and will change my keystore
# For making debug keys that'll probably live longer than me:
# keytool -genkeypair -keyalg RSA -validity 36500
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -storepass "android" -sigFile CERT ../result.apk mykey &&
echo "Okay"
