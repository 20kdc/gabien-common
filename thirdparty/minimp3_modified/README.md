# minimp3 modified

This will at some point contain a modified copy of https://github.com/lieff/minimp3/ !

Specific planned modifications are:

* Support for GaBIEn's kinda-cursed cross-compilation system
* Whatever I have to do to make it capable of measuring a stream without fully decoding it
* Stripping out anything I don't need (particularly stdio will receive this treatment)

However, this hasn't been started *yet.* Terrible bout of laziness, I know.

*Current state: minimp3.h has been copied into the repository but has not yet undergone any of these modifications.*

Things that need doing:

* Removal of s16 output support
* Remove `MINIMP3_ONLY_MP3`
* Getting it to actually build as part of the gabien-natives build cycle (may require SIMD be removed, or may not)
* Possibly strip down and abstract API in the hopes of creating an Unlicense libmpg123 shim

