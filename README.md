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

Coverage details, registration and insurance records, problem logs, backup/export, reports, and settings UI remain in Phases 3–4. Disabled dashboard actions intentionally mark those boundaries.

## Build

Android Studio’s bundled JDK 17 and Android SDK 36 are supported.

```shell
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Maintenance intervals in sample data are deliberately blank editable templates. Users must confirm intervals against the motorcycle owner’s manual or dealer booklet.
