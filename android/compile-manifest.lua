-- gabien-android - gabien backend for Android
-- Written starting in 2016 by contributors (see CREDITS.txt)
-- To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
-- You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
local package, versionName, versionCode, permissions = ...
print("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"" .. package .. "\"")
print(" android:versionCode=\"" .. versionCode .. "\"")
print(" android:versionName=\"" .. versionName .. "\">")
print(" <uses-sdk android:minSdkVersion=\"7\" android:targetSdkVersion=\"10\" />")
for p in permissions:gmatch("[^,]+") do
 print(" <uses-permission android:name=\"" .. p .. "\"/>")
end
print(" <application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\" android:theme=\"@style/AppTheme\">")
print("  <activity android:name=\"gabien.MainActivity\" android:immersive=\"true\">")
print("   <intent-filter>")
print("    <action android:name=\"android.intent.action.MAIN\"/>")
print("    <category android:name=\"android.intent.category.LAUNCHER\"/>")
print("   </intent-filter>")
print("  </activity>")
print(" </application>")
print("</manifest>")

