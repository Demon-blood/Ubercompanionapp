# Delivery Companion AIO

Android Kotlin/Compose delivery companion app.

## Included

- Native Android dashboard
- Floating overlay permission flow
- Foreground overlay service
- Manual offer decision helper
- ACCEPT / REJECT recommendation based on filters
- Minimum pay, €/km, €/hour, distance, time, wait, stacked-order, blocked-area, and preferred-area rules
- Trip logging
- Profit, distance, order, and expense summaries
- Local SharedPreferences storage
- Draggable overlay panel

## Not included

This project does not auto-tap, auto-accept, auto-reject, scrape, inject input into, or control the Uber Eats driver app.

## Build

Open the folder in Android Studio and run:

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Setup on Android

1. Install the APK.
2. Open the app.
3. Press **Start Floating Overlay**.
4. Grant “Display over other apps” permission.
5. Press **Start Floating Overlay** again.