# MotoCare production audit

Audit date: 2026-07-19

## Result

MotoCare 1.0.0 is a production release candidate. The codebase, local-data flows, offline privacy contract, optimized release build, and automated tests pass. Publishing still requires the release owner's signing identity, Play listing configuration, and representative physical-device checks.

## Verified

- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`, `connectedDebugAndroidTest`, and `bundleRelease` pass.
- 12 JVM tests and 11 API 37 emulator tests pass.
- Room schema versions 1 and 2 are committed, with a tested non-destructive `1 → 2` migration.
- JSON restore accepts versions 1 and 2, upgrades legacy motorcycle rows, validates foreign keys, and rolls back the entire restore on failure.
- Odometer purchase baselines feed dashboard, reports, cost-per-kilometre, and coverage forecasting through one shared calculation path.
- Release code and resources are optimized with R8; the mapping file is generated for crash retracing.
- The merged manifest contains no network, broad storage, location, camera, microphone, or contacts permissions. An instrumentation test enforces this contract.
- Android cloud backup and device-to-device transfer are excluded; user-controlled JSON/CSV export remains available through the document picker.
- Reminders are opt-in, and the runtime notification permission is requested only after reminders are enabled.
- Adaptive, themed monochrome, and notification icons build successfully.
- Onboarding was visually checked at 200% font scale and remained fully reachable by scrolling.
- Git commits use the configured human author and conventional commit subjects; no signing material or credentials are tracked.

## Non-blocking maintenance findings

- Lint reports dependency-update availability and a KSP migration suggestion. These are maintenance opportunities, not correctness or release errors; dependency upgrades should be handled as a separately tested change.
- The optimized unsigned AAB is roughly 4.5 MB. The release owner must produce the final signed bundle using the documented Gradle properties.

## Release-owner gate

Complete every unchecked item in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md), especially:

- provide the upload keystore through secure Gradle/CI properties and verify the signed AAB;
- run TalkBack, light/dark, large-font, notification, backup/restore, and CSV smoke tests on representative physical devices, including an API 26 device or emulator;
- publish the privacy policy, complete Play data safety/content rating, provide support contact and store copy, and capture final screenshots;
- archive the signed AAB and matching R8 mapping file for the release.
