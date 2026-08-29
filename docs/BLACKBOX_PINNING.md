# BlackBox Pinning

## Purpose

Aiham Private Space integrates FBlackBox as a source submodule so a clean checkout can reproduce the same virtualization-engine source revision.

## Previous broken reference

The repository previously pinned `BlackBox` to commit `f1b366d702032c9813167e844231131a6f2c7a49`. Jules could not recursively clone the repository because the public FBlackBox repository did not provide that object to the clone operation:

```text
fatal: remote error: upload-pack: not our ref f1b366d702032c9813167e844231131a6f2c7a49
```

That reference was removed from `main` before this repair.

## Current reference

BlackBox is pinned to the public `2.1.0` tag.

- Repository: `https://github.com/FBlackBox/BlackBox.git`
- Tag: `2.1.0`
- Commit: `823ffcbe8b94055ba68767c7422b4d174550fa86`
- Tag ref: `refs/tags/2.1.0`

The tag and commit were verified against the public FBlackBox repository before restoring the submodule.

## Submodule configuration

`.gitmodules` contains:

```ini
[submodule "BlackBox"]
\tpath = BlackBox
\turl = https://github.com/FBlackBox/BlackBox.git
```

The parent repository records the exact BlackBox commit as a Git submodule (gitlink), rather than relying on a moving branch.

## Reproducible checkout

Use:

```bash
git clone --recursive https://github.com/edcvv77-lab/Quotes- Aiham-Private-Space
cd Aiham-Private-Space
git submodule status
```

The `BlackBox` directory must be populated and the submodule must resolve to:

```text
823ffcbe8b94055ba68767c7422b4d174550fa86
```

If the repository was already cloned without submodules:

```bash
git submodule sync --recursive
git submodule update --init --recursive
```

## Android 11 assessment

FBlackBox `2.1.0` documents Android support from Android 5.0 through Android 12. The Aiham Private Space host continues to target API 30 (Android 11). This establishes source-level compatibility for the selected engine revision, but it does not by itself prove that every guest application will run correctly on every Android 11 device.

The project must separately validate runtime behavior on the target Android 11 hardware, especially for applications that depend on Google Play services, device integrity checks, background services, notifications, storage, or other system integrations.

## Build requirements

The host project currently targets:

- compileSdkVersion: 30
- targetSdkVersion: 30
- minimum SDK: 21
- Java: 11 for the build environment used to validate the BlackBox integration
- NDK: 21.0.6113669 where required by the current BlackBox build configuration

The exact Gradle/Android Gradle Plugin versions remain defined by the repository build files and must be used together with the selected SDK/NDK versions.

## Integration API

The current `BlackBoxVirtualEngine` uses the BlackBox APIs for initialization, installation, listing, launching, and uninstalling. The integration must be validated against the checked-out `2.1.0` source rather than assuming API compatibility from the tag name alone.

## Important limitation

A successful Gradle build proves that the source and build configuration are compatible. It does **not** prove complete runtime isolation or compatibility of arbitrary guest applications. Runtime validation on an actual Android 11 device is required before declaring the MVP production-ready.
