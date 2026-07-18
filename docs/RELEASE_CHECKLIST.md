# MotoCare release checklist

## Quality

- Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest bundleRelease`.
- Run `./gradlew connectedDebugAndroidTest` on an API 26+ emulator or device.
- Smoke-test onboarding with both sample and empty starts.
- Check light, dark, large-font, and TalkBack navigation on a physical device.
- Verify notification permission, channel settings, and reminder opt-outs.
- Create a backup, restore it, and compare motorcycle and record counts.
- Open every CSV export in a spreadsheet application.

## Store preparation

- Review the adaptive launcher and themed monochrome icon on representative OEM launchers.
- Confirm the release `versionCode` and `versionName` are higher than the last uploaded release.
- Configure release signing outside source control using all four Gradle properties:
  - `MOTOCARE_RELEASE_STORE_FILE`
  - `MOTOCARE_RELEASE_STORE_PASSWORD`
  - `MOTOCARE_RELEASE_KEY_ALIAS`
  - `MOTOCARE_RELEASE_KEY_PASSWORD`
- Keep those values in the release owner's Gradle user properties or CI secret store—never in this repository.
- Run `./gradlew bundleRelease`; an unsigned bundle is produced when the four properties are absent.
- Generate a signed Android App Bundle and inspect it with Android Studio's APK Analyzer.
- Archive `app/build/outputs/mapping/release/mapping.txt` with every release so obfuscated stack traces can be retraced.
- Complete the Play data-safety form using `PRIVACY.md` and publish the privacy policy at a stable URL.
- Capture phone screenshots in light and dark themes.
- Confirm the app name, short description, full description, content rating, and support contact.

## Final safety checks

- Confirm no network, analytics, advertising, or broad storage permissions were introduced.
- Confirm the automated merged-manifest permission test passes.
- Confirm Room migrations work from every previously released schema.
- Confirm backup restore is compatible with the release schema.
- Confirm sample maintenance schedules remain clearly labeled as editable templates.
