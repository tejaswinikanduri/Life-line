# Every Day Counts — Android Live Wallpaper

This is an Android Studio project for a black lock-screen/home-screen live wallpaper with:
- large digital clock and date
- up to 3 editable goal names and target dates
- automatic daily countdown and progress percentage
- dot timeline with completed, current, and upcoming days
- refresh while the wallpaper is visible (every minute)

## Build and install
1. Open this folder in Android Studio (recent version).
2. Let Gradle sync and install the Android SDK platform 35 if prompted.
3. Connect an Android phone with USB debugging enabled, or use Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the generated APK on your phone.
5. Open **Every Day Counts**, enter up to three goals and target dates, and tap **SAVE GOALS**.
6. Tap **PREVIEW / SET LIVE WALLPAPER**, preview it, then choose Lock screen / Home screen as offered by your phone.

## Notes
- This is source code, not a prebuilt APK. It needs Android Studio/Android SDK to build.
- Android manufacturers vary: some phones may offer different wallpaper placement choices.
- Countdown data is stored locally in the app's SharedPreferences. No account or server is used.
- The day count starts on the day you save a new target date. Editing a target date starts a new progress timeline for that goal.
- The wallpaper updates once a minute while visible, so the clock refreshes and the date/countdown updates automatically.
