/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

// even more workarounds!
#define MINIMP3_USER_WILL_DO_INCLUDES_THEMSELVES
#define MINIMP3_IMPLEMENTATION

#include "../../thirdparty/minimp3_modified/minimp3.h"

int64_t J_MP3(alloc)(void * env, void * self, void * cls) {
    mp3dec_t * instance = malloc(sizeof(mp3dec_t));
    if (instance) {
        memset(instance, 0, sizeof(mp3dec_t));
        return J_PTR(instance);
    }
    JNI_ThrowNew(env, cls, "Unable to allocate memory");
    return 0;
}

int32_t J_MP3(getLastFrameBytes)(void * env, void * self, int64_t instance) {
    return ((mp3dec_t *) C_PTR(instance))->last_frame_info.frame_bytes;
}

int32_t J_MP3(getLastFrameSampleRate)(void * env, void * self, int64_t instance) {
    return ((mp3dec_t *) C_PTR(instance))->last_frame_info.hz;
}

int32_t J_MP3(getLastFrameChannels)(void * env, void * self, int64_t instance) {
    return ((mp3dec_t *) C_PTR(instance))->last_frame_info.channels;
}

int32_t J_MP3(decodeFrame)(void * env, void * self, int64_t instance, JNIBA_ARG(packet), int32_t packet_len, JNIBA_ARG(output)) {
    JNIBA_L(packet);
    JNIFA_L(output);
    int samples = mp3dec_g_decode_frame(C_PTR(instance), (uint8_t *) packet, (size_t) packet_len, (float *) output);
    JNIBA_R(packet, JNI_ABORT);
    JNIFA_R(output, 0);
    return samples;
}

void J_MP3(free)(void * env, void * self, int64_t instance) {
    free(C_PTR(instance));
}

