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

No Gradle. `./build.sh` runs aapt2 → javac → d8 → zipalign → apksigner and needs a JDK 17, build-tools 34.0.0 and platform android-34, with `ANDROID_SDK_ROOT` pointing at the SDK.

```sh
ANDROID_SDK_ROOT=~/Android/Sdk ./build.sh
```

Output: `build/nijatva-notes.apk`, signed with a local debug key. Replace it with your own release key before distributing anything.

## Download

A debug-signed build of version 0.2.0 sits in [`dist/nijatva-notes-0.2.0.apk`](dist/nijatva-notes-0.2.0.apk). Install with `adb install`, or open the file on the phone. It is signed with a throwaway key, so treat it as a test build.

## Documents

- [`docs/SPEC.md`](docs/SPEC.md) — product and technical specification

## Licence

All rights reserved until a licence is added before the first release.
