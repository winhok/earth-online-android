# Project instructions for coding agents

Earth Online is a Chinese, offline-first Android quest manager. Read README.md and verification/STATUS.md before claiming anything passed. This repository starts as a source release candidate, not an Android-verified release.

- Keep pure business rules in :core and Android I/O in :app.
- Room is the authoritative state. A completion record, not a mutable player balance, determines XP.
- Completion identity is questId + occurrence. Preserve reward/skill snapshots when undoing and redoing an occurrence.
- Mutate completion and audit events in one transaction. Never introduce fallbackToDestructiveMigration.
- Use the player's stored ZoneId for daily accounting; never rely on a process surviving midnight.
- Validate import entirely before replacement, and replace in one transaction. Never log private quest or note contents.
- Do not add network, AI, analytics, account, payment or cloud dependencies without an explicit product decision and privacy changes.
- Never fabricate screenshots, APK links, CI statuses, Room schemas, dependency hashes or passing tests.
- Existing core smoke command: bash scripts/test-core-offline.sh (local Kotlin CLI required).
- Full gate: ./gradlew :core:test :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleRelease.
- Instrumented gate on an isolated emulator: ./gradlew :app:connectedDebugAndroidTest. It resets debug-app data.
- Commit generated Room schemas only after an actual Android build; add real migrations before changing database version.
- Do not commit signing keys, tokens, local.properties, private backups or personal test data. Do not force-push or make the repository public without explicit authorization.
