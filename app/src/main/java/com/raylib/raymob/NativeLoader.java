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

package com.raylib.raymob;  // Don't change the package name (see gradle.properties)

import android.app.NativeActivity;
import android.os.Build;
import android.view.KeyEvent;
import android.os.Bundle;
import android.window.OnBackInvokedDispatcher;

public class NativeLoader extends NativeActivity {

    public DisplayManager displayManager;
    public SoftKeyboard softKeyboard;
    public boolean initCallback = false;

    // Loading method of your native application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayManager = new DisplayManager(this);
        softKeyboard = new SoftKeyboard(this);
        System.loadLibrary("raymob");   // Load your game library (don't change raymob, see gradle.properties)

        /* BACK, the supported way: from API 33 the system delivers the gesture here
           instead of the KEYCODE_BACK key, and from API 36 (this app's target) it
           does not dispatch the key at all, finishing the activity instead. The
           native side registers its handlers, and flags itself ready, from
           InitCallBacks() at boot; until a game calls it, BACK backgrounds the app
           rather than going dead. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> { if (initCallback) onAppBack(); else moveTaskToBackground(); });
        }
    }

    // Handling loss and regain of application focus
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (BuildConfig.FEATURE_DISPLAY_IMMERSIVE && hasFocus) {
            displayManager.setImmersiveMode(); // If the app has focus, re-enable immersive mode
        }
    }

    // Callback methods for managing the Android software keyboard
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        softKeyboard.onKeyUpEvent(event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(initCallback) {
            onAppStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(initCallback) {
            onAppResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(initCallback) {
            onAppPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(initCallback){
            onAppStop();
        }
    }

    /** Called from the native side when BACK has nothing left to close: the game
     *  goes to the background rather than being destroyed. */
    public void moveTaskToBackground() {
        runOnUiThread(() -> moveTaskToBack(true));
    }

    private native void onAppStart();
    private native void onAppResume();
    private native void onAppPause();
    private native void onAppStop();
    private native void onAppBack();

}