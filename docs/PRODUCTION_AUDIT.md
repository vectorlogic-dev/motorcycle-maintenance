# MotoCare production audit

Audit date: 2026-07-31

## Result

MotoCare 1.0.3 is ready for its GitHub APK release. The codebase, local-data flows, offline privacy contract, optimized release build, and automated tests pass. Google Play publishing still requires representative physical-device checks and listing configuration.

## Verified

- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`, `connectedDebugAndroidTest`, and `bundleRelease` pass.
- 14 JVM tests and 14 API 37 emulator tests pass.
- The signed 1.0.3 APK and AAB use the same release certificate as the previous releases; APK signature verification and AAB JAR verification pass.
- Emulator upgrades through signed 1.0.3 succeeded without clearing app data; a clean install showed the empty-start onboarding with no sample action.
- Room schema versions 1, 2, and 3 are committed, with tested non-destructive `1 → 2` and `2 → 3` migrations.
- JSON restore accepts versions 1 through 3, upgrades legacy motorcycle rows, validates foreign keys, and rolls back the entire restore on failure.
- Odometer purchase baselines feed dashboard, reports, cost-per-kilometre, and coverage forecasting through one shared calculation path.
- New motorcycles receive editable starter maintenance schedules tailored to their selected final-drive and cooling equipment.
- Maintenance items can open a service record with the completed work already selected.
- Release code and resources are optimized with R8; the mapping file is generated for crash retracing.
- The merged manifest contains no network, broad storage, location, camera, microphone, or contacts permissions. An instrumentation test enforces this contract.
- Android cloud backup and device-to-device transfer are excluded; user-controlled JSON/CSV export remains available through the document picker.
- Reminders are opt-in, and the runtime notification permission is requested only after reminders are enabled.
- Adaptive, themed monochrome, and notification icons build successfully.
- Onboarding, Records, Backup, and bottom navigation were visually checked at 200% font scale and remained fully reachable without clipped actions.
- Fresh installations create no sample motorcycle or record data, and bottom navigation uses persistent selected-destination labels with compact large-text alternatives and full accessibility descriptions.
- Motorcycle-specific screens use consistent loading and empty states with a direct route to the garage, while multi-motorcycle dashboards use a full-width selector.
- Git commits use the configured human author and conventional commit subjects; no signing material or credentials are tracked.

## Non-blocking maintenance findings

- Lint reports dependency-update availability and a KSP migration suggestion. These are maintenance opportunities, not correctness or release errors; dependency upgrades should be handled as a separately tested change.
- The optimized signed APK, signed AAB, and matching R8 mapping file are generated for archival with the release.

## Release-owner gate

Complete every unchecked item in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md), especially:

- run TalkBack, light/dark, large-font, notification, backup/restore, and CSV smoke tests on representative physical devices, including an API 26 device or emulator;
- publish the privacy policy, complete Play data safety/content rating, provide support contact and store copy, and capture final screenshots;
- archive the signed AAB and matching R8 mapping file for the release.
