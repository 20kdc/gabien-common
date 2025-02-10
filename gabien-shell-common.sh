#!/bin/sh
# gabien-common - Cross-platform game and UI framework
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

# Shell script common functions.

if [ ! -e "$JAVA_HOME_GABIEN" ]; then
 echo "JAVA_HOME_GABIEN (JDK for use with GaBIEn) not set or doesn't exist."
 echo "Value: $JAVA_HOME_GABIEN"
 echo "Example: /usr/lib/jvm/java-17-openjdk-amd64"
 exit 1
fi

if [ "$GABIEN_SHELL_COMMON_DID_JHOOK" = "" ]; then
 export JAVA_HOME="$JAVA_HOME_GABIEN"
 export PATH="$JAVA_HOME_GABIEN/bin:$PATH"
 export GABIEN_SHELL_COMMON_DID_JHOOK=1
fi
