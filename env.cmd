@echo off
rem gabien-common - Cross-platform game and UI framework
rem Written starting in 2016 by contributors (see CREDITS.txt)
rem To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
rem A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

rem This file is used to swap the Wine JDK variable into the main JDK variable and start a prompt.

if defined JAVA_1_8_HOME_W goto has_wine_override

goto anyway

:has_wine_override

set JAVA_1_8_HOME=%JAVA_1_8_HOME_W%

goto anyway

:anyway

call bin\activate.cmd

cmd
