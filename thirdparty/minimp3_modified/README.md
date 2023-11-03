# minimp3 modified

This contains a modified copy of https://github.com/lieff/minimp3/ !

As with `stb_vorbis_modified`, `main.c` is a test program which `make-vt` builds.

## Modifications

* S16 output support removed in favour of just floats
* `_g` library name suffix to prevent symbol conflicts
* rename `minimp3_init` to `mp3dec_g_reset_sync` to prevent misnomer...
  * actually ditch it entirely - when do you lose sync and NOT want to properly reset the decoder...?
* remove `layer`, `bitrate_kbps` and `frame_offset` frameinfo fields
* integrate frameinfo struct into main decoder struct as `last_frame_info`
* remove `MINIMP3_NONSTANDARD_BUT_LOGICAL` and `MINIMP3_ONLY_MP3`
* documentation on the functions
* attached simpler test program
* remove x86-64 vector support because xmmintrin.h relies on stdlib.h for some reason

## TODOs

* Hardening? (`size_t`)

