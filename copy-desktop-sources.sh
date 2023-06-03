#!/bin/sh
# gabien-common - Cross-platform game and UI framework
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

# Utility script to copy the gabien-common sources involved in a desktop application.
# Doesn't copy licensing files, make sure to do that!

if [ "$#" -ne 1 ]; then
 echo "unzip-desktop-jars TARGET"
 exit 1
fi

cp -r uslx/src/main/* $1 &&
cp -r datum/src/main/* $1 &&
cp -r media/src/main/* $1 &&
cp -r common/src/main/* $1 &&
cp -r natives/src/main/java $1 &&
mkdir -p $1/c &&
cp natives/c/*.c natives/c/*.h natives/c/badgpu/*.c natives/c/badgpu/*.h $1/c &&
cp -r javase/src/main/* $1

