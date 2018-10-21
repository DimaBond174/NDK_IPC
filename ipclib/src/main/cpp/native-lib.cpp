#include <jni.h>
#include <string>
#include "WorkerThread.h"

WorkerThread workerThread;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_com_bond_ipclib_IPClib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jint JNICALL
Java_com_bond_ipclib_IPClib_init(
        JNIEnv *env,
        jobject, /* this */
        jstring appname,
        jstring shmname,
        jstring appname2,
        jint strsize) {
    const char *c_appname = env->GetStringUTFChars(appname,NULL);
    const char *c_appname2 = env->GetStringUTFChars(appname2,NULL);
    const char *c_shmname = env->GetStringUTFChars(shmname,NULL);
    workerThread.init(c_appname, c_shmname, c_appname2, strsize);
    env->ReleaseStringUTFChars(appname,c_appname);
    env->ReleaseStringUTFChars(appname2,c_appname2);
    env->ReleaseStringUTFChars(shmname,c_shmname);
    return 1;
}

JNIEXPORT jint JNICALL
Java_com_bond_ipclib_IPClib_start(
        JNIEnv *env,
     jobject /* this */) {

    return workerThread.start();
}

JNIEXPORT void JNICALL
Java_com_bond_ipclib_IPClib_stop(
        JNIEnv *env,
        jobject /* this */) {

    workerThread.stop();
}

JNIEXPORT jint JNICALL
Java_com_bond_ipclib_IPClib_getThreadState(
        JNIEnv *env,
        jobject /* this */) {

    return workerThread.getThreadState();
}

JNIEXPORT void JNICALL Java_com_bond_ipclib_IPClib_sendShared(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray data,
        jint len)
{
    jbyte * cData1 = env->GetByteArrayElements(data, 0);
    workerThread.sendShared(cData1, len);

    env->ReleaseByteArrayElements(data, cData1, JNI_ABORT);
}

JNIEXPORT jbyteArray JNICALL
Java_com_bond_ipclib_IPClib_getShared(
        JNIEnv *env,
        jobject /* this */) {

    std::shared_ptr<std::vector<int8_t>> p_vec = workerThread.getShared();
    if (!p_vec) { return nullptr;}
    jbyteArray result=env->NewByteArray(p_vec.get()->size());
    env->SetByteArrayRegion(result, 0, p_vec.get()->size(), p_vec.get()->data());
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_bond_ipclib_IPClib_getSock(
        JNIEnv *env,
        jobject /* this */)
{
    return env->NewStringUTF(workerThread.getSock().c_str());
}

JNIEXPORT void JNICALL
Java_com_bond_ipclib_IPClib_sendSock(
        JNIEnv *env,
        jobject, /* this */
        jstring str) {
    const char *c_str = env->GetStringUTFChars(str,NULL);

    workerThread.sendSock(c_str);
    env->ReleaseStringUTFChars(str,c_str);
}

JNIEXPORT jstring JNICALL
Java_com_bond_ipclib_IPClib_getALoop(
        JNIEnv *env,
        jobject /* this */)
{
    return env->NewStringUTF(workerThread.getALoop().c_str());
}

JNIEXPORT void JNICALL
Java_com_bond_ipclib_IPClib_sendALoop(
        JNIEnv *env,
        jobject, /* this */
        jstring str) {
    const char *c_str = env->GetStringUTFChars(str,NULL);

    workerThread.sendALoop(c_str);
    env->ReleaseStringUTFChars(str,c_str);
}

#ifdef __cplusplus
}
#endif