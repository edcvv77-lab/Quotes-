# Project Handoff / Stop Point

This file is the mandatory handoff record for any developer or AI continuing the project.

## Current stop point

Date: 2026-08-31

Active development branch:
`ui/pro-architecture-redesign`

Latest code/config commit verified by CI:
`ac455e76029c77957609a52f9d0c41e2b82c7153`

Latest verified GitHub Actions run:
- workflow: **Build Aiham Private Space**
- run: **#263**
- result: **SUCCESS**
- source commit: `ac455e76029c77957609a52f9d0c41e2b82c7153`

The build verified:
- real JVM unit tests;
- private BlackBox overlay application;
- host APK build;
- guest APK build;
- instrumentation APK build;
- real split-fixture generation from the AAB for an API 30 tablet device spec;
- embedded guest consistency;
- targetSdk 30/package metadata;
- BlackBox proxy components;
- forbidden permission leak checks.

Stable-signing steps were skipped because GitHub signing secrets are still not configured. The generated host artifact remains test-only and must not be treated as the permanent in-place update APK.

## What changed in this phase

### Architecture / developer continuity
Added:
- `docs/CURRENT_ARCHITECTURE.md`
- `docs/PROJECT_HANDOFF.md`
- `docs/UI_REDESIGN_PLAN.md`
- `AGENTS.md`

Updated:
- `README.md` now points new developers to the actual architecture and handoff docs.
- build CI now accepts `ui/**` branches.
- build CI ignores documentation-only pushes so the handoff file can be updated without cancelling a code build.

### Quotes interface redesign
Phone and sw600dp tablet layouts were redesigned while preserving existing view IDs and behavior.

Implemented:
- warmer editorial background/surface system;
- deep evergreen + muted brass brand palette;
- premium gradient masthead;
- decorative quote mark treatment;
- clearer title/subtitle hierarchy;
- improved quote-count chip;
- search field with dedicated search icon;
- primary `اقتباس جديد` action;
- secondary favorites action;
- clearer list section marker;
- quote cards with accent rail, larger readable text and grouped action controls;
- improved add/edit quote editor styling;
- tablet content width increased and centered for better visual balance.

### Private-space interface redesign
Phone and sw600dp tablet layouts were redesigned without changing engine code or permissions.

Implemented:
- midnight/emerald secure-space visual identity;
- secure header with shield indicator;
- stronger operational status card;
- **نسخ تطبيق من الجهاز** promoted to the primary action;
- `إضافة APK` and refresh demoted to secondary actions;
- cleaner guest-app panel and larger guest tiles;
- icon plates and clearer guest labels;
- compact usage hint;
- visually separated lock/return control;
- tablet layout uses wider content and a four-column guest grid.

### Installed-app clone picker
Kept the existing functional improvements and polished the presentation:
- real application icons;
- real labels;
- package ID as low-emphasis metadata;
- alphabetical ordering from the existing manager;
- search;
- visible result count;
- rounded modal surface;
- improved row spacing and icon plate;
- dialog width capped for tablets and window background made transparent.

### Design-system resources
Added/updated:
- `res/values/colors.xml`
- `res/values/styles.xml`
- `res/values/themes.xml`
- branded button styles;
- quotes card/search/action backgrounds;
- private-space root/header/status/panel/tile surfaces;
- picker surfaces;
- custom vector icons for search, add, favorites, clone, APK, refresh, lock, shield and navigation.

### Hadeel private-space personalization

Implemented after the first professional redesign:
- only the private-space screen is personalized for Hadeel;
- blush pink, lavender, cream and rose-gold palette;
- `مساحة هديل الخاصة` title, crown and Hadeel name sticker;
- butterfly, sparkle and flower vector decorations;
- soft Hadeel guest cards and matching installed-app picker;
- private-only status/navigation bar colors;
- staggered private-screen entrance and guest-card motion;
- decorative animation is cancelled when leaving private space;
- Quotes home visuals remain unchanged;
- no BlackBox, permission, package-ID or Android API-target change.

## What is proven

- Current branch compiles at code commit `ac455e76029c77957609a52f9d0c41e2b82c7153`.
- GitHub Actions run **#263** completed **SUCCESS**.
- Unit tests pass.
- All phone and sw600dp XML resources compile.
- MaterialButton styles and new vector/shape resources compile.
- Split APK build/overlay checks remain intact after the visual redesign.
- Final manifest metadata and forbidden permission checks still pass.
- No targetSdk/minSdk/package/engine behavior was changed by the redesign.

## What is NOT yet proven

Do not claim any of the following as complete:

- visual correctness on the target Android 11 tablet;
- full automated physical Android 11 runtime acceptance;
- WhatsApp successfully launching and functioning on the target Android 11 tablet;
- guest survival across a real target-device reboot;
- final Settings/Apps visibility acceptance on the target tablet;
- stable in-place updates from GitHub CI;
- Android 14 guest compatibility.

## Current product acceptance target

The next decisive test is on the target Android 11 tablet:

1. install the current test build;
2. open Quotes and confirm the new phone/tablet visual layout renders correctly;
3. enter the private space;
4. confirm the redesigned private-space UI fits the screen without clipping;
5. tap **نسخ تطبيق من الجهاز**;
6. confirm app icons, labels, search and result count render correctly;
7. clone one ordinary installed app;
8. tap the guest tile;
9. verify a visible guest UI actually appears;
10. repeat after closing/reopening Quotes;
11. then test a real split package;
12. only after that move to WhatsApp.

If launch fails, capture the exact screen and Logcat where available. Do not retry blindly.

## Current known technical debt

1. Android 11 physical runtime acceptance is still open.
2. WhatsApp acceptance is still open.
3. Android 14 guest UI surfacing is unreliable and is not the primary target.
4. `MainActivity` still owns too many responsibilities; defer major refactor until Android 11 runtime is accepted.
5. duplicate KTS build files remain stale and dangerous.
6. old `fix_*.sh` / `setup_*.sh` scripts remain in the repository and must not be run on the active project.
7. several older docs remain historically stale; `CURRENT_ARCHITECTURE.md` is the current source of truth.
8. stable signing secrets are still missing from GitHub Actions.

## Exact next action

Do **not** redesign the engine or change Android API levels next.

Next action:
**run a visual + functional acceptance pass on the target Android 11 tablet using the new UI build, then record the result here.**

If the UI is visually accepted but guest launch fails, return to runtime diagnosis only. If the UI clips or scales poorly, adjust the `sw600dp` layouts without touching BlackBox.

## APK safety classification

Current CI host artifact:
`Aiham-Quotes-ci-debug-NOT-FOR-UPDATES`

Classification:
**TEST ONLY**

Reason:
GitHub Actions does not currently have the permanent signing secrets. Do not use this artifact as the long-term tablet update stream.

## Mandatory stop/update protocol

At the end of **every** development session, update this file before stopping.

Always record:

1. exact active branch;
2. latest code/config commit SHA;
3. latest GitHub Actions run number and conclusion;
4. files changed;
5. behavior changed;
6. tests actually run and their result;
7. tests that were not possible;
8. current known bug(s);
9. exact next action;
10. whether an APK is test-only or safe for long-term in-place updates.

Do not write "ready" merely because Gradle compiled.

## First files a new developer should read

In this order:

1. `docs/CURRENT_ARCHITECTURE.md`
2. this file
3. `docs/UI_REDESIGN_PLAN.md`
4. `app/src/main/java/com/aiham/privatespace/engine/VirtualEngine.kt`
5. `app/src/main/java/com/aiham/privatespace/engine/BlackBoxVirtualEngine.kt`
6. `app/src/main/java/com/aiham/privatespace/MainActivity.kt`
7. `blackbox-overrides/Bcore/...`
8. `.github/workflows/build-apk.yml`
9. `.github/workflows/android11-device-runtime.yml`
10. `docs/UPDATE_SIGNING.md`

## Never do these without explicit technical justification

- force push;
- rewrite `main` history;
- upgrade targetSdk because Android Studio suggests it;
- switch to the stale KTS/Compose build;
- run old `fix_*.sh` or `setup_*.sh` scripts against the active branch;
- edit the BlackBox submodule as the only copy of a fix;
- add `MANAGE_EXTERNAL_STORAGE` or `REQUEST_INSTALL_PACKAGES` as a shortcut;
- claim WhatsApp works before a real test;
- remove the `VirtualEngine` abstraction;
- distribute a CI debug-signed APK as the permanent tablet build.
