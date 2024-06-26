-- gabien-android - gabien backend for Android
-- Written starting in 2016 by contributors (see CREDITS.txt)
-- To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
-- A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
local package, versionName, versionCode, permissions = ...
print("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"" .. package .. "\"")
print(" android:installLocation=\"auto\"")
print(" android:versionCode=\"" .. versionCode .. "\"")
print(" android:versionName=\"" .. versionName .. "\">")
print(" <uses-sdk android:minSdkVersion=\"7\" android:targetSdkVersion=\"23\" />")
for p in permissions:gmatch("[^,]+") do
 print(" <uses-permission android:name=\"" .. p .. "\"/>")
end
print(" <application")
print("  android:hardwareAccelerated=\"true\"")
-- Sadly, Android takes permission to debug to mean SLOW THIS DOWN.
-- Not that it's documented in the manifest documentation, noooo.
-- You have to go to JNI Tips, where it will happily THEN tell you that enabling this activates CheckJNI.
-- What insanity.
--print("  android:debuggable=\"true\"")
print("  android:icon=\"@drawable/icon\"")
print("  android:label=\"@string/app_name\"")
print("  android:theme=\"@style/AppTheme\">")
print("  <activity android:name=\"gabien.MainActivity\" android:immersive=\"true\">")
print("   <intent-filter>")
print("    <action android:name=\"android.intent.action.MAIN\"/>")
print("    <category android:name=\"android.intent.category.LAUNCHER\"/>")
print("   </intent-filter>")
print("  </activity>")
print(" </application>")
print("</manifest>")

