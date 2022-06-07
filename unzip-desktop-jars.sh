#!/bin/sh
# gabien-common - Cross-platform game and UI framework
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

# Utility script to unzip the gabien-common JARs involved in a desktop application.

if [ "$#" -ne 1 ]; then
 echo "unzip-desktop-jars TARGET"
 exit 1
fi

unzip -o uslx/target/gabien-uslx-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o common/target/gabien-common-0.666-SNAPSHOT.jar -d "$1" &&
unzip -o javase/target/gabien-javase-0.666-SNAPSHOT.jar -d "$1"

