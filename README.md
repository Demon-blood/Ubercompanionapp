# Delivery Companion AIO

Android Kotlin/Compose delivery companion app with GitHub Actions APK build workflow.

## Included

- Full Android project source
- GitHub Actions build workflow: `.github/workflows/build-apk.yml`
- Native Android dashboard
- Floating overlay permission flow
- Foreground overlay service
- Manual offer decision helper
- ACCEPT / REJECT recommendation based on filters
- Rules:
  - minimum pay
  - minimum €/km
  - minimum €/hour
  - maximum distance
  - maximum trip time
  - maximum pickup wait
  - stacked-order minimum payout
  - blocked areas
  - preferred areas
- Trip logging
- Profit, distance, order, and expense summaries
- Local SharedPreferences storage
- Draggable overlay panel

## Not included

This project does not auto-tap, auto-accept, auto-reject, scrape, inject input into, or control the Uber Eats driver app.

## Phone-only GitHub build

1. Create a GitHub repository.
2. Upload everything in this ZIP to the repository root.
3. Open the repository on GitHub.
4. Go to **Actions**.
5. Select **Build Android APK**.
6. Tap **Run workflow**.
7. Open the finished workflow run.
8. Download the artifact named `DeliveryCompanionAIO-debug-apk`.

## Local build

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build fix included

This ZIP includes `gradle.properties` with:

```properties
android.useAndroidX=true
android.enableJetifier=true
android.suppressUnsupportedCompileSdk=35
```

This fixes the AndroidX dependency build error in GitHub Actions.
