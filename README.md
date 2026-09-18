# Nightstand

A bedside clock for Android, inspired by the "standby" idea: turn your phone on its side and get a big
clock on the left and a quiet, scrollable list of your notifications on the right.

Version 0.1 is designed for 720 × 1600 phones such as the **Samsung Galaxy A12** (an 800 × 360 dp
landscape window).

## What it does (v0.1)

- **Clock side:** large digital clock (follows your phone's 12/24-hour setting), weekday, date and month,
  next alarm, and battery / charging level.
- **Notification side:** every notification on the phone, newest first. Tap to open it (unlocks first if
  needed), swipe or tap × to dismiss it, or **Clear all**. Dismissing here also removes it from the phone's
  notification shade.
- **Pop-ups off while open:** Nightstand switches on its own Do Not Disturb mode while it is on screen, so
  notifications appear in the app instead of popping up. **Alarms, phone calls and media still come
  through.** Your own Do Not Disturb settings are put back exactly as they were when you leave, and are
  restored even if the app crashes.
- **Always landscape:** opens with the charging port on the right and stays that way until you close it.
- **Screen stays on**, system bars are hidden, and it can show over the lock screen.
- **Hold to exit:** the back gesture is disabled; press and hold the exit button for about a second.
- **Burn-in protection:** the clock moves a few pixels as the minutes change.

## First run

The app asks for two permissions and links to the right settings page for each:

1. **Notification access** (required) — so it can read and dismiss notifications.
2. **Do Not Disturb access** (optional, you can skip) — so it can silence pop-ups while open.

On Samsung phones these live under *Settings → Notifications → Advanced settings*
(*Notification access* / *Do Not Disturb permission*) if the button doesn't take you straight there.

## Building

1. Install [Android Studio](https://developer.android.com/studio) (any 2025 or newer release).
2. **File → Open…** and choose this folder. Let Gradle sync (first sync downloads the Android SDK pieces).
3. On the phone: *Settings → About phone → Software information* → tap **Build number** 7 times, then turn on
   *Developer options → USB debugging*.
4. Plug the phone in and press **Run ▶**.

Command line: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## Roadmap

- Night mode (dim red display after dark)
- Now-playing music controls
- Privacy mode (hide message text)
- Auto-start when charging (Android screen saver / "dream")
- Other screen sizes

## F-Droid

The project is set up to meet F-Droid's inclusion rules from the start: GPL-3.0 licence, only open-source
dependencies (AndroidX / Jetpack Compose), no Google Play Services, analytics, ads or tracking, the
dependency-info signing block disabled, and store text in `fastlane/metadata/android/`.

## Licence

GPL-3.0-or-later. See [LICENSE](LICENSE).
