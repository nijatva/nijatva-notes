# Notes — Nijatva

Offline Markdown notes with an optional encrypted vault.

Part of the [Nijatva](https://vasudev-work.github.io) suite by **DEVIRA**: small, single-purpose Android apps built without Google Play Services, without accounts, without ads and without analytics.

## Status

Phase 1 scaffold: working prototype, debug-signed APK builds and installs. UI layer is framework Views in Java, not yet the Compose layer described in the spec.

## Scope

Offline Markdown notes with an optional encrypted vault.

## Permissions

`None (SAF for export)`

The manifest declares zero permissions. No `INTERNET`, no storage permission — export goes through `ACTION_CREATE_DOCUMENT`.

## The no-GMS rule for this app

SAF export only. No cloud sync, no Drive backup.

Every Nijatva app has to run on AOSP without Google Play Services, which means it also runs on de-Googled ROMs and on BharOS. If a feature cannot be built without GMS, the feature changes, not the rule.

## What is implemented

- Notes list: pinned and other sections, filter chips, search, sort by edited / created / title
- Editor: Markdown source, autosave, syntax toolbar, preview
- Notebooks: create, rename, delete (notes move to Default), counts
- Recently deleted: 30-day retention, restore, delete forever, empty
- Vault: passphrase setup, per-note lock with AES-256-GCM, auto-lock on background
- Export a note as `.md` through SAF
- Appearance and Developer screens, and the whole palette, come from nijatva-core's signature kit

Not yet: import, grid view, biometric unlock, attachments, search index (FTS), Compose UI.

## Build

Standard Gradle project. One dependency, from the suite's own library.

```sh
# once, in a nijatva-core checkout
./gradlew :signature:publishToMavenLocal

# then here
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK with platform 34 and build-tools 34.0.0.

## Shared with the rest of the suite

`com.nijatva:signature` (from [nijatva-core](https://github.com/vasudev-work/nijatva-core)) supplies the Appearance screen (theme and accent), the Developer card, and the palette itself. Notes keeps no copy of that code: `Ui.apply()` reads `SignatureTheme`, and the menu opens `Signature.appearance()` and `Signature.developer()`. The theme and accent the user picks therefore read the same here as in every other Nijatva app.

## Documents

- [`docs/SPEC.md`](docs/SPEC.md) — product and technical specification

## Licence

All rights reserved until a licence is added before the first release.
