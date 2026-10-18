#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "lame.h"

#define MAX_FRAMES 8192
#define OUTPUT_BYTES 24576
typedef struct {
    lame_t lame;
    int channels;
    /* Align scratch PCM and sanitize floating-point input without touching MediaCodec buffers. */
    union { short pcm16[MAX_FRAMES * 2]; float pcm32[MAX_FRAMES * 2]; } pcm;
    unsigned char output[OUTPUT_BYTES];
} Encoder;

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass error = (*env)->FindClass(env, type);
    if (error) (*env)->ThrowNew(env, error, message);
}

JNIEXPORT jlong JNICALL Java_com_vibearc_app_NativeMp3_create(JNIEnv *env, jobject self, jint rate, jint channels) {
    (void)self;
    if (channels < 1 || channels > 2 || rate < 8000 || rate > 192000) {
        fail(env, "java/lang/IllegalArgumentException", "Unsupported MP3 input format"); return 0;
    }
    Encoder *encoder = calloc(1, sizeof(*encoder));
    if (!encoder) { fail(env,"java/lang/OutOfMemoryError","MP3 encoder allocation failed"); return 0; }
    encoder->channels = channels;
    encoder->lame = lame_init();
    if (!encoder->lame) { free(encoder); fail(env,"java/lang/IllegalStateException","Could not initialize native MP3 encoder"); return 0; }
    lame_t lame = encoder->lame;
    int result = lame_set_num_channels(lame, channels) |
        lame_set_in_samplerate(lame, rate) |
        lame_set_out_samplerate(lame, rate == 48000 ? 48000 : 44100) |
        lame_set_mode(lame, channels == 1 ? MONO : JOINT_STEREO) |
        lame_set_brate(lame, 320) | lame_set_quality(lame, 2) |
        lame_set_VBR(lame, vbr_off) | lame_set_bWriteVbrTag(lame, 0) |
        lame_set_findReplayGain(lame, 0);
    lame_set_write_id3tag_automatic(lame, 0); // Mp3Tags writes cover/title/artist once.
    if (result != 0 || lame_init_params(lame) < 0) {
        lame_close(lame); free(encoder); fail(env,"java/lang/IllegalStateException","Native MP3 setup failed"); return 0;
    }
    return (jlong)(intptr_t)encoder;
}

JNIEXPORT jint JNICALL Java_com_vibearc_app_NativeMp3_encode(JNIEnv *env, jobject self, jlong handle,
        jobject pcm, jint offset, jint bytes, jboolean floating, jbyteArray output) {
    (void)self;
    Encoder *encoder = (Encoder *)(intptr_t)handle;
    if (!encoder || !pcm || !output) { fail(env,"java/lang/IllegalStateException","MP3 encoder is closed"); return 0; }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, pcm);
    unsigned char *data = (*env)->GetDirectBufferAddress(env, pcm);
    int frame_bytes = encoder->channels * (floating ? 4 : 2);
    if (!data || offset < 0 || bytes < 0 || (jlong)offset + bytes > capacity ||
        bytes % frame_bytes != 0 || bytes / frame_bytes > MAX_FRAMES ||
        (*env)->GetArrayLength(env, output) < OUTPUT_BYTES) {
        fail(env,"java/lang/IllegalArgumentException","Invalid native MP3 buffer"); return 0;
    }
    int frames = bytes / frame_bytes;
    memcpy(&encoder->pcm, data + offset, (size_t)bytes);
    int size;
    if (floating) {
        for (int i = 0; i < frames * encoder->channels; i++) {
            float value = encoder->pcm.pcm32[i];
            encoder->pcm.pcm32[i] = !isfinite(value) ? 0.f : fmaxf(-1.f, fminf(1.f, value));
        }
        size = encoder->channels == 2
            ? lame_encode_buffer_interleaved_ieee_float(encoder->lame, encoder->pcm.pcm32, frames, encoder->output, OUTPUT_BYTES)
            : lame_encode_buffer_ieee_float(encoder->lame, encoder->pcm.pcm32, encoder->pcm.pcm32, frames, encoder->output, OUTPUT_BYTES);
    } else {
        size = encoder->channels == 2
            ? lame_encode_buffer_interleaved(encoder->lame, encoder->pcm.pcm16, frames, encoder->output, OUTPUT_BYTES)
            : lame_encode_buffer(encoder->lame, encoder->pcm.pcm16, encoder->pcm.pcm16, frames, encoder->output, OUTPUT_BYTES);
    }
    if (size < 0) { fail(env,"java/lang/IllegalStateException","Native MP3 encoding failed"); return 0; }
    (*env)->SetByteArrayRegion(env, output, 0, size, (const jbyte *)encoder->output);
    return size;
}

JNIEXPORT jint JNICALL Java_com_vibearc_app_NativeMp3_finish(JNIEnv *env, jobject self, jlong handle, jbyteArray output) {
    (void)self;
    Encoder *encoder = (Encoder *)(intptr_t)handle;
    if (!encoder || !output || (*env)->GetArrayLength(env, output) < OUTPUT_BYTES) {
        fail(env,"java/lang/IllegalStateException","Invalid MP3 finalization"); return 0;
    }
    int size = lame_encode_flush(encoder->lame, encoder->output, OUTPUT_BYTES);
    if (size < 0) { fail(env,"java/lang/IllegalStateException","Native MP3 flush failed"); return 0; }
    (*env)->SetByteArrayRegion(env, output, 0, size, (const jbyte *)encoder->output);
    return size;
}

JNIEXPORT void JNICALL Java_com_vibearc_app_NativeMp3_destroy(JNIEnv *env, jobject self, jlong handle) {
    (void)env; (void)self;
    Encoder *encoder = (Encoder *)(intptr_t)handle;
    if (encoder) { lame_close(encoder->lame); free(encoder); }
}
