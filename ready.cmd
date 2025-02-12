@echo off
rem gabien-common - Cross-platform game and UI framework
rem Written starting in 2016 by contributors (see CREDITS.txt)
rem To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
rem A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

cd "%~dp0\"
java -cp micromvn umvn test -q
if %errorlevel% neq 0 exit /b %errorlevel%
java -cp micromvn umvn package-only -q
if %errorlevel% neq 0 exit /b %errorlevel%
java -cp micromvn umvn install-only -q
if %errorlevel% neq 0 exit /b %errorlevel%
