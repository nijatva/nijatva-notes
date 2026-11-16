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
- Appearance: System / Dark / Light / Black themes, Green / Saffron / Indigo accents
- About: developer card with live GitHub profile link

Not yet: import, grid view, biometric unlock, attachments, search index (FTS), Compose UI.

## Build

Standard Gradle project, no plugins beyond the Android Gradle Plugin.

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # unsigned; add your own signing config
```

Requires JDK 17 and the Android SDK with platform 34 and build-tools 34.0.0. Open the folder in Android Studio and it configures itself.

`build.sh` is a fallback that packages the same sources without Gradle (aapt2 → javac → d8 → zipalign → apksigner), for machines without Gradle or network access:

```sh
ANDROID_SDK_ROOT=~/Android/Sdk bash build.sh
```

The app has no dependencies — `app/build.gradle` has an empty `dependencies` block on purpose.

## Documents

- [`docs/SPEC.md`](docs/SPEC.md) — product and technical specification

## Licence

All rights reserved until a licence is added before the first release.
