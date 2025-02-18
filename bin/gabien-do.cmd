@echo off
rem gabien-common - Cross-platform game and UI framework
rem Written starting in 2016 by contributors (see CREDITS.txt)
rem To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
rem A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

java -cp "%GABIEN_HOME%\micromvn" umvn -f "%GABIEN_HOME%\build-script" install
if %errorlevel% neq 0 exit /b

java -cp "%GABIEN_HOME%\micromvn" umvn -f build-script compile
if %errorlevel% neq 0 exit /b

java -cp "%GABIEN_HOME%\micromvn" umvn -f build-script umvn-run gabien.builder.Main %*
