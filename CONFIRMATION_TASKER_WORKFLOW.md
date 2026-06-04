# Confirmation-based Tasker workflow

This build implements a confirmation workflow instead of unattended Uber Eats UI tapping.

## What happens when an offer is detected

1. The app reads offer text through Accessibility, notifications, manual input, or OCR fallback.
2. The decision engine evaluates the offer.
3. The app broadcasts Tasker-compatible intent actions with offer details.
4. The app opens a full-screen confirmation screen.
5. You choose one of:
   - `I ACCEPTED`
   - `I DECLINED`
   - `MAYBE / SKIPPED`
6. The choice is saved to the local Room database.
7. For accepted/declined confirmations, the app attempts to bring Uber Driver/Eats to the front so you can complete the final platform action yourself.

## Tasker actions exposed by the app

Tasker can send these Android intent actions to the companion app:

```text
com.example.ubereatscompanion.TASKER.SHOW_CONFIRMATION
com.example.ubereatscompanion.TASKER.MARK_ACCEPTED
com.example.ubereatscompanion.TASKER.MARK_DECLINED
com.example.ubereatscompanion.TASKER.MARK_MAYBE
com.example.ubereatscompanion.TASKER.SPEAK_LAST_OFFER
com.example.ubereatscompanion.TASKER.OPEN_NAVIGATION
com.example.ubereatscompanion.TASKER.START_SHIFT
com.example.ubereatscompanion.TASKER.STOP_SHIFT
com.example.ubereatscompanion.TASKER.SET_MODE
```

## Offer broadcasts Tasker can listen for

```text
com.example.ubereatscompanion.TASKER.OFFER_EVALUATED
com.example.ubereatscompanion.TASKER.RECOMMEND_ACCEPT
com.example.ubereatscompanion.TASKER.RECOMMEND_MAYBE
com.example.ubereatscompanion.TASKER.RECOMMEND_DECLINE
```

## Extras sent to Tasker

```text
uec_recommendation
uec_score
uec_price
uec_distance_km
uec_pickup_distance_km
uec_minutes
uec_euro_per_km
uec_euro_per_hour
uec_battery_after
uec_pickup
uec_dropoff
uec_reasons
uec_raw_text
```

## Not included

This build intentionally does not include a ride-safe speed mode, because it was specifically not requested for this update.

This build also does not contain code that injects taps into Uber Eats.
