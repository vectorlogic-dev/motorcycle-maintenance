# MotoCare privacy

MotoCare is designed as an offline, single-device application.

## Data handling

- Motorcycle profiles, maintenance history, costs, document details, and preferences are stored locally on the device.
- MotoCare does not require an account and does not send data to a MotoCare server.
- MotoCare does not include analytics, advertising SDKs, or user tracking.
- Photos, videos, receipts, backups, and exports are selected or created through Android's system document picker. The database stores URI references rather than media binaries.
- Notifications are generated locally from the user's saved dates, mileage, and preferences.

## User control

The user controls backup and export destinations. Restoring a JSON backup replaces current MotoCare records only after an explicit confirmation. Uninstalling the app removes its private local database and preferences; files the user exported through the document picker remain in the location the user selected.

## Permissions

MotoCare requests notification permission on supported Android versions. It does not request broad storage access, location, contacts, camera, microphone, or network access.

This document describes version 0.1.0 and should be reviewed whenever dependencies or data handling change.
