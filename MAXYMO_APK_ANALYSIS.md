# Maxymo APK high-level analysis

Analyzed file: `maxymo_v747.apk` supplied by the user.

Scope: high-level capability analysis only. No proprietary Maxymo source code was copied into this project.

## Observable capabilities found from APK assets/resources

The APK contains strings/resources indicating these feature groups:

- Accessibility Service permission flow.
- Notification Listener permission flow.
- Display-over-other-apps / overlay permission flow.
- Precise and background location permission flow.
- Offer filters by earnings, distance, pickup distance, trip distance, trip time, total time, per-mile/per-km/per-hour, minimum order amount, passenger rating, surge, and upfront earnings.
- Auto Accept and Auto Decline settings.
- Bring driver app / offers to screen.
- Stop new requests / stop new offers.
- Shift stats, delivery history, earnings charts, best hour/day, average offer, average per mile/km/hour, trip distance/time.
- Multi-platform labels/resources: Uber, Uber Eats, Lyft, DoorDash, Grubhub, Spark, Vromo and others.
- Overlay options showing distance, per-hour, per-mile/km, time, rating.
- Screenshot/OCR-related resources.
- Navigation app references: Google Maps and Waze.
- Audio/voice offer threshold and sound settings.
- Store/order filters for shopping, pharmacy, curbside, order & pay, stacked orders, multi-stop orders.

## Implemented in this companion app

Implemented independently:

- Accessibility reader for Uber Driver / Uber Eats visible text.
- Notification listener for supported delivery-app offer notifications.
- OCR fallback through Android MediaProjection and ML Kit Text Recognition.
- Floating recommendation overlay service.
- Voice alert manager using Android Text-to-Speech.
- Richer offer parsing: price, km/miles, minutes, pickup/drop-off hints, pickup distance, stacked/multi-stop/order-and-pay/shopping flags.
- Expanded decision engine: min €/km, min €/hour, min payout, max distance, max pickup distance, max total minutes, min trip minutes, rain, wind, battery reserve, store rules.
- Store rules with order-and-pay, stacked order, multi-stop toggles.
- Shift session storage.
- Notification/settings buttons in the app.
- Navigation launcher helpers for Google Maps and Waze.
- App-rule persistence structure.
- History and analytics foundation.

## Not implemented by design

- Automatic tapping of Accept/Decline buttons.
- Forced switching/controlling of the Uber app UI.
- Login automation.
- Subscription/paywall/ad features.
- Any copied Maxymo implementation internals.

Reason: this companion app is designed to observe, calculate, log, and recommend without controlling the Uber Eats app.
