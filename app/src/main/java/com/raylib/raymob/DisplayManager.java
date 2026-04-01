package com.raylib.raymob;

import android.os.Build;
import android.view.Display;
import android.app.NativeActivity;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class DisplayManager {

    NativeActivity activity;
    public Display display;

    public DisplayManager(android.content.Context context) {
        activity = (NativeActivity)(context);
        enableEdgeToEdge();
        if (BuildConfig.FEATURE_DISPLAY_KEEP_ON) {
            keepScreenOn(true);
        }
        if (BuildConfig.FEATURE_DISPLAY_IMMERSIVE) {
            setImmersiveMode();
        }
        if (BuildConfig.FEATURE_DISPLAY_INTO_CUTOUT) {
            renderIntoCutoutArea();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display = context.getDisplay();
        } else {
            display = ((NativeActivity) (context)).getWindowManager().getDefaultDisplay();
        }
    }

    public void enableEdgeToEdge() {
        Window window = activity.getWindow();
        WindowCompat.enableEdgeToEdge(window);
    }

    public void keepScreenOn(boolean keepOn) {
        if (keepOn) {
            activity.getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            activity.getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void setImmersiveMode() {
        Window window = activity.getWindow();
        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    public int getOrientation()
    {
        return display != null ? display.getRotation() : -1;
    }

    public void renderIntoCutoutArea() {
        // Android 11+ supports ALWAYS, which Android 15 requires for non-floating windows.
        // Android 9-10 only provide SHORT_EDGES for rendering into display cutouts.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        LayoutParams lp = activity.getWindow().getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lp.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else {
            lp.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        activity.getWindow().setAttributes(lp);
    }

}
