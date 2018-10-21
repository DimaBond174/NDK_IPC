#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring

JNICALL
Java_com_bond_ipc_1ndk_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
