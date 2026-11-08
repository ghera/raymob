# raymob fork for a multiplatform project based on raylib-iOS

⚠️ This is an unofficial fork of [raymob](https://github.com/Bigfoot71/raymob) providing multiplatform (Android / iOS / Windows / macOS / Linux) support.

This fork is **not affiliated with the official raymob project**.

## What is different from upstream

- Switched the dependency from [raylib](https://github.com/raysan5/raylib) to [raylib-iOS](https://github.com/raysan5/raylib);

- Moved raylib submodule path to share it across across IDEs; 

- Android: the back gesture is routed through `OnBackInvokedCallback` (API 33+, the only
  path from API 36) instead of `KEYCODE_BACK`, with `MoveTaskToBackground()` available from
  C. A game receives the BACK callback only if it calls `InitCallBacks()` once after
  `InitWindow`; until then BACK sends the app to the background. Below API 33 there is no
  dispatcher: the key still arrives, but raylib *eats* it after queueing it, so a game that
  wants BACK on those versions has to read `KEY_BACK` from `GetKeyPressed()`;

- Android: the app focus edge belongs to the engine, not to the bridge. `rcore_android.c`
  grants one frame after `APP_CMD_LOST_FOCUS` with `FLAG_WINDOW_UNFOCUSED` still set and
  returns from the event pump, so the game pauses by reading `IsWindowFocused()` and the
  frame it pauses in is the one the system snapshots -- nothing to install, and nothing
  written from the Java thread. `SetOnPauseCallBack()` / `SetOnResumeCallBack()` still
  fire but are deprecated for that job: they are the same edge, delivered from the wrong
  thread;

- Android: display cutout insets (`GetSafeAreaInsets()`), in-app review
  (`RequestInAppReview()`), device language (`GetPreferredLanguage()`) and the AdMob
  bridge (banner/interstitial + UMP consent form, `InitAds()` / `SetAdsBannerVisible()` /
  `ShowInterstitialAd()`), which also propagates the consent decision to Firebase Consent
  Mode v2. The ads stack is off as long as `admob.app_id` in `gradle.properties` is empty;
  to turn it on, fill the three `admob.*` properties, declare
  `com.google.android.gms.permission.AD_ID` in the manifest, and add `google-services.json`
  plus the `google-services` plugin only if analytics is wanted too;
  
- Minor tooling updates.

## Recommended project structure

```
MyProject
├── desktop (VSCode project for Windows / macOS / Linux)
├── ios (Xcode project - see example in raylib-iOS)
├── android (Android Studio project - this repository)
├── core (common code shared across all platforms)
└── raylib (raylib-iOS submodule)
```

## Status

This fork is intended for experimental and personal use.
It is **not production-ready**.

---

<img src="app/src/main/ic_launcher-playstore.png" alt="raymob icon" width="256" height="256">

# raymob [![Targeting raylib 5.5+](https://img.shields.io/badge/raylib-5.5+-3DDC84)](https://raylib.com) [![Supported Platforms](https://img.shields.io/badge/Platform-Android-3DDC84)](https://developer.android.com/) [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE) [![License](https://img.shields.io/badge/license-zlib%2Flibpng-blue.svg)](LICENSE)

raymob is a simple implementation of [raylib](https://www.raylib.com/) for Android.

## Prerequisites

**You will need SDK API 34 and NDK r23 (or higher, compatible with CMake 3.30.3).**

If you already have this version of SDK and NDK without having Android Studio, you should still be able to compile the project using `gradlew.bat` for Windows or `gradlew` for Linux or MacOS.

## How to Use?

1. Clone the repository and automatically initialize and update all submodules:
   ```
   git clone --recurse-submodules https://github.com/Bigfoot71/raymob.git
   ```
2. Open the `gradle.properties` file and fill in the necessary values for your application, such as the app name, package name, version, etc.
3. That's it! You can start coding your raylib app in the `app/src/main/cpp` directory!

> [!NOTE]
> Do not modify the instances of `com.raylib.raymob` and `raymob` as they will be automatically replaced during compilation with the information provided in `gradle.properties`.

## Compatibility

By default, the raymob template targets APIs 24 to 34, which corresponds from Android 7.0 to Android 13 (_so 96% of devices according to [apilevels.com](https://apilevels.com/)_).

This project is fully C/C++ compatible for the logical part with raylib and you can also customize the Java part to your wishes.

## Additional Features

In this project, you have access to the header file [raymob.h](app/src/main/cpp/deps/raymob/raymob.h), which provides functions for controlling sensors, vibration, and the Android soft keyboard, as well as lower-level functions such as obtaining the `android_app`, manipulating the cache, managing resources, and calling Java functions from your native code.

## Useful Links

- [AdMob Integration in raymob](https://gist.github.com/Bigfoot71/b3a658458ece93ddcb06f4c78f85076a): Gist demonstrating the integration of AdMob in raymob.

## Contributions

If you believe that your repositories or gists related to raymob can be beneficial to others, please feel free to reach out to me or open a pull request to have them added here.
I welcome contributions and collaborations to enhance the raymob project and make it more valuable for the community.

You can also contribute to the project by reporting issues, suggesting improvements, or helping to document the project. Your input is highly appreciated!

### Support

If you have any questions or encounter problems when using this implementation, please do not hesitate to ask for help by submitting an issue on this repository, this may help other users.
