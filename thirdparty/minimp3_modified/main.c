// gabien-common minimp3_modified test program
// This file was not a derivative of minimp3, but has been licensed via the same licensing as provided in COPYING.txt of this directory.

#include <stdio.h>

#define MINIMP3_IMPLEMENTATION
#include "minimp3.h"

uint8_t load_buffer[65536];
float pcm[MINIMP3_MAX_SAMPLES_PER_FRAME];
mp3dec_t dec = {};

int main(int argc, char ** argv) {
    FILE * fn, * fo;
    if (argc != 3) {
        puts("maxwell FILEIN FILEOUT");
        return 1;
    }
    fn = fopen(argv[1], "rb");
    if (!fn) {
        puts("missing file idk");
        return 1;
    }

    // pass 1
    int loaded_bytes = 0;
    int total_samples = 0;
    while (1) {
        while (loaded_bytes < 65536) {
            int res = fgetc(fn);
            if (res == EOF)
                break;
            load_buffer[loaded_bytes++] = res;
        }
        if (loaded_bytes == 0)
            break;
        int samples = mp3dec_g_decode_frame(&dec, load_buffer, loaded_bytes, NULL);
        total_samples += samples;
        int fb = dec.last_frame_info.frame_bytes;
        if (samples == 0) {
            // attempt resync... :(
            fb = loaded_bytes;
            if (fb > 4096)
                fb = 4096;
        }
        memmove(load_buffer, load_buffer + fb, loaded_bytes - fb);
        loaded_bytes -= fb;
    }
    printf("%ihz %ich // play -r %i -e float -b 32 -c %i %s\n", dec.last_frame_info.hz, dec.last_frame_info.channels, dec.last_frame_info.hz, dec.last_frame_info.channels, argv[2]);
    // pass 2
    memset(&dec, 0, sizeof(dec));
    fo = fopen(argv[2], "wb");
    if (!fo) {
        puts("cannot create output file idk");
        return 1;
    }
    fseek(fn, 0, SEEK_SET);
    loaded_bytes = 0;
    total_samples = 0;
    while (1) {
        while (loaded_bytes < 65536) {
            int res = fgetc(fn);
            if (res == EOF)
                break;
            load_buffer[loaded_bytes++] = res;
        }
        if (loaded_bytes == 0)
            break;
        int samples = mp3dec_g_decode_frame(&dec, load_buffer, loaded_bytes, pcm);
        total_samples += samples;
        int fb = dec.last_frame_info.frame_bytes;
        if (samples == 0) {
            // attempt resync... :(
            fb = 4096;
            if (fb > 4096)
                fb = 4096;
        } else {
            fwrite(pcm, 1, samples * 4 * dec.last_frame_info.channels, fo);
        }
        memmove(load_buffer, load_buffer + fb, loaded_bytes - fb);
        loaded_bytes -= fb;
    }
    printf("%i total samples\n", total_samples);
    // done
    fclose(fn);
    fclose(fo);
    return 1;
}

