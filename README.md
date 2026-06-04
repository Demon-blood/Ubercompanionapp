# Uber Eats Companion

Android companion app for e-scooter Uber Eats delivery decisions.

## What this version implements

- Jetpack Compose dashboard
- Manual offer evaluator
- ACCEPT / MAYBE / DECLINE decision engine
- €/km and estimated €/hour calculations
- E-scooter battery estimator using Wh/km and battery capacity
- Rain and wind-aware payout rules
- Room database for offers, deliveries, waiting zones, and store-specific rules
- CSV delivery-history import from Android file picker
- Foreground GPS service using Fused Location Provider
- Live weather fetch using Open-Meteo based on current GPS
- Accessibility Service that reads visible Uber text, parses offers, evaluates them, and saves them locally
- OCR fallback using MediaProjection + ML Kit Text Recognition with throttling and crash guards
- Recent saved offers shown in the app
- Initial Turnhout waiting-zone recommendations until enough imported history exists

## Safety boundary

The app observes visible offer data, calculates, logs, and recommends. It does not tap, accept, decline, reject, or automate the Uber Eats / Uber Driver UI.

## Important reality check

The manual evaluator, GPS, weather, CSV import, database, and decision engine are fully implemented in code.

Live Uber offer extraction depends on what Android and the installed Uber app expose:

- Accessibility works only if Uber's offer text is exposed in the accessibility node tree.
- OCR works only after the user grants Android screen-capture permission.
- OCR accuracy depends on screen layout, language, font size, and offer-card visibility.

## Build

Open the folder in Android Studio and run:

```bash
./gradlew assembleDebug
```

If there is no Gradle wrapper in the checkout yet, use Android Studio's Gradle sync or run with a local Gradle install:

```bash
gradle wrapper
gradle assembleDebug
```

## First run

1. Install the debug APK.
2. Grant location and notification permissions.
3. Tap **Accessibility** and enable **Uber Eats Offer Reader**.
4. Tap **Start GPS**.
5. For fallback screen reading, tap **Start OCR** and approve Android's screen-capture prompt.
6. Open Uber Driver / Uber Eats Driver. When an offer appears, the app will try Accessibility first and OCR fallback if enabled.

## Notes

OCR is deliberately throttled to reduce crashes, battery drain, and memory pressure. It reads the screen for decision support only and does not interact with Uber.

## Maxymo-inspired independent feature implementation

This version includes a high-level feature pass based on the uploaded Maxymo APK resource analysis. No Maxymo source code was copied.

Added capabilities:

- Accessibility-based offer reading.
- Notification-listener offer reading.
- OCR fallback using MediaProjection + ML Kit.
- Floating recommendation overlay.
- Android Text-to-Speech voice alerts.
- Store-specific filters and expanded app-wide rules.
- Pickup distance, total time, min €/hour, min €/km, min payout, stacked-order, multi-stop, and order-and-pay filtering.
- Shift-session database table.
- Google Maps/Waze navigation helper.
- Maxymo-style analytics foundation: recent offers, imported delivery history, best-time stats engine.

Safety/product boundary:

- Auto-accept/auto-decline settings exist in the data model for compatibility with rule storage, but the app does not execute automated taps in Uber Eats.
- The app recommends only. It does not control Uber Eats.


## Tasker automation bridge

This build includes a Tasker bridge. The app sends Android broadcasts when an offer is evaluated and includes offer variables such as recommendation, price, €/km, €/hour, pickup, drop-off, battery-after-trip, and rejection reasons.

See `TASKER_INTEGRATION.md` for exact Tasker profile actions, extras, and command examples.

Implemented Tasker-safe commands:

- start shift
- stop shift
- set mode: normal, rain, heavy_rain, strict
- speak last detected offer
- open navigation to a supplied query or last pickup
- configure named Tasker tasks for ACCEPT / MAYBE / DECLINE recommendations

The app still does not tap Accept or Decline inside Uber Eats.

## Confirmation workflow update

This package adds a confirmation-based Tasker workflow. Detected offers produce Tasker broadcasts and launch a full-screen confirmation screen with `I ACCEPTED`, `I DECLINED`, and `MAYBE / SKIPPED`. Confirmed choices are stored in Room and shown in the app. See `CONFIRMATION_TASKER_WORKFLOW.md`.

Ride-safe speed mode is not included in this update.

## GitHub Actions build

This package includes a ready-to-run GitHub Actions workflow:

```text
.github/workflows/android-build.yml
```

Push the project to GitHub, open the **Actions** tab, run **Android CI**, then download the `uber-eats-companion-debug-apk` artifact. Full instructions are in `GITHUB_ACTIONS_BUILD.md`.

