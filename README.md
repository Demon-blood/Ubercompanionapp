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
- E-scooter electricity cost and battery impact estimates
- AI-style waiting zone rankings from your logged delivery history
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


## JVM target fix included

This ZIP sets Java and Kotlin to JVM 17 in `app/build.gradle.kts`.

This fixes:

```text
Inconsistent JVM-target compatibility detected for tasks 'compileDebugJavaWithJavac' (1.8) and 'compileDebugKotlin' (17)
```


## Notes on automatic Uber overlay data

This app does not scan, scrape, OCR, or control the Uber Eats offer popup. Offer data must be entered manually or imported from a compliant source. The decision engine can then use that data automatically once it is inside the companion app.


## Weather & event signals

This version adds:
- Internet permission
- Open-Meteo weather check for Turnhout
- Weather-based e-scooter demand/risk score
- Event lookup scaffold for Turnhout/Belgium using UiTdatabank-style event data
- Weather/event modifiers inside AI waiting-zone scoring

## OCR / Uber popup reading

This app does not perform live OCR, screen scraping, AccessibilityService reading, or automated interaction with the Uber Eats offer popup. Offer data must be entered manually or imported from a compliant source.


## Offer location capture

This version adds:
- Fine/coarse location permissions
- Google fused location provider dependency
- Current GPS snapshot at offer time
- Pickup destination text field
- Delivery destination text field
- Offer timestamp/source metadata
- Trip history display for pickup, delivery, and offer GPS

The GPS capture is user-permission based and does not interact with Uber's UI.
