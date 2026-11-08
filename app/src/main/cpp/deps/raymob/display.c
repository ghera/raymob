/*
 *  raymob License (MIT)
 *
 *  Copyright (c) 2023-2024 Le Juez Victor
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

#include "raymob.h"

void KeepScreenOn(bool keepOn)
{
    jobject nativeLoaderInst = GetNativeLoaderInstance();

    if (nativeLoaderInst != NULL) {
        JNIEnv* env = AttachCurrentThread();

        jclass nativeLoaderClass = (*env)->GetObjectClass(env, nativeLoaderInst);
        jfieldID displayManagerField = (*env)->GetFieldID(env, nativeLoaderClass, "displayManager", "Lcom/raylib/raymob/DisplayManager;");
        jobject displayManager = (*env)->GetObjectField(env, nativeLoaderInst, displayManagerField);

        if (displayManager != NULL) {
            jclass displayManagerClass = (*env)->GetObjectClass(env, displayManager);
            jmethodID method = (*env)->GetMethodID(env, displayManagerClass, "keepScreenOn", "(Z)V");
            (*env)->CallVoidMethod(env, displayManager, method, (jboolean)keepOn);
        }

        DetachCurrentThread();
    }
}

Orientation GetScreenOrientation()
{
    Orientation result = 0;
    jobject nativeLoaderInst = GetNativeLoaderInstance();

    if (nativeLoaderInst != NULL) {
        JNIEnv* env = AttachCurrentThread();

        jclass nativeLoaderClass = (*env)->GetObjectClass(env, nativeLoaderInst);
        jfieldID displayManagerField = (*env)->GetFieldID(env, nativeLoaderClass, "displayManager", "Lcom/raylib/raymob/DisplayManager;");
        jobject displayManager = (*env)->GetObjectField(env, nativeLoaderInst, displayManagerField);

        if (displayManager != NULL) {
            jclass displayManagerClass = (*env)->GetObjectClass(env, displayManager);
            jmethodID screenOrientationMethod = (*env)->GetMethodID(env, displayManagerClass, "getOrientation", "()I");
            jint screenOrientation = (*env)->CallIntMethod(env, displayManager, screenOrientationMethod);

            if (result >= 0 && result < 4) { // just sanity checking in case android API changes
                result = screenOrientation;
            }
        }

        DetachCurrentThread();
    }

    return result;
}

Vector4 GetSafeAreaInsets(void)
{
    Vector4 result = {0};
    jobject nativeLoaderInst = GetNativeLoaderInstance();

    if (nativeLoaderInst != NULL) {
        JNIEnv* env = AttachCurrentThread();

        jclass nativeLoaderClass = (*env)->GetObjectClass(env, nativeLoaderInst);
        jfieldID displayManagerField = (*env)->GetFieldID(env, nativeLoaderClass, "displayManager", "Lcom/raylib/raymob/DisplayManager;");
        if (displayManagerField == NULL) {
            DetachCurrentThread();
            return result;
        }

        jobject displayManager = (*env)->GetObjectField(env, nativeLoaderInst, displayManagerField);

        if (displayManager != NULL) {
            jclass displayManagerClass = (*env)->GetObjectClass(env, displayManager);
            jmethodID safeAreaMethod = (*env)->GetMethodID(env, displayManagerClass, "getSafeAreaInsets", "()[I");

            if (safeAreaMethod != NULL) {
                jintArray safeArea = (jintArray)(*env)->CallObjectMethod(env, displayManager, safeAreaMethod);
                if ((*env)->ExceptionCheck(env)) {
                    (*env)->ExceptionDescribe(env);
                    (*env)->ExceptionClear(env);
                    safeArea = NULL;
                }

                if (safeArea != NULL && (*env)->GetArrayLength(env, safeArea) >= 4) {
                    jint values[4] = {0};
                    (*env)->GetIntArrayRegion(env, safeArea, 0, 4, values);
                    result.x = (float)values[0];
                    result.y = (float)values[1];
                    result.z = (float)values[2];
                    result.w = (float)values[3];
                }

                if (safeArea != NULL) {
                    (*env)->DeleteLocalRef(env, safeArea);
                }
            }

            (*env)->DeleteLocalRef(env, displayManagerClass);
            (*env)->DeleteLocalRef(env, displayManager);
        }

        (*env)->DeleteLocalRef(env, nativeLoaderClass);
        DetachCurrentThread();
    }

    return result;
}
