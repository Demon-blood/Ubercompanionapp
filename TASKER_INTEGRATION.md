# Tasker integration

This project includes a Tasker-compatible automation bridge inspired by the type of integration couriers use with companion apps. It does **not** automate taps in Uber Eats. It exposes offer decisions to Tasker and accepts safe commands from Tasker.

## Required Tasker setting

In Tasker, enable:

- Preferences → Misc → Allow External Access

The app declares Tasker's run-task permission:

```xml
<uses-permission android:name="net.dinglisch.android.tasker.PERMISSION_RUN_TASKS" />
```

Tasker's public documentation says external apps can invoke Tasker tasks through broadcasts when this permission and external access are enabled.

## Broadcasts sent by this app

Every evaluated offer sends:

```text
com.example.ubereatscompanion.TASKER.OFFER_EVALUATED
```

It also sends one decision-specific event:

```text
com.example.ubereatscompanion.TASKER.RECOMMEND_ACCEPT
com.example.ubereatscompanion.TASKER.RECOMMEND_MAYBE
com.example.ubereatscompanion.TASKER.RECOMMEND_DECLINE
```

## Extras sent to Tasker

```text
uec_source
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

## Tasker profile example

Create a Tasker profile:

```text
Event → System → Intent Received
Action: com.example.ubereatscompanion.TASKER.RECOMMEND_ACCEPT
```

Then add Tasker actions such as:

```text
Say: Accept, %uec_price euros, %uec_euro_per_km euros per kilometer
Vibrate Pattern
Notify
Append to File
Launch App
```

## Commands Tasker can send to this app

Use Tasker → Send Intent with one of these actions:

```text
com.example.ubereatscompanion.TASKER.START_SHIFT
com.example.ubereatscompanion.TASKER.STOP_SHIFT
com.example.ubereatscompanion.TASKER.SPEAK_LAST_OFFER
com.example.ubereatscompanion.TASKER.OPEN_NAVIGATION
com.example.ubereatscompanion.TASKER.SET_MODE
com.example.ubereatscompanion.TASKER.CONFIGURE_TASKER
```

For SET_MODE, send string extra:

```text
uec_mode: normal
uec_mode: rain
uec_mode: heavy_rain
uec_mode: strict
```

For OPEN_NAVIGATION, optionally send:

```text
uec_nav_query: McDonald's Turnhout
```

If no query is provided, the app tries to open navigation to the last detected pickup.

## Configure named Tasker tasks from Tasker

Send intent action:

```text
com.example.ubereatscompanion.TASKER.CONFIGURE_TASKER
```

Optional extras:

```text
uec_tasker_enabled: true
uec_tasker_broadcast_events: true
uec_tasker_run_named_tasks: true
uec_tasker_task_accept: Your Accept Alert Task
uec_tasker_task_maybe: Your Maybe Alert Task
uec_tasker_task_decline: Your Decline Alert Task
```

When enabled, the app sends Tasker's `net.dinglisch.android.tasker.ACTION_TASK` broadcast with `task_name` and the offer extras.

## Android shell examples

These are useful for testing without Tasker:

```bash
adb shell am broadcast -a com.example.ubereatscompanion.TASKER.SET_MODE --es uec_mode rain
adb shell am broadcast -a com.example.ubereatscompanion.TASKER.START_SHIFT
adb shell am broadcast -a com.example.ubereatscompanion.TASKER.SPEAK_LAST_OFFER
adb shell am broadcast -a com.example.ubereatscompanion.TASKER.OPEN_NAVIGATION --es uec_nav_query "Grote Markt Turnhout"
```

## Confirmation workflow update

This package adds a confirmation-based Tasker workflow. Detected offers produce Tasker broadcasts and launch a full-screen confirmation screen with `I ACCEPTED`, `I DECLINED`, and `MAYBE / SKIPPED`. Confirmed choices are stored in Room and shown in the app. See `CONFIRMATION_TASKER_WORKFLOW.md`.

Ride-safe speed mode is not included in this update.
