# Nijatva Notes — Product & Technical Specification

**App:** Notes (working title)
**Suite:** Nijatva by DEVIRA
**Repo:** `vasudev-work/nijatva-notes`
**Status:** Draft spec, pre-scaffold
**Date:** 29 October 2026

---

## 1. One-line definition

Offline Markdown notes for Android, with an optional encrypted vault and file-based export — no accounts, no network, no Google Play Services.

## 2. What this document is built from

Two inputs:

1. The repo README, which fixes the scope ("Offline Markdown notes with an optional encrypted vault"), the permission posture ("None (SAF for export)") and the no-GMS rule.
2. Four reference screenshots of an existing notes app, supplied as a UI reference. **The note titles and bodies in those screenshots are redacted**, so only the structure is usable: the list layout, the notebook drawer, the editor chrome and the sort menu. Nothing in this spec is inferred from the note content.

Where the reference app has a feature that conflicts with the no-GMS rule or with the "small, single-purpose" principle, the feature changes, not the rule — the same way the README frames it.

## 3. Position in the suite

`nijatva-core` lands first and supplies:

- the Compose design system (colour, type, components)
- SAF helper
- text-to-speech wrapper
- encrypted preferences
- backup/restore helper
- no-GMS base `Application` class

Notes is a **consumer** of core. It must not fork or duplicate any of those helpers. If Notes needs something core lacks, the change goes into core and is versioned there.

## 4. Non-goals for v1

Named explicitly so they do not creep in:

| Not doing | Why |
|---|---|
| Cloud sync, Drive backup, any server | No GMS, and no accounts. A note app that phones home is not this app. |
| To-dos as a separate feature | The reference app has a Notes / To-dos tab split. A checklist inside a note covers the need; a second data type doubles the model for little gain. Revisit after v1 ships. |
| Rich text / WYSIWYG editing | Notes are Markdown. One source of truth, greppable, portable, no proprietary format. |
| Collaborative editing, sharing links | Requires a server. |
| Ads, analytics, crash reporting SDKs | Suite rule. Diagnostics stay local. |
| Widgets, wear, tablet-specific layouts | Phase 2 at the earliest. |
| Automatic import from other note apps | Out of scope; a one-time Markdown folder import via SAF is a possible v1.1. |

## 5. Feature set

### 5.1 Notes list (reference screenshot 1)

- Header: app title, total note count ("65 notes"), search entry point, overflow menu.
- Filter row as chips: **All notes**, **Favourites**, **Quick note**, plus one chip per notebook.
- **Pinned** section above **Others**, collapsible.
- Each row: title (single line, ellipsised), a preview snippet, last-edited date, a small notebook/favourite indicator.
- List is virtualised and must stay smooth at several thousand notes.
- Floating action button creates a new note in the currently filtered notebook.
- Swipe actions: pin/unpin, favourite/unfavourite, delete (with undo snackbar).
- Empty state: one line of copy plus a "New note" button. No illustrations, no onboarding carousel.

**Sorting** (reference screenshot 4): by time created, or by time edited, with the active option ticked. The choice persists in encrypted preferences. A third option, title A–Z, is cheap to add and worth including.

**View modes:** list (default) and grid, as in the reference. Grid shows title plus first two lines only.

### 5.2 Notebooks (reference screenshot 2)

- All notes, then user-created notebooks with note counts.
- Two notebooks are **fixed and cannot be deleted**: `Quick note` (target for the quick-capture entry points) and `Favourites` (a saved filter, not a real folder — see below).
- `Default notebook` is the fallback for notes created without a chosen notebook, and is renameable but not deletable.
- **Locked notes** and **Recently deleted** are system containers, shown with a lock/trash glyph, not user notebooks.
- Create, rename, reorder (drag), delete a notebook. Deleting a notebook moves its notes to Default — it never deletes notes without a second explicit confirmation naming the count.

Design decision: **Favourites is a flag, not a notebook.** A note can be favourited and live in any notebook. The reference UI presents it in the notebook list, so it appears there too, but the data model keeps it as a boolean. Quick note *is* a real notebook.

### 5.3 Editor (reference screenshot 3)

- Top bar: back, undo, redo, overflow, **Done**.
- A metadata line under the top bar: last-edited timestamp and current notebook — tapping the notebook opens a picker.
- Title field, then body. Both plain text; title is the first `#` heading in the file, so the Markdown stays conventional.
- Formatting toolbar, horizontally scrollable, above the keyboard:
  - bold / italic / strikethrough
  - heading cycle (H1 → H2 → H3 → paragraph)
  - bulleted list, numbered list, checklist
  - quote, inline code, code block
  - link
  - horizontal rule
  - highlighter — implemented as `==text==`, and rendered only inside the app
  - attachment insert (image, via SAF picker)
- All toolbar actions insert Markdown syntax at the cursor. There is no hidden formatting layer.
- Preview toggle renders CommonMark + tables + task lists + `==highlight==`.
- Autosave: debounce 800 ms after typing stops, plus save on pause and on back. No explicit Save button — Done simply leaves the editor. A "Saved" / "Saving…" indicator sits in the metadata line.
- Undo/redo covers the current editing session only; it is not a document history.

### 5.4 Search

- Full-text search over titles and bodies, across all notebooks including locked ones **only after the vault is unlocked in that session**.
- Case- and diacritic-insensitive, substring matching with highlighted fragments in results.
- Uses SQLite FTS over the plaintext index. For encrypted notes the index is built at unlock time, in memory or in the encrypted database, and never written to plain storage.
- No search history, no recent queries stored.

### 5.5 Encrypted vault (the "Locked notes" container)

Threat model, stated plainly: this protects notes from someone holding the unlocked phone, from a lost device with no screen lock, and from casual file browsing. It does **not** protect against a determined attacker with the device unlocked and root access, and it is not a substitute for full-disk encryption.

- Vault is opt-in. Until the user sets a vault passphrase, the app stores notes as plain Markdown in app-private storage.
- Once enabled, notes marked *locked* have their bodies encrypted at rest. Titles of locked notes are also encrypted; the list shows an obfuscated title until unlock, so the container does not leak content by filename.
- Cipher: AES-256-GCM per note, random 96-bit nonce per write, authenticated with the note id as associated data.
- Key derivation: Argon2id (memory 64 MiB, iterations 3, parallelism 1) over the passphrase with a per-vault random salt. Fallback to PBKDF2-HMAC-SHA256 at 600k iterations only if Argon2 is unavailable on the target ABI.
- The derived key is wrapped by a key in the **Android Keystore** (hardware-backed where available) so the vault can optionally be unlocked with biometrics. Passphrase unlock always remains available; biometrics is a convenience layer, never the only route.
- Auto-lock: on screen off, on app background, and after a configurable idle timeout (default 5 minutes). Keys are zeroed from memory on lock.
- Failed-unlock throttling with escalating delay. No wipe-on-N-failures — destroying a user's notes over mistyped passphrases is a worse failure than the one it prevents. Offer it as an opt-in setting, clearly worded, default off.
- The vault passphrase is unrecoverable. The setup screen says so, once, in plain words, and requires the user to confirm they understand. No security questions, no email recovery — both would need the network and would weaken the model.

### 5.6 Recently deleted

- Deleted notes go to a trash container with the deletion timestamp.
- Retained 30 days by default, then purged on next app start.
- Restore or delete-forever per note; empty-trash action with confirmation.
- Notes in trash are not searchable.

### 5.7 Export and import (SAF only)

- **Export one note** as `.md`, or as `.txt` with Markdown stripped.
- **Export a notebook** as a `.zip` of `.md` files plus a `manifest.json` recording notebook name, note titles, timestamps and favourite flags.
- **Export everything** as the same zip at root level. Locked notes are excluded by default; if the user asks to include them, they are exported **decrypted** into a zip the user picks a destination for, behind a warning that names exactly what is being written.
- **Import** a folder or zip of `.md` files into a chosen notebook. Front-matter, if present, is read for title, created and modified dates; anything unrecognised is preserved in the body.
- Export uses `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT_TREE`. **No `MANAGE_EXTERNAL_STORAGE`, no broad storage permission.**

### 5.8 Backup and restore

- Manual, file-based, through the core backup helper: an encrypted archive written to a user-chosen SAF location.
- No scheduled background backup in v1 — a scheduled job that silently writes files is exactly the kind of thing that needs GMS-free `WorkManager` care and a lot of testing. It is a v1.1 candidate.

### 5.9 Quick capture

- A launcher shortcut (static, no GMS) that opens the editor directly into `Quick note`.
- `ACTION_SEND` / `ACTION_PROCESS_TEXT` intent filters so text shared from other apps lands as a new quick note. This requires no permission and no service.
- No quick-capture tile or widget in v1.

## 6. Data model

Two-layer: the database is an index; the Markdown file is the record.

```
Note
  id            TEXT (UUIDv4) PRIMARY KEY
  title         TEXT            -- mirror of first heading, for listing
  body          BLOB/TEXT       -- ciphertext when locked, else plaintext
  notebook_id   TEXT FK
  is_favourite  INTEGER
  is_locked     INTEGER
  is_pinned     INTEGER
  created_at    INTEGER (epoch ms)
  modified_at   INTEGER (epoch ms)
  deleted_at    INTEGER NULL    -- non-null while in trash
  file_path     TEXT            -- relative path of the .md file

Notebook
  id, name, position, created_at, is_system
```

On-disk layout, app-private storage:

```
files/notes/<notebook-slug>/<note-id>.md
files/attachments/<note-id>/<attachment-id>.<ext>
```

Rationale: the DB can be rebuilt by scanning the Markdown tree. A corrupted database costs the user their ordering and flags, not their words. Every write goes file-first, then index.

Plaintext `.md` files sit in app-private storage, which is not readable by other apps on a non-rooted device and is excluded from cloud auto-backup (`android:allowBackup="false"`, `dataExtractionRules` excluding the notes directory). If a user wants their notes off-device, that is an explicit SAF export — the app never does it behind their back.

## 7. Technical shape

- **Language:** Kotlin, no Java sources.
- **UI:** Jetpack Compose, Material 3, themed entirely from the core design system.
- **Architecture:** single-module app, MVVM with unidirectional data flow, `StateFlow` UI state, coroutines + Flow for storage. No DI framework in v1 — manual construction in the `Application` class is enough at this size and keeps the dependency list honest.
- **Persistence:** Room over SQLite; SQLCipher for the vault database. FTS4/FTS5 virtual table for search.
- **Markdown:** a maintained Kotlin CommonMark implementation for parse/render, wrapped in a thin adapter so it can be swapped. Editor is a `BasicTextField` over a `TextFieldValue` with a syntax-insertion helper — not a rich-text editor.
- **Min SDK:** 26 (Android 8.0). **Target SDK:** current at release. Compile against the latest stable.
- **No GMS:** no `play-services-*`, no Firebase, no `com.google.android.gms` dependency of any kind. Verify with a dependency-tree check in CI.
- **No `INTERNET` permission** in the manifest. This is the strongest statement the app can make about what it does with your notes, and it should be enforced by an assertion in CI, not by good intentions.
- **CI:** build + unit tests + a manifest permission assertion + a dependency scan for GMS artifacts. `./gradlew assembleDebug testDebugUnitTest` on every push.

## 8. Permissions

`None` — matching the README.

- No storage permissions: SAF gives per-URI access.
- No `POST_NOTIFICATIONS`: no notifications in v1.
- No `INTERNET`, no `ACCESS_NETWORK_STATE`.
- `USE_BIOMETRIC` only if the biometric unlock path ships, and it is a normal-level permission.

## 9. Accessibility and i18n

- Every control has a content description; the formatting toolbar is operable by TalkBack and switch access.
- Minimum touch target 48 dp; text scales to 200% without clipping — the editor and the metadata line are the risk points.
- Dynamic colour follows the system where available, overridable by the core theme.
- All user-facing strings in `strings.xml` from day one. English at launch; Hindi and Marathi next, since the suite's audience is Indian first.

## 10. Milestones

| Phase | Deliverable | Exit criteria |
|---|---|---|
| 0 | This spec, agreed | Scope and non-goals signed off |
| 1 | Scaffold: Gradle, Compose app, core dependency, CI with permission assertion | `assembleDebug` green on CI; empty app launches on AOSP emulator |
| 2 | Notes list, notebooks, editor with autosave, plain Markdown files | Create, edit, move, delete a note; survives process death |
| 3 | Search (FTS), favourites, pinning, sorting, trash | 5,000-note synthetic dataset stays under 100 ms for list scroll and search |
| 4 | Encrypted vault: Argon2id, AES-GCM, Keystore wrap, auto-lock | Crypto unit tests pass; no plaintext of a locked note anywhere on disk |
| 5 | SAF export/import, backup and restore | Round-trip a notebook out and back with metadata intact |
| 6 | Accessibility pass, i18n, release build | TalkBack and 200% font checks clean; APK with zero dangerous permissions |

Phase 1 of the suite build, as the README says — so this app is not competing with the other seven for a shared core that does not exist yet. Phase 0 is this document.

## 11. Open questions

1. **Application id and namespace.** `nijatva-core` does not declare one yet. Suggestion: `in.devira.nijatva.notes`, with core at `in.devira.nijatva.core`. Needs deciding before scaffold, because changing it later means a new install identity.
2. **App name shown to the user.** "Notes" is generic and will collide in launchers. "Nijatva Notes" is unambiguous but long on a home screen. Suggest "Nijatva Notes" for the store listing and label, accepting the truncation.
3. **Attachments.** Images in notes are in scope above, but they raise size, backup and encryption questions. Confirm, or cut to v1.1 and keep v1 text-only.
4. **Suite build order.** Which of the eight apps is Phase 2? Notes is a good candidate — it exercises core's SAF, encrypted preferences and design system at once — but that is the tracker's call, not this document's.
5. **Licence.** README says "all rights reserved until a licence is added before the first release". If the suite is going to be open source, the licence choice affects whether core can be a published library. Decide before the first public commit of code.

---

*Sources: `vasudev-work/nijatva-notes` README; `vasudev-work/nijatva-core` README; four reference screenshots of an existing notes app supplied by the repo owner (note content redacted, UI structure only).*
