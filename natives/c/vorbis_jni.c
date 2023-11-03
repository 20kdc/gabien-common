/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

// even more workarounds!
#define STB_VORBIS_USER_WILL_DO_INCLUDES_THEMSELVES

#include "../../thirdparty/stb_vorbis_modified/stb_vorbis.c"

int64_t J_VORBIS(open)(void * env, void * self, JNIBA_ARG(id), int32_t id_len, JNIBA_ARG(setup), int32_t setup_len, void * cls) {
    char errorDetails[256];
    int error = 0;
    JNIBA_L(id);
    JNIBA_L(setup);
    stb_vorbis_g * instance = stb_vorbis_g_open((uint8_t *) id, (size_t) id_len, (uint8_t *) setup, (size_t) setup_len, &error);
    JNIBA_R(id, JNI_ABORT);
    JNIBA_R(setup, JNI_ABORT);
    if (instance)
        return J_PTR(instance);
    sprintf(errorDetails, "%i", error);
    JNI_ThrowNew(env, cls, errorDetails);
    return 0;
}

int32_t J_VORBIS(getSampleRate)(void * env, void * self, int64_t instance) {
    return stb_vorbis_g_get_sample_rate(C_PTR(instance));
}

int32_t J_VORBIS(getChannels)(void * env, void * self, int64_t instance) {
    return stb_vorbis_g_get_channels(C_PTR(instance));
}

int32_t J_VORBIS(getMaxFrameSize)(void * env, void * self, int64_t instance) {
    return stb_vorbis_g_get_max_frame_size(C_PTR(instance));
}

int32_t J_VORBIS(getPacketSampleCount)(void * env, void * self, int64_t instance, JNIBA_ARG(packet), int32_t packet_len) {
    JNIBA_L(packet);
    int samples = stb_vorbis_g_get_packet_sample_count(C_PTR(instance), (uint8_t *) packet, (size_t) packet_len);
    JNIBA_R(packet, JNI_ABORT);
    return samples;
}

int32_t J_VORBIS(decodeFrame)(void * env, void * self, int64_t instance, JNIBA_ARG(packet), int32_t packet_len, JNIBA_ARG(output)) {
    float ** output_internal = NULL;
    JNIBA_L(packet);
    int samples = stb_vorbis_g_decode_frame(C_PTR(instance), (uint8_t *) packet, (size_t) packet_len, &output_internal);
    JNIBA_R(packet, JNI_ABORT);
    if (output_internal) {
        JNIFA_L(output);
        int channels = stb_vorbis_g_get_channels(C_PTR(instance));
        int idx = 0, i, j;
        for (i = 0; i < samples; i++)
            for (j = 0; j < channels; j++)
                output[idx++] = output_internal[j][i];
        JNIFA_R(output, 0);
    }
    return samples;
}

int32_t J_VORBIS(getError)(void * env, void * self, int64_t instance) {
    return (int32_t) stb_vorbis_g_get_error(C_PTR(instance));
}

void J_VORBIS(flush)(void * env, void * self, int64_t instance) {
    stb_vorbis_g_flush(C_PTR(instance));
}

void J_VORBIS(close)(void * env, void * self, int64_t instance) {
    stb_vorbis_g_close(C_PTR(instance));
}

