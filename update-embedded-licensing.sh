#!/bin/sh
# gabien-common - Cross-platform game and UI framework
# Written starting in 2016 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

# Updates gabien license data embedded into USLX.

cp COPYING.txt uslx/src/main/resources/assets/gabien/licensing/common/COPYING.txt &&
cp thirdparty/stb_vorbis_modified/COPYING.txt natives-util/src/main/resources/assets/gabien/licensing/stb_vorbis/COPYING.txt &&
cp thirdparty/stb_vorbis_modified/CREDITS.txt natives-util/src/main/resources/assets/gabien/licensing/stb_vorbis/CREDITS.txt &&
cp CREDITS.txt uslx/src/main/resources/assets/gabien/licensing/common/CREDITS.txt

