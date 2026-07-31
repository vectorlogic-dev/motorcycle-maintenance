# MotoCare production audit

Audit date: 2026-07-31

## Result

MotoCare 1.0.4 passes the code, local-data, offline privacy, optimized-build, signing, emulator, upgrade, and automated-test gates. It is ready for its GitHub APK release. Google Play publishing still requires representative physical-device checks and listing configuration.

## Verified

- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`, `connectedDebugAndroidTest`, and the signed `assembleRelease` and `bundleRelease` builds pass.
- 19 JVM tests and 22 Android 17/API 37 emulator tests pass.
- The signed 1.0.4 APK and AAB verify successfully and use the same release certificate as 1.0.3.
- A signed `1.0.3 → 1.0.4` emulator upgrade preserved an existing motorcycle. A clean signed 1.0.4 install showed the empty-start onboarding with no sample data.
- Room schema versions 1 through 4 are committed, with tested non-destructive `1 → 2`, `2 → 3`, and `3 → 4` migrations.
- JSON restore accepts versions 1 through 4, upgrades legacy motorcycle and odometer rows, validates foreign keys and attachment owners, and rolls back the entire restore on failure.
- Initial and later odometer dates feed current mileage, dashboard, reports, cost-per-kilometre, reminders, and coverage forecasting through shared chronological calculations.
- Motorcycle creation commits the profile, starting odometer reading, and editable starter schedules in one database transaction.
- New motorcycles receive editable starter maintenance schedules tailored to their selected final-drive and cooling equipment.
- Equipment changes suggest only missing starter items and do not replace owner-edited schedules.
- Maintenance items can open a service record with the completed work already selected.
- Service records reject maintenance items belonging to another motorcycle, and editable receipt/media references are kept consistent when records change.
- Archived motorcycles can be restored or permanently deleted with confirmation.
- Financing can be edited or removed, paid dates can be recorded, and purchase/loan ownership costs use one calculation path in Dashboard and Reports.
- Reminder notifications open the relevant app destination and scheduling follows the local opt-in setting.
- Release code and resources are optimized with R8; the mapping file is generated for crash retracing.
- The merged manifest contains no network, broad storage, location, camera, microphone, or contacts permissions. An instrumentation test enforces this contract.
- Android cloud backup and device-to-device transfer are excluded; user-controlled JSON/CSV export remains available through the document picker.
- Reminders are opt-in, and the runtime notification permission is requested only after reminders are enabled.
- Adaptive, themed monochrome, and notification icons build successfully.
- Onboarding, Records, Backup, and bottom navigation were visually checked at 200% font scale and remained fully reachable without clipped actions.
- The motorcycle form and its initial-odometer calendar were smoke-tested on the Android 17/API 37 emulator.
- Fresh installations create no sample motorcycle or record data, and bottom navigation uses persistent selected-destination labels with compact large-text alternatives and full accessibility descriptions.
- Motorcycle-specific screens use consistent loading and empty states with a direct route to the garage, while multi-motorcycle dashboards use a full-width selector.
- Git commits use the configured human author and conventional commit subjects; no signing material or credentials are tracked.

## Non-blocking maintenance findings

- Lint reports dependency-update availability and a KSP migration suggestion. These are maintenance opportunities, not correctness or release errors; dependency upgrades should be handled as a separately tested change.
- The optimized signed APK, signed AAB, and matching R8 mapping file are generated for archival. Signing credentials remain in the release owner's macOS Keychain and are not stored in the repository.

## Release-owner gate

Complete every unchecked item in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md), especially:

- run TalkBack, light/dark, large-font, notification, backup/restore, and CSV smoke tests on representative physical devices, including an API 26 device or emulator;
- publish the privacy policy, complete Play data safety/content rating, provide support contact and store copy, and capture final screenshots;
- archive the signed AAB with its matching R8 mapping file and retain the signing keystore and Keychain credential securely.
