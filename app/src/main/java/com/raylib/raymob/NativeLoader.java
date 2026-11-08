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
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.window.OnBackInvokedDispatcher;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.raylib.raymob.bridge.AdsHelper;
import com.raylib.raymob.bridge.ReviewHelper;

public class NativeLoader extends NativeActivity {
    public DisplayManager displayManager;
    public SoftKeyboard softKeyboard;
    public ReviewHelper reviewHelper;
    public AdsHelper adsHelper;
    private FirebaseAnalytics firebaseAnalytics;
    public boolean initCallback = false;

    // Loading method of your native application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayManager = new DisplayManager(this);
        softKeyboard = new SoftKeyboard(this);
        reviewHelper = new ReviewHelper(this);
        adsHelper = new AdsHelper(this);
        /* Firebase is initialised by the google-services plugin from
           android/app/google-services.json (FirebaseInitProvider runs before this):
           no explicit options here, and no placeholder keys that would point
           Crashlytics at a project that does not exist. A project without that
           file has no default app, so analytics stays off instead of throwing. */
        firebaseAnalytics = FirebaseApp.getApps(this).isEmpty()
                ? null
                : FirebaseAnalytics.getInstance(this);
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

    @Override
    protected void onDestroy() {
        if (adsHelper != null) {
            adsHelper.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (adsHelper != null) {
            adsHelper.onConfigurationChanged();
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

    public void requestInAppReview() {
        reviewHelper.requestInAppReview();
    }

    public String getPreferredLanguage() {
        Configuration configuration = getResources().getConfiguration();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (!configuration.getLocales().isEmpty()) {
                return configuration.getLocales().get(0).toLanguageTag();
            }
        } else if (configuration.locale != null) {
            return configuration.locale.toLanguageTag();
        }

        return java.util.Locale.getDefault().toLanguageTag();
    }

    public void initAds() {
        adsHelper.init();
    }

    public void setAdsBannerVisible(boolean visible) {
        adsHelper.setBannerVisible(visible);
    }

    public float getAdsReservedBottomInset() {
        return adsHelper.getReservedBottomInset();
    }

    public void showInterstitialAd() {
        adsHelper.showInterstitialAd();
    }

    public boolean isInterstitialReady() {
        return adsHelper.isInterstitialReady();
    }

    public boolean wasInterstitialDismissed() {
        return adsHelper.wasInterstitialDismissed();
    }

    public void logAnalyticsEvent(
            String eventName,
            String stringParamName,
            String stringParamValue,
            String intParam1Name,
            int intParam1Value,
            String intParam2Name,
            int intParam2Value,
            String intParam3Name,
            int intParam3Value) {
        if (firebaseAnalytics == null || eventName == null || eventName.isEmpty()) {
            return;
        }

        Bundle params = new Bundle();
        if (stringParamName != null && !stringParamName.isEmpty()) {
            params.putString(stringParamName, stringParamValue == null ? "" : stringParamValue);
        }
        putIntParam(params, intParam1Name, intParam1Value);
        putIntParam(params, intParam2Name, intParam2Value);
        putIntParam(params, intParam3Name, intParam3Value);
        firebaseAnalytics.logEvent(eventName, params);
    }

    private static void putIntParam(Bundle params, String paramName, int value) {
        if (paramName != null && !paramName.isEmpty()) {
            params.putLong(paramName, value);
        }
    }

    public void crashForCrashlyticsTest() {
        new android.os.Handler(android.os.Looper.getMainLooper())
            .post(
                () -> {
                    throw new RuntimeException("Test Crash"); // Force a crash
                });
    }
}
