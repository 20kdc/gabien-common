#!/bin/sh
# gabien-common - Cross-platform game and UI framework
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

# Utility script to copy the gabien-common sources involved in a desktop application.
# Doesn't copy licensing files, make sure to do that!

if [ "$#" -ne 1 ]; then
 echo "unzip-desktop-jars TARGET"
 exit 1
fi

cp -r common/src/main/* $1 &&
cp -r javase/src/main/* $1 &&
cp -r uslx/src/main/* $1

