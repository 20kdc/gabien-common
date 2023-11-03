# minimp3 modified

This contains a modified copy of https://github.com/lieff/minimp3/ !

## Modifications

* S16 output support removed in favour of just floats
* `_g` library name suffix to prevent symbol conflicts
* rename `minimp3_init` to `mp3dec_g_reset_sync` to prevent misnomer
* remove `layer` and `frame_offset` frameinfo fields
* integrate frameinfo struct into main decoder struct as `last_frame_info`
* remove `MINIMP3_NONSTANDARD_BUT_LOGICAL` and `MINIMP3_ONLY_MP3`
* documentation on the functions
* attached simpler test program

## TODO

Specific planned modifications are:

* Support for GaBIEn's kinda-cursed cross-compilation system
* Whatever I have to do to make it capable of measuring a stream without fully decoding it
* Stripping out anything I don't need (particularly stdio will receive this treatment)

However, this hasn't been started *yet.* Terrible bout of laziness, I know.

Things that need doing:

* Remove `MINIMP3_ONLY_MP3`
* Getting it to actually build as part of the gabien-natives build cycle (may require SIMD be removed, or may not)
* Possibly strip down and abstract API in the hopes of creating an Unlicense libmpg123 shim

As with `stb_vorbis_modified`, `main.c` is a test program which `make-vt` builds.

