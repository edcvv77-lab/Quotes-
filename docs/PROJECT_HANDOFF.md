# Project Handoff / Stop Point

This file is the mandatory handoff record for any developer or AI continuing the project.

## Current stop point

Date: 2026-08-31

Active development branch:
`ui/pro-architecture-redesign`

Branch base:
`d0cd18fda16a91ba60a8a20d0924025d379847d2`

Last verified build before the UI redesign:
- GitHub Actions run: **#226**
- result: **SUCCESS**
- source commit: `d0cd18fda16a91ba60a8a20d0924025d379847d2`

That build verified unit tests, split fixture generation, overlay application, APK builds, manifest metadata and permission-leak checks.

## What is proven

- Quotes host builds for Android 11 targetSdk 30.
- Private-space screen opens.
- Installed application enumeration works.
- clone picker now displays icons, real labels and search.
- split installed applications are staged as base + split APK cluster.
- BlackBox overlay contains split parsing/copy/resource/native-lib support.
- build CI creates and validates a real split fixture.
- broad BlackBox sample permissions are removed from the final host.
- user testing on Android 14 showed applications can be imported, but guest UI surfacing is inconsistent on Android 14.

## What is NOT yet proven

Do not claim any of the following as complete:

- full automated physical Android 11 acceptance;
- WhatsApp successfully launching and functioning on the target Android 11 tablet;
- guest survival across a real target-device reboot;
- final Settings/Apps visibility acceptance on the target tablet;
- stable in-place updates from GitHub CI (signing secrets are still missing);
- Android 14 guest compatibility.

## Current product acceptance target

The next decisive manual test is on the target Android 11 tablet:

1. install the current test build;
2. enter private space;
3. choose **نسخ تطبيق**;
4. clone one ordinary installed app;
5. tap its guest tile;
6. verify a visible guest UI actually appears;
7. repeat after closing/reopening Quotes;
8. then test a real split package;
9. only after that move to WhatsApp.

If a launch fails, collect the exact screen and Logcat where available. Do not retry blindly.

## Work currently in progress

The next phase is a professional UI redesign while preserving all runtime behavior.

Design objectives:
- premium, coherent Quotes identity;
- visually distinct private-space identity without exposing technical jargon;
- stronger information hierarchy;
- clearer action feedback;
- better phone and sw600dp tablet layouts;
- touch targets suitable for non-technical users;
- maintain classic XML Views for Android 11 stability;
- no engine/permission/targetSdk changes as part of visual work.

## Mandatory stop/update protocol

At the end of **every** development session, update this file before stopping.

Always record:

1. exact active branch;
2. latest commit SHA;
3. latest GitHub Actions run number and conclusion;
4. files changed;
5. behavior changed;
6. tests actually run and their result;
7. tests that were not possible;
8. current known bug(s);
9. exact next action;
10. whether an APK is safe only for testing or safe for long-term in-place updates.

Do not write "ready" merely because Gradle compiled.

## First files a new developer should read

In this order:

1. `docs/CURRENT_ARCHITECTURE.md`
2. this file
3. `app/src/main/java/com/aiham/privatespace/engine/VirtualEngine.kt`
4. `app/src/main/java/com/aiham/privatespace/engine/BlackBoxVirtualEngine.kt`
5. `app/src/main/java/com/aiham/privatespace/MainActivity.kt`
6. `blackbox-overrides/Bcore/...`
7. `.github/workflows/build-apk.yml`
8. `.github/workflows/android11-device-runtime.yml`
9. `docs/UPDATE_SIGNING.md`

## Never do these without explicit technical justification

- force push;
- rewrite `main` history;
- upgrade targetSdk because Android Studio suggests it;
- switch to the stale KTS/Compose build;
- run old `fix_*.sh` or `setup_*.sh` scripts against the active branch;
- edit the BlackBox submodule as the only copy of a fix;
- add `MANAGE_EXTERNAL_STORAGE` or `REQUEST_INSTALL_PACKAGES` as a shortcut;
- claim WhatsApp works before a real test;
- remove the VirtualEngine abstraction;
- distribute a CI debug-signed APK as the permanent tablet build.
