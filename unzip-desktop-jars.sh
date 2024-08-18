#!/bin/sh
# gabien-common - Cross-platform game and UI framework
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

# Utility script to unzip the gabien-common JARs involved in a desktop application.

if [ "$#" -ne 1 ]; then
 echo "unzip-desktop-jars TARGET"
 exit 1
fi

unzip -o uslx/target/gabien-uslx-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o ../datum/java/target/gabien-datum-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o media/target/gabien-media-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o common/target/gabien-common-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o ui/target/gabien-ui-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o natives-sdk/natives.jar -d "$1" &&
unzip -o natives-util/target/natives-util-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o javase/target/gabien-javase-0.666-SNAPSHOT.jar -d "$1"

