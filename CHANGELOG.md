# Changelog

## Unreleased

## 1.0.4 - 2026-07-31

- Added an editable calendar date for each motorcycle's initial odometer reading
- Made current mileage and riding averages follow chronological history, including safe backdated readings
- Made motorcycle creation atomic and added a tested non-destructive Room 3 → 4 migration
- Added equipment-change suggestions for missing maintenance templates without overwriting owner edits
- Strengthened service-to-maintenance ownership validation and attachment replacement
- Added open and remove actions for saved motorcycle photos, receipts, and problem media
- Added archived motorcycle restore and confirmed permanent deletion
- Added financing edit, removal, payment-date entry, and protected installment-schedule rebuilding
- Unified cash purchase and financing costs across Dashboard and Reports
- Added notification deep links, clearer overdue wording, and reminder scheduling that follows the opt-in setting
- Hardened form validation and version-4 JSON backup snapshots and compatibility tests

## 1.0.3 - 2026-07-31

- Added research-backed, editable starter maintenance intervals for new and existing motorcycles
- Added a Record service action that opens the service form with the maintenance item preselected
- Added drive and cooling profiles so starter schedules include only relevant chain, belt, and coolant items

## 1.0.2 - 2026-07-19

- Replaced the horizontally scrolling Quick Actions strip with a fixed, compact Quick log grid
- Added a categorized All actions bottom sheet so every dashboard action remains visible without swiping
- Made Odometer, Fuel, Service, Expense, and Issue shortcuts open their add forms directly
- Changed the Parking shortcut to open Expenses instead of silently creating a parking record
- Added consistent loading and no-motorcycle states across motorcycle-specific screens
- Replaced transient bottom-navigation bubbles with native selected destination labels
- Replaced the motorcycle chip carousel with a full-width selector when multiple motorcycles exist
- Made Records and Backup tools scroll safely at large font sizes and compact screen heights

## 1.0.1

- Removed the bundled Honda Click125 sample so every fresh installation starts with an empty garage
- Simplified onboarding to one clear empty-start action
- Replaced wrapping bottom-navigation labels with evenly spaced icons and a brief animated destination label
- Preserved TalkBack destination descriptions and the selected-item visual indicator

## 1.0.0

First production-candidate release.

- All features from the four implementation phases, including editable history and calendar-based date entry
- Version-1 and version-2 JSON backup compatibility with transactional rollback on invalid restores
- Enforced offline permission contract and disabled Android cloud/device-transfer backup
- Optimized release bundle with external, source-control-safe signing configuration
- Consistent purchase-baseline riding averages across dashboard, reports, and coverage forecasts
- Opt-in reminders, large-font scroll support, adaptive launcher artwork, and a compliant notification icon

## 0.1.0

Initial offline preview release.

- Multiple motorcycle profiles, odometer history, and editable maintenance schedules
- Service, expense, fuel, parking, financing, registration, insurance, coverage, and issue records
- Dashboard reminders, six-month reports, JSON backup/restore, and CSV exports
- Configurable display, notification, and quick-entry preferences
- Dark theme, accessible status presentation, empty states, validation, and destructive-action confirmation

Maintenance templates are not manufacturer recommendations. Users must verify intervals against their owner's manual or dealer booklet.
