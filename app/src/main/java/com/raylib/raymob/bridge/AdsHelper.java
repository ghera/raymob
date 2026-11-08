package com.raylib.raymob.bridge;

import android.app.NativeActivity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.raylib.raymob.BuildConfig;
import java.util.HashMap;
import java.util.Map;

public class AdsHelper {
  private static final String TAG = "raymobAds";

  private final NativeActivity activity;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private ConsentInformation consentInformation;
  private WindowManager windowManager;
  private WindowManager.LayoutParams bannerWindowParams;
  private FrameLayout bannerContainer;
  private AdView adView;
  private AdSize adSize;

  private InterstitialAd interstitialAd;
  private boolean interstitialDismissed = false;

  private boolean initRequested = false;
  private boolean mobileAdsInitStarted = false;
  private boolean mobileAdsInitialized = false;
  private boolean bannerVisibleRequested = false;
  private boolean bannerLoadStarted = false;
  private int reservedBottomInsetPx = 0;
  private int bottomSystemInsetPx = 0;

  public AdsHelper(NativeActivity activity) {
    this.activity = activity;
  }

  public void init() {
    /* No AdMob app id configured (see app/build.gradle): keep the ads, the consent
    form and the Firebase consent propagation off, instead of initialising the
    SDK against an empty id. */
    if (BuildConfig.ADMOB_APP_ID.isEmpty()) {
      return;
    }

    runOnUiThread(
        () -> {
          if (initRequested) {
            return;
          }

          initRequested = true;
          ensureBannerContainer();
          requestConsent();
        });
  }

  public void setBannerVisible(boolean visible) {
    runOnUiThread(
        () -> {
          bannerVisibleRequested = visible;
          updateBannerContainerVisibility();
          if (visible) {
            updateReservedBottomInset(getReservedHeightForAdSize(getPreferredAdSize()));
            loadBannerIfReady();
          }
        });
  }

  public float getReservedBottomInset() {
    return reservedBottomInsetPx;
  }

  public void onConfigurationChanged() {
    runOnUiThread(
        () -> {
          if (adView != null) {
            destroyAdView();
          }
          bannerLoadStarted = false;
          if (bannerVisibleRequested) {
            updateReservedBottomInset(getReservedHeightForAdSize(getPreferredAdSize()));
          }
          loadBannerIfReady();
        });
  }

  public void showInterstitialAd() {
    runOnUiThread(
        () -> {
          if (interstitialAd == null) {
            interstitialDismissed = true;
            return;
          }
          interstitialAd.setAdEventCallback(
              new InterstitialAdEventCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                  interstitialDismissed = true;
                  loadInterstitialAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(
                    @NonNull FullScreenContentError error) {
                  // Show fallita dopo il load: sblocca comunque l'audio del gioco e ricarica.
                  Log.w(TAG, "Interstitial failed to show: " + error);
                  interstitialDismissed = true;
                  loadInterstitialAd();
                }
              });
          interstitialAd.show(activity);
        });
  }

  public boolean isInterstitialReady() {
    return interstitialAd != null;
  }

  public boolean wasInterstitialDismissed() {
    boolean dismissed = interstitialDismissed;
    interstitialDismissed = false;
    return dismissed;
  }

  private void loadInterstitialAd() {
    if (!mobileAdsInitialized) {
      return;
    }
    AdRequest request = new AdRequest.Builder(BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID).build();
    InterstitialAd.load(
        request,
        new AdLoadCallback<InterstitialAd>() {
          @Override
          public void onAdLoaded(@NonNull InterstitialAd ad) {
            interstitialAd = ad;
            Log.i(TAG, "Interstitial loaded");
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError error) {
            Log.w(TAG, "Interstitial failed to load: " + error);
          }
        });
  }

  public void destroy() {
    runOnUiThread(
        () -> {
          if (interstitialAd != null) {
            interstitialAd.destroy();
            interstitialAd = null;
          }
          destroyAdView();
          if (bannerContainer != null) {
            removeBannerContainer();
          }
          bannerContainer = null;
          updateReservedBottomInset(0);
        });
  }

  private void requestConsent() {
    consentInformation = UserMessagingPlatform.getConsentInformation(activity);
    ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

    consentInformation.requestConsentInfoUpdate(
        activity,
        params,
        () ->
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                activity,
                formError -> {
                  if (formError != null) {
                    Log.w(TAG, "Consent form failed: " + formError.getMessage());
                  }
                  initializeMobileAdsIfAllowed();
                }),
        requestConsentError -> {
          Log.w(TAG, "Consent info update failed: " + requestConsentError.getMessage());
          initializeMobileAdsIfAllowed();
        });

    initializeMobileAdsIfAllowed();
  }

  /** Propagates the UMP consent decision to Firebase Consent Mode v2. */
  private void applyFirebaseConsent(boolean granted) {
    if (FirebaseApp.getApps(activity).isEmpty()) {
      return; // no google-services.json: there is no default app to propagate to
    }

    FirebaseAnalytics.ConsentStatus status =
        granted ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED;
    Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> map = new HashMap<>();
    map.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, status);
    map.put(FirebaseAnalytics.ConsentType.AD_STORAGE, status);
    map.put(FirebaseAnalytics.ConsentType.AD_USER_DATA, status);
    map.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION, status);
    FirebaseAnalytics.getInstance(activity).setConsent(map);
  }

  private void initializeMobileAdsIfAllowed() {
    boolean canRequestAds = consentInformation != null && consentInformation.canRequestAds();
    applyFirebaseConsent(canRequestAds);
    if (!canRequestAds) {
      return;
    }

    if (mobileAdsInitStarted) {
      loadBannerIfReady();
      return;
    }

    mobileAdsInitStarted = true;
    new Thread(
            () ->
                MobileAds.initialize(
                    activity,
                    new InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build(),
                    initializationStatus ->
                        runOnUiThread(
                            () -> {
                              mobileAdsInitialized = true;
                              Log.i(TAG, "GMA Next-Gen initialized");
                              loadBannerIfReady();
                              loadInterstitialAd();
                            })))
        .start();
  }

  private void ensureBannerContainer() {
    if (bannerContainer != null) {
      return;
    }

    bannerContainer = new FrameLayout(activity);
    bannerContainer.setClipToPadding(false);
    bannerContainer.setVisibility(View.GONE);

    windowManager = activity.getWindowManager();
    bannerWindowParams =
        new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    bannerWindowParams.gravity = Gravity.BOTTOM | Gravity.START;
    bannerWindowParams.packageName = activity.getPackageName();
    bannerWindowParams.token = activity.getWindow().getDecorView().getWindowToken();

    try {
      windowManager.addView(bannerContainer, bannerWindowParams);
    } catch (RuntimeException e) {
      Log.w(TAG, "Window overlay failed, falling back to content view", e);
      FrameLayout.LayoutParams layoutParams =
          new FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.WRAP_CONTENT,
              Gravity.BOTTOM | Gravity.START);
      activity.addContentView(bannerContainer, layoutParams);
      bannerWindowParams = null;
    }

    layoutBannerCentered();
    updateReservedBottomInset(getReservedHeightForAdSize(getPreferredAdSize()));

    ViewCompat.setOnApplyWindowInsetsListener(
        bannerContainer,
        (view, insets) -> {
          Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
          bottomSystemInsetPx = systemBars.bottom;
          layoutBannerCentered();
          if (adView != null) {
            updateReservedBottomInsetForCurrentBanner();
          } else {
            updateReservedBottomInset(getReservedHeightForAdSize(getPreferredAdSize()));
          }
          return insets;
        });
    ViewCompat.requestApplyInsets(bannerContainer);
  }

  private void loadBannerIfReady() {
    if (!bannerVisibleRequested || !mobileAdsInitialized) {
      return;
    }

    ensureBannerContainer();

    if (adView != null && bannerLoadStarted) {
      updateReservedBottomInsetForCurrentBanner();
      updateBannerContainerVisibility();
      return;
    }

    destroyAdView();

    adSize = getPreferredAdSize();
    adView = new AdView(activity);
    bannerLoadStarted = true;

    FrameLayout.LayoutParams adViewParams =
        new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER);
    bannerContainer.addView(adView, adViewParams);
    bannerContainer.bringToFront();
    layoutBannerCentered();

    updateReservedBottomInset(getReservedHeightForAdSize(adSize));
    updateBannerContainerVisibility();

    BannerAdRequest adRequest =
        new BannerAdRequest.Builder(BuildConfig.ADMOB_BANNER_AD_UNIT_ID, adSize).build();
    adView.loadAd(
        adRequest,
        new AdLoadCallback<BannerAd>() {
          @Override
          public void onAdLoaded(@NonNull BannerAd bannerAd) {
            Log.i(TAG, "Banner loaded");
            updateReservedBottomInsetForCurrentBanner();
            updateBannerContainerVisibility();
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            Log.w(TAG, "Banner failed to load: " + adError);
            updateReservedBottomInsetForCurrentBanner();
          }
        });
  }

  private int getReservedHeightForAdSize(AdSize adSize) {
    return getReservedHeightForBannerHeight(adSize.getHeightInPixels(activity));
  }

  private void updateReservedBottomInsetForCurrentBanner() {
    if (adView == null) {
      return;
    }

    int adHeightPx = adView.getHeight();
    if (adHeightPx <= 0) {
      return;
    }

    updateReservedBottomInset(getReservedHeightForBannerHeight(adHeightPx));
  }

  private void updateReservedBottomInset(int insetPx) {
    reservedBottomInsetPx = Math.max(0, insetPx);
  }

  private int getReservedHeightForBannerHeight(int bannerHeightPx) {
    return Math.max(0, bannerHeightPx) + bottomSystemInsetPx;
  }

  private void layoutBannerCentered() {
    if (bannerContainer == null) {
      return;
    }

    int bannerWidthPx = getPreferredAdSize().getWidthInPixels(activity);

    if (bannerWindowParams != null && windowManager != null) {
      int displayWidthPx = getDisplayWidthPx();
      int leftPx = Math.max(0, (displayWidthPx - bannerWidthPx) / 2);
      bannerWindowParams.width = bannerWidthPx;
      bannerWindowParams.x = leftPx;
      bannerWindowParams.y = bottomSystemInsetPx;
      bannerWindowParams.gravity = Gravity.BOTTOM | Gravity.START;
      try {
        windowManager.updateViewLayout(bannerContainer, bannerWindowParams);
      } catch (RuntimeException e) {
        Log.w(TAG, "Banner layout update failed", e);
      }
      return;
    }

    ViewGroup.LayoutParams params = bannerContainer.getLayoutParams();
    if (params instanceof FrameLayout.LayoutParams) {
      FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
      int displayWidthPx = getDisplayWidthPx();
      int leftPx = Math.max(0, (displayWidthPx - bannerWidthPx) / 2);
      frameParams.width = bannerWidthPx;
      frameParams.leftMargin = leftPx;
      frameParams.bottomMargin = bottomSystemInsetPx;
      frameParams.gravity = Gravity.BOTTOM | Gravity.START;
      bannerContainer.setLayoutParams(frameParams);
    }
  }

  private AdSize getPreferredAdSize() {
    boolean isTablet =
        (activity.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    return isTablet ? AdSize.FULL_BANNER : AdSize.BANNER;
  }

  private int getDisplayWidthPx() {
    return activity.getResources().getDisplayMetrics().widthPixels;
  }

  private void updateBannerContainerVisibility() {
    if (bannerContainer == null) {
      return;
    }
    bannerContainer.setVisibility(
        bannerVisibleRequested && adView != null ? View.VISIBLE : View.GONE);
    bannerContainer.bringToFront();
  }

  private void removeBannerContainer() {
    if (bannerWindowParams != null && windowManager != null) {
      try {
        windowManager.removeViewImmediate(bannerContainer);
      } catch (RuntimeException e) {
        Log.w(TAG, "Banner window removal failed", e);
      }
      bannerWindowParams = null;
      bannerContainer = null;
      return;
    }

    if (bannerContainer != null && bannerContainer.getParent() instanceof ViewGroup) {
      ((ViewGroup) bannerContainer.getParent()).removeView(bannerContainer);
    }
    bannerContainer = null;
  }

  private void destroyAdView() {
    if (adView == null) {
      return;
    }

    if (adView.getParent() instanceof ViewGroup) {
      ((ViewGroup) adView.getParent()).removeView(adView);
    }
    adView.destroy();
    adView = null;
  }

  private void runOnUiThread(Runnable runnable) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      runnable.run();
    } else {
      mainHandler.post(runnable);
    }
  }
}
