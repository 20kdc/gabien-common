#!/bin/sh
# gabien-common - Cross-platform game and UI framework
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

# Ensures gabien-common is ready for use in a release.
# Be sure to set GABIEN_NATIVES_DEV=1 to unlock the natives test for dev releases.

micromvn/umvn test -q &&
micromvn/umvn package-only -q &&
micromvn/umvn install-only -q
