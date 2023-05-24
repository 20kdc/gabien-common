// gcc -I/media/modus/External2/jdk-17.0.3+7/include -I/media/modus/External2/jdk-17.0.3+7/include/linux -o jnifns jnifns.c ; ./jnifns
#include <stddef.h>
#include <jni.h>
#define FN(i,s) printf("#define JNI_%s %li\n", s, offsetof(struct JNINativeInterface_, i) / sizeof(void*));

void main() {
    FN(NewStringUTF,"NewStringUTF")

    FN(GetBooleanArrayRegion,"GetBooleanArrayRegion")
    FN(GetByteArrayRegion,"GetByteArrayRegion")
    FN(GetCharArrayRegion,"GetCharArrayRegion")
    FN(GetShortArrayRegion,"GetShortArrayRegion")
    FN(GetIntArrayRegion,"GetIntArrayRegion")
    FN(GetLongArrayRegion,"GetLongArrayRegion")
    FN(GetFloatArrayRegion,"GetFloatArrayRegion")
    FN(GetDoubleArrayRegion,"GetDoubleArrayRegion")

    FN(SetBooleanArrayRegion,"SetBooleanArrayRegion")
    FN(SetByteArrayRegion,"SetByteArrayRegion")
    FN(SetCharArrayRegion,"SetCharArrayRegion")
    FN(SetShortArrayRegion,"SetShortArrayRegion")
    FN(SetIntArrayRegion,"SetIntArrayRegion")
    FN(SetLongArrayRegion,"SetLongArrayRegion")
    FN(SetFloatArrayRegion,"SetFloatArrayRegion")
    FN(SetDoubleArrayRegion,"SetDoubleArrayRegion")

    FN(NewDirectByteBuffer,"NewDirectByteBuffer")
    FN(GetDirectBufferAddress,"GetDirectBufferAddress")
}
