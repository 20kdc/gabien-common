@echo off
rem gabien-common - Cross-platform game and UI framework
rem Written starting in 2016 by contributors (see CREDITS.txt)
rem To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
rem A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

set GABIEN_HOME=%~dp0..

if defined JAVA_1_8_HOME goto has_jdk8

echo JAVA_1_8_HOME was not defined.
echo Please set it using, for example: setx JAVA_1_8_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.442.6-hotspot

:has_jdk8

set JAVA_HOME=%JAVA_1_8_HOME%
set PATH=%JAVA_HOME%\bin;%GABIEN_HOME%\bin;%PATH%
