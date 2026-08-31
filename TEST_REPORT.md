# Aiham Private Space / Quotes — Test Report

Date: 2026-08-30
Primary acceptance target: Android 11 / API 30

## Scope

This report distinguishes build-time/CI verification from runtime verification on a real Android device.
A green Gradle build is not treated as proof that FBlackBox guest virtualization works at runtime.

## Source and BlackBox revision

- Repository: `edcvv77-lab/Quotes-`
- Work branch: `phase2-runtime-fix`
- FBlackBox submodule revision is pinned and verified by CI:
  `823ffcbe8b94055ba68767c7422b4d174550fa86` (2.1.0)
- The submodule revision was not replaced or upgraded.

## Remote CI results

Verified on the runtime-fix branch:

- Real JVM unit-test task: PASS.
- CI verifies that an actual JUnit XML report exists (the task is no longer accepted when tests are `NO-SOURCE`).
- Host APK build: PASS.
- Guest APK build: PASS.
- Android instrumentation-test APK build: PASS.
- Embedded guest APK is compared byte-for-byte with the guest APK built in the same CI run: PASS.
- Host package metadata: `com.aiham.quotes`, minSdk 21, targetSdk 30, compileSdk 33: PASS.
- Guest package metadata: `com.aiham.virtualtest`, minSdk 21, targetSdk 30, compileSdk 33: PASS.
- Final host manifest contains FBlackBox `LauncherActivity` and proxy activities: PASS.
- CI verifies the pinned BlackBox commit before build.

A validated CI run after the runtime/manifest checks completed successfully:
GitHub Actions run `33276796382`.

## Unit-test coverage

The current JVM tests verify:

1. Normal quote creation is stored.
2. Blank input is rejected.
3. Exact `aiham77rk` opens the private flow and is not stored.
4. `aiham77rk ` (trailing space) does not trigger the private flow.
5. Case-changed input does not trigger the private flow.

The trigger comparison is intentionally exact; it no longer trims the input before checking the secret hash.

## BlackBox integration findings

The host uses the real FBlackBox APIs:

- `BlackBoxCore.doAttachBaseContext(...)`
- `BlackBoxCore.doCreate()`
- `installPackageAsUser(File, userId)`
- `getInstalledPackages(...)`
- `launchApk(...)`
- `uninstallPackageAsUser(...)`

The pinned FBlackBox 2.1.0 implementation can return an `InstallResult` whose default `success` remains true after some caught exceptions.
The host integration therefore no longer trusts `InstallResult.success` alone. It verifies the package through the BlackBox virtual package manager after installation.

Uninstall is also verified by querying BlackBox after the uninstall request.

## Secure storage hardening

The host now wraps the context passed into BlackBox so BlackBox virtual external storage resolves under the host's private internal data directory rather than `/storage/emulated/.../Android/data`.

Runtime initialization fails closed if either the BlackBox virtual root or virtual external root escapes the host private data directory or resolves under public `/storage`.

The Android instrumentation acceptance test now requires the BlackBox virtual external root to be inside the host private data directory and not under `/storage`.

The self-hosted Android 11 workflow additionally requires an `AIHAM_ENGINE` log proving `internal=true` and `publicExternal=false`.

## Guest APK pipeline

The generated guest APK is no longer committed as a source asset.

Gradle now:

1. builds `AihamVirtualTest`;
2. copies the generated APK to a generated build assets directory;
3. embeds that exact generated APK in the host;
4. CI extracts the embedded APK and compares it byte-for-byte with the current guest build.

Required artifacts:

- `Aiham-Quotes-debug.apk`
- `AihamVirtualTest-debug.apk`

Additional physical-device test artifact:

- `Aiham-Quotes-runtime-tests.apk`

## Runtime instrumentation test prepared

`BlackBoxRuntimeInstrumentedTest` performs the intended acceptance sequence on a compatible Android device:

1. verify BlackBox engine readiness;
2. extract the bundled guest APK;
3. install the guest through BlackBox;
4. verify it is present in the BlackBox virtual package list;
5. verify Android host PackageManager does not report it as an independent installed package;
6. request virtual launch;
7. uninstall through BlackBox;
8. verify it is absent from the BlackBox virtual package manager.

This test has been built by CI, but it has NOT been executed on a physical Android 11 device in the current remote environment.

## Hosted CI Android 11 ARM limitation

Three hosted-runner approaches were evaluated after the initial report:

1. GitHub macOS ARM64 + Android 11 ARM emulator:
   - Android Emulator started, then exited with `HVF error: HV_UNSUPPORTED`.
   - GitHub macOS ARM runners do not provide the nested virtualization required by the Android Emulator.

2. GitHub macOS ARM64 + `aosp_atd` + `-no-accel`:
   - The current emulator still attempted HVF and exited with the same `HV_UNSUPPORTED` error.

3. GitHub Linux ARM64 (`ubuntu-24.04-arm`):
   - The runner itself is real `aarch64`.
   - The Android Emulator host package is not published/available for this Linux ARM64 environment (`Failed to find package 'emulator'`).

Therefore hosted GitHub Actions cannot currently execute the required Android 11 ARM BlackBox runtime test for this project without changing the tested ABI/framework. The build CI remains limited to tests it can prove reliably.

A separate manual workflow, `.github/workflows/android11-device-runtime.yml`, is provided for a self-hosted runner connected to a real Android 11 ARM device. It performs checkout, build, installation, instrumentation, host PackageManager verification, and log capture.

## Physical Android 11 status

Status: PARTIALLY VERIFIED BY USER-PROVIDED DEVICE EVIDENCE.

The current tool environment still has no directly connected ARM Android 11 device, so automated ADB acceptance has not been executed by the assistant.

User-provided evidence from the target Android 11 tablet has verified:

- the Quotes host opens;
- the secret flow opens the private space;
- `com.aiham.virtualtest` installs inside BlackBox;
- the virtual guest launches far enough for the visible `Aiham Private Space Test` activity to appear.

Still requiring explicit Android 11 acceptance evidence:

- virtual uninstall and disappearance after uninstall;
- Android Settings -> Apps showing only the Quotes host and not the guest;
- `adb shell pm list packages` proving the guest is absent from the host package manager;
- the new internal-only storage root after the secure-storage hardening change.

### Android 11 test procedure

Install only the host APK as a normal Android package:

```bash
adb install -r Aiham-Quotes-debug.apk
adb shell pm list packages | grep com.aiham.quotes
```

Before virtual guest installation, confirm the guest is not a normal package:

```bash
adb shell pm list packages | grep com.aiham.virtualtest
```

Expected: no output.

Launch the host:

```bash
adb logcat -c
adb shell am start -n com.aiham.quotes/.MainActivity
```

Manual functional sequence:

- Add a normal Arabic quote and confirm it remains visible.
- Enter exactly `aiham77rk` through Add Quote.
- Confirm it opens the management screen and is not added to quotes.
- Install the bundled test application.
- Refresh/list virtual applications and confirm `com.aiham.virtualtest` is present.
- Launch the test application.
- While it is virtually installed, run:

```bash
adb shell pm list packages | grep com.aiham.virtualtest
```

Expected: no output.

Also check Android Settings -> Apps. The host `اقتباسات` is expected to be visible. The guest `Aiham Private Space Test` must not appear as an independently installed Android application.

Then uninstall the guest from the private screen and confirm it disappears from the virtual list.

Useful runtime logs:

```bash
adb logcat -s AIHAM_ENGINE AIHAM_GUEST AIHAM_SPACE
```

### Automated Android 11 acceptance test

Install the host and test APK:

```bash
adb install -r Aiham-Quotes-debug.apk
adb install -r Aiham-Quotes-runtime-tests.apk
```

Run:

```bash
adb shell am instrument -w com.aiham.quotes.test/androidx.test.runner.AndroidJUnitRunner
```

A passing instrumentation result is required before declaring the BlackBox runtime MVP complete.

## Android 11 compatibility notes

- Host minSdk: 21.
- Host targetSdk: 30.
- Host compileSdk: 33.
- Guest targetSdk: 30.
- FBlackBox native libraries in the host are ARM `armeabi-v7a` and `arm64-v8a`.
- Android 11 package visibility is handled explicitly for the test guest so the host PackageManager visibility assertion is meaningful.
- BlackBox proxy components and launcher component are checked in the merged final APK manifest by CI.

## Android 14 status

Status: NOT VERIFIED.

FBlackBox 2.1.0 dates from 2022 and relies on hidden APIs, reflection, native hooking, and OS internals.
Its compatibility helpers explicitly cover older Android releases and do not provide evidence of full Android 14 support.

The host can be built with targetSdk 30, but that does not prove BlackBox runtime compatibility on Android 14.
Android 14 installation, initialization, virtual launch, lifecycle, and restrictions must be tested separately on a real device. Any failure must be diagnosed from the exact Logcat exception instead of being hidden by changing targetSdk.

## Known limitations / remaining acceptance work

- A physical ARM Android 11 runtime test is still required.
- Android 14 runtime support is unproven.
- A successful `launchApk` return value proves that BlackBox accepted the launch request, not that the guest UI visibly completed startup; the physical-device instrumentation/manual check is the final proof.
- WhatsApp or other complex apps are outside the first acceptance test. They must not be attempted until `AihamVirtualTest` passes the complete Android 11 lifecycle test.
