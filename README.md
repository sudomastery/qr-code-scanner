# QR Scanner

A fast, dark-mode QR and barcode scanner for Android built with Kotlin, Jetpack Compose, CameraX, and ML Kit.

## Features

- On-device scanning with ML Kit (no network needed), supports QR, Aztec, Data Matrix, PDF417, EAN, UPC, Code 128/39/93, Codabar, and ITF
- Dark Material You design with large rounded corners
- Scan history saved locally with Room: search, favorites, copy, delete single entries or clear all
- Vibrate on scan (toggleable) and optional beep
- Auto open links or show a result sheet first (toggleable)
- URL tracker removal: strips utm_*, fbclid, gclid, msclkid, igshid, si, and many other tracking parameters before opening or copying
- OTP key extractor: scanning an otpauth:// code shows issuer, account, and algorithm details, with the secret key hidden by default and a one-tap copy button
- Wi-Fi QR parsing with password reveal and copy
- Recognizes email, phone, SMS, geo, and contact (vCard/MECARD) codes with matching actions
- Flashlight toggle and pinch to zoom
- Scan codes from images in your gallery

## Building

Requires JDK 17+ and the Android SDK (compileSdk 35).

```sh
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

## Install on a device

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Privacy

Everything stays on the device. There are no network permissions, no analytics, and no ads. History is stored in a local database and can be disabled or cleared in Settings.
