# Quick Tool

A single native Android app (Kotlin, XML Views, Material 3, MVVM) bundling four tools:

1. **Quick Volume** — media/alarm/notification/ring sliders, ringer modes, notification controls, home-screen widget.
2. **Screenshot & Screen Recorder** — MediaProjection screenshots, MP4 screen recording with optional mic, image editor (rotate/draw/undo/save/share), vertical image stitcher, floating capture bubble.
3. **Internet Speed Meter** — live download/upload rates from `TrafficStats`, foreground notification, optional floating overlay. No network traffic is generated or sent anywhere.
4. **QR Code Scanner** — CameraX + bundled ML Kit barcode scanner, torch, pinch zoom, scan from gallery, history.

Package: `com.kafi.utilityhub` · minSdk 24 · target/compileSdk 36 · no ads, no analytics, no `INTERNET` permission.

## Build

The project is a standard Android Studio project (AGP 8.10.1, Gradle 8.11.1, Kotlin 2.1.0, JDK 17).

Local:
```bash
cd KafiUtilityHub
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

CI: `.github/workflows/android.yml` builds `assembleDebug` + unit tests on every push/PR and uploads `app-debug` as an artifact. The Gradle wrapper JAR is not committed; run `gradle wrapper` once locally or let the workflow-installed Gradle build it.

> The app cannot be compiled inside Termux on this 32-bit ARM device (no JDK/Android SDK, ~577 MB RAM). Use the GitHub Actions workflow or a PC with Android Studio.

## Permissions and why

| Permission | Used for |
|---|---|
| `CAMERA` | QR scanning only |
| `POST_NOTIFICATIONS` | foreground service notices (API 33+) |
| `SYSTEM_ALERT_WINDOW` | optional floating capture bubble / speed overlay |
| `FOREGROUND_SERVICE*` | volume notification, projection capture/recording, speed meter |
| `RECORD_AUDIO` | optional microphone audio in recordings (asked only when enabled) |
| `ACCESS_NOTIFICATION_POLICY` | Silent ringer mode toggle |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (≤32) | picking images for the editor/stitcher/gallery scan |
| `WRITE_EXTERNAL_STORAGE` (≤28) | saving captures on older Android versions |
| `ACCESS_NETWORK_STATE` | read traffic counters |
| `WAKE_LOCK`, `VIBRATE` | recording reliability, widget feedback |

Screen capture requires per-session system consent; Android 14+ does not allow reusing a MediaProjection token, so capture/record ask again each time.

## Project layout

```
app/src/main/java/com/kafi/utilityhub/
  data/        Room history + DataStore settings
  capture/     ProjectionHolder, ScreenCaptureService, ScreenRecordService,
               CaptureActivity, OverlayService, DrawingView, EditorActivity, StitchActivity
  network/     TrafficMonitor, SpeedMeterService, SpeedMeterActivity
  qr/          BarcodeAnalyzer, QrScannerActivity
  volume/      VolumeActivity, VolumeNotificationService, VolumeWidgetProvider
  ui/          Home/Tools/History/Settings fragments, adapter
  util/        NotificationHelper, PermissionHelper, Formatters, MediaStoreUtils
app/src/main/res/  layouts, menu, values, drawables, widget info
app/src/test/      Formatters unit tests
```

## Known limitations

- No on-device build recipe in this repo environment; CI is the supported build path.
- Screenshot/recording use `MediaProjection`, so secure (DRM) surfaces appear black.
- The widget updates volume for the music stream only.
- The image editor supports rotate + freehand drawing; it does not implement crop or text overlays.