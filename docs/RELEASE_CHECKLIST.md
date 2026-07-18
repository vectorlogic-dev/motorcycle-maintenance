# MotoCare release checklist

## Quality

- Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`.
- Run `./gradlew connectedDebugAndroidTest` on an API 26+ emulator or device.
- Smoke-test onboarding with both sample and empty starts.
- Check light, dark, large-font, and TalkBack navigation on a physical device.
- Verify notification permission, channel settings, and reminder opt-outs.
- Create a backup, restore it, and compare motorcycle and record counts.
- Open every CSV export in a spreadsheet application.

## Store preparation

- Replace the preview launcher artwork with final adaptive icon assets.
- Set the release `versionCode` and `versionName`.
- Configure release signing outside source control.
- Generate a signed Android App Bundle and inspect it with Android Studio's APK Analyzer.
- Complete the Play data-safety form using `PRIVACY.md` and publish the privacy policy at a stable URL.
- Capture phone screenshots in light and dark themes.
- Confirm the app name, short description, full description, content rating, and support contact.

## Final safety checks

- Confirm no network, analytics, advertising, or broad storage permissions were introduced.
- Confirm Room migrations work from every previously released schema.
- Confirm backup restore is compatible with the release schema.
- Confirm sample maintenance schedules remain clearly labeled as editable templates.
