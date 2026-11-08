#include "raymob.h"

static Callback onStart = NULL;
static Callback onPause = NULL;
static Callback onResume = NULL;
static Callback onStop = NULL;
static Callback onBack = NULL;

void SetOnStartCallBack(Callback callback){
    onStart = callback;
}
void SetOnResumeCallBack(Callback callback){
    onResume = callback;
}
void SetOnPauseCallBack(Callback callback){
    onPause = callback;
}
void SetOnStopCallBack(Callback callback){
    onStop = callback;
}
void SetOnBackCallBack(Callback callback){
    onBack = callback;
}

JNIEXPORT void JNICALL
custom_onAppStart(JNIEnv *env, jobject obj) {
    if(onStart) onStart();
}
JNIEXPORT void JNICALL
custom_onAppResume(JNIEnv *env, jobject obj) {
    if(onResume) onResume();
}
JNIEXPORT void JNICALL
custom_onAppPause(JNIEnv *env, jobject obj) {
    if(onPause) onPause();
}
JNIEXPORT void JNICALL
custom_onAppStop(JNIEnv *env, jobject obj) {
    if(onStop) onStop();
}
JNIEXPORT void JNICALL
custom_onAppBack(JNIEnv *env, jobject obj) {
    if(onBack) onBack();   /* the game decides: a step back, or the task to the background */
}

void MoveTaskToBackground(void) {
    jobject instance = GetNativeLoaderInstance();
    if (instance == NULL) return;

    JNIEnv* env = AttachCurrentThread();
    jclass cls = (*env)->GetObjectClass(env, instance);
    jmethodID method = (*env)->GetMethodID(env, cls, "moveTaskToBackground", "()V");

    if (method != NULL) (*env)->CallVoidMethod(env, instance, method);

    DetachCurrentThread();
}

static JNINativeMethod methods[] = {
        {"onAppStart", "()V", (void *)custom_onAppStart},
        {"onAppResume", "()V", (void *)custom_onAppResume},
        {"onAppPause", "()V", (void *)custom_onAppPause},
        {"onAppStop", "()V", (void *)custom_onAppStop},
        {"onAppBack", "()V", (void *)custom_onAppBack},
};

void InitCallBacks(){
    jobject nativeLoaderInst = GetNativeLoaderInstance();

    if (nativeLoaderInst != NULL) {
        JNIEnv* env = AttachCurrentThread();

        jclass nativeLoaderClass = (*env)->GetObjectClass(env, nativeLoaderInst);

        (*env)->RegisterNatives(env, nativeLoaderClass, methods, sizeof(methods) / sizeof(methods[0]));

        jfieldID fieldId = (*env)->GetFieldID(env, nativeLoaderClass, "initCallback", "Z");
        (*env)->SetBooleanField(env, nativeLoaderInst, fieldId, JNI_TRUE);

        DetachCurrentThread();
    }
}