# MotoCare

MotoCare is an Android-first, offline motorcycle maintenance and ownership tracker built with Kotlin and Jetpack Compose.

## Implemented

- Optional Honda Click125 Smart Edition sample setup or an empty start
- Multiple motorcycle profiles with archive support and masked optional identifiers
- Manual odometer history, validation, explicit correction confirmation, and riding-rate summaries
- Editable maintenance schedules with mileage and time triggers evaluated as “whichever comes first”
- Dashboard for odometer, due/overdue maintenance, free-maintenance coverage, and basic financing context
- Daily WorkManager maintenance checks and Android notification channel
- Version-1 Room schema for the planned ownership records, URI-only attachments, Hilt, repositories, Flow, and DataStore defaults
- Service history with receipt references and transactional maintenance-schedule updates
- Expense tracking with daily, monthly, and annual summaries plus configurable one-tap parking
- Fuel logs, monthly spending, and economy calculated only across valid full-tank intervals
- Financing schedules with payment statuses, rebates, balances, due dates, and payoff estimates
- Dashboard ownership totals, cost-per-kilometre figures, and active Phase 2 quick actions
- Purchase date/type/price, seller, and second-hand ownership details
- Coverage forecasting with upcoming services before the recorded limit
- Registration, insurance, document references, and expiry reminders without legal conclusions
- Problem and symptom tracking with resolution history and media URI references
- Storage Access Framework JSON backup/restore and CSV exports without broad storage permissions
- Accessible six-month cost and distance reports with native Compose charts
- Room schema v2 with a tested, non-destructive `1 → 2` migration
- Settings for theme, date format, currency display, notification preferences, per-motorcycle reminder opt-outs, and quick-entry defaults
- Repository, migration, and critical-screen instrumentation coverage
- Offline privacy documentation and a Play Store release checklist

Phases 1–4 of the initial MotoCare brief are implemented. The project is an unsigned preview build; final adaptive artwork, store assets, release signing, and physical-device accessibility checks remain release-owner tasks.

## Build

Android Studio’s bundled JDK 17 and Android SDK 36 are supported.

```shell
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
```

Maintenance intervals in sample data are deliberately blank editable templates. Users must confirm intervals against the motorcycle owner’s manual or dealer booklet.

See [PRIVACY.md](PRIVACY.md) for local data handling and [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) for release preparation.
