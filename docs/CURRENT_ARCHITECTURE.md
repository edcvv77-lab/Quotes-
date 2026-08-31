# Current Architecture — Quotes / Aiham Private Space

Last audited: 2026-08-31
Primary runtime target: Android 11 / API 30
Host package: `com.aiham.quotes`
Current virtualization engine: FBlackBox 2.1.0 pinned at `823ffcbe8b94055ba68767c7422b4d174550fa86`

## 1. Product boundary

The repository contains one user-facing Android application with two intentionally different experiences:

1. **Quotes host**
   - Normal visible application named `اقتباسات`.
   - Stores, searches, edits, favorites, copies and shares quotes.
   - A hidden trigger routes into the private-space screen.

2. **Private space**
   - Lets the user import a normal installed Android app or a standalone APK.
   - Runs guest applications through FBlackBox without installing the guest as a normal Android package.
   - The acceptance target is that Android Settings / launcher see the Quotes host only, while guest application files and data remain owned by the Quotes application.

The private space is not a system work profile and does not have Knox/Device Owner privileges.

## 2. Build source of truth

The active build is **Groovy Gradle**, not the Kotlin DSL files.

Authoritative files:
- `build.gradle`
- `settings.gradle`
- `app/build.gradle`
- `AihamVirtualTest/build.gradle`

Stale legacy files still present and **must not be treated as source of truth**:
- `build.gradle.kts`
- `settings.gradle.kts`
- `app/build.gradle.kts`
- old setup/fix shell scripts
- `.github/workflows/build.yml` (manual legacy workflow)

Current host build contract:
- compileSdk 33
- targetSdk 30
- minSdk 21
- Java/Kotlin bytecode target 11
- ABIs: arm64-v8a and armeabi-v7a
- package: `com.aiham.quotes`

Do not silently migrate this project to Compose, AGP 8, targetSdk 34, or a different package. The stale KTS files describe an abandoned path and conflict with the production Android 11 target.

## 3. Modules and ownership

### `:app`
User-facing Quotes application and private-space orchestration.

Important code:
- `AihamApp.kt` — initializes BlackBox during Application attach/create.
- `MainActivity.kt` — currently owns both Quotes UI and private-space UI orchestration.
- `QuoteService.kt` — quote business rules.
- `QuoteStore.kt` — SharedPreferences persistence and migration.
- `SecurityTrigger.kt` — exact hidden-trigger hash comparison.
- `apps/GuestApkInspector.kt` — copies/inspects standalone APK files.
- `apps/InstalledAppCloneManager.kt` — enumerates importable installed apps, labels, icons and split paths.
- `apps/GuestAppCatalog.kt` — stores guest labels/icons for the private-space UI.
- `permissions/GuestPermissionManager.kt` — maps guest dangerous permissions to permissions declared by the Quotes host.
- `engine/VirtualEngine.kt` — engine abstraction.
- `engine/BlackBoxVirtualEngine.kt` — FBlackBox implementation and runtime verification.
- `storage/StorageIsolationPolicy.kt` — path normalization/containment checks.

### `:AihamVirtualTest`
Controlled guest fixture used by CI/instrumentation. It is deliberately separate from the host and is also built as an Android App Bundle to produce real split APKs.

### `BlackBox` submodule
Pinned upstream FBlackBox source. Never edit the submodule directly as the durable source of project-specific changes.

### `blackbox-overrides/Bcore`
Project-owned overlay copied over the pinned BlackBox source by the Gradle `applyBlackBoxOverlay` task. This is the durable location for BlackBox modifications.

Current overlay responsibilities:
- split APK cluster parsing;
- private copying of base + split APKs;
- split resource loading;
- split native library extraction;
- private split paths in virtual `ApplicationInfo`;
- app-scoped virtual external storage;
- proxy activity recents suppression;
- pruned BlackBox sample permissions.

## 4. Runtime data flow

### Quotes flow

`MainActivity -> QuoteService -> QuoteStore -> SharedPreferences`

Rules:
- blank quote rejected;
- hidden trigger opens private space and is not stored;
- edit path cannot save the hidden trigger;
- favorites persist independently.

### Installed-app clone flow

`MainActivity`
→ `InstalledAppCloneManager.listCloneCandidates()`
→ user selects package
→ `GuestPermissionManager.buildPlan(base.apk)`
→ request only missing dangerous permissions that the host itself declares
→ `BlackBoxVirtualEngine.installHostPackage(packageName)`
→ copy host `base.apk + splitSourceDirs` to a private staging cluster
→ BlackBox cluster parser
→ copy base + splits into BlackBox private app directory
→ verify virtual package manager sees the package
→ verify virtual base and every split path are under the private BlackBox app directory
→ save label/icon in `GuestAppCatalog`
→ render guest tile.

### Standalone APK flow

Document picker
→ copy selected URI to host cache
→ inspect APK metadata
→ permission plan
→ `installApk(File)`
→ verify virtual package presence
→ save label/icon
→ delete temporary APK.

A single standalone `.apk` file is supported through this path. Installed split applications should use **نسخ تطبيق** so every installed split can be captured.

### Launch flow

Guest tile
→ `BlackBoxVirtualEngine.launchApp()`
→ resolve virtual launch intent
→ Android 11 and older use the direct BlackBox activity-manager path
→ newer Android currently use the BlackBox launcher trampoline with fallback
→ virtual-process startup is verified.

Important: a running guest process is not proof that a visible guest Activity successfully surfaced. UI feedback now distinguishes process start from visible host transition, but Android 11 physical acceptance remains authoritative.

## 5. Storage and visibility model

- Virtual internal root is under the Quotes host app data directory.
- Virtual external root is app-scoped under Quotes external files.
- Imported split APKs are copied into BlackBox-owned private app storage.
- Guest packages are expected to be absent from the host Android PackageManager.
- Android Settings may still show permissions/storage usage under **Quotes**, because the host owns the real Linux UID/package.
- Absolute invisibility from every OS surface is not guaranteed by an ordinary unprivileged app.

`PrivateStorageContext.kt` remains in source but the current engine does not use it as the active BlackBox context wrapper. Do not cite it as the active storage mechanism unless the integration is changed.

## 6. Permission model

The host manifest deliberately owns the real Android permissions. The BlackBox library manifest is stripped of its broad sample permission set through the overlay.

CI rejects leakage of high-risk inherited permissions including:
- `MANAGE_EXTERNAL_STORAGE`
- `REQUEST_INSTALL_PACKAGES`
- `SYSTEM_ALERT_WINDOW`
- `PACKAGE_USAGE_STATS`
- protected package install/delete permissions
- `READ_LOGS`
- launcher shortcut install permission

Do not re-add these merely to make a guest application work without first identifying the exact runtime requirement.

## 7. UI architecture today

The UI is classic XML/View-based Android, not Compose.

Phone resources:
- `res/layout/activity_main.xml`
- `res/layout/activity_private_space.xml`

Tablet resources:
- `res/layout-sw600dp/activity_main.xml`
- `res/layout-sw600dp/activity_private_space.xml`

Reusable rows/cards:
- `item_quote_card.xml`
- `item_guest_app.xml`
- `dialog_installed_app_picker.xml`
- `item_installed_app_candidate.xml`

Current weakness: `MainActivity` is too large and contains UI rendering, async thread orchestration, quote screen state, permission continuation and private-space screen state. Do not perform a large architectural split while Android 11 runtime acceptance is unresolved. After runtime acceptance, extract screen/controller classes behind the existing behavior.

## 8. Tests and proof levels

### JVM tests
Cover quote rules, installed-app filtering, permission policy and storage path policy.

### Build CI — `.github/workflows/build-apk.yml`
Proves:
- pinned BlackBox revision;
- unit tests actually execute;
- overlay was applied;
- host/guest/test APKs build;
- AAB creates a real split-APK fixture for an API 30 tablet spec;
- embedded guest matches the same-run guest build;
- host metadata targetSdk 30;
- BlackBox proxy components exist;
- forbidden permissions do not leak.

A green hosted build **does not prove runtime virtualization**.

### Physical Android 11 workflow — `.github/workflows/android11-device-runtime.yml`
Requires a self-hosted Android 11 ARM device and performs:
- split fixture installation;
- private split import;
- removal of normal host copy;
- virtual package survival;
- virtual launch;
- host PackageManager non-visibility;
- log evidence.

This is the authoritative automated runtime acceptance, but it still requires a connected real API 30 device.

## 9. Signing

Permanent update certificate fingerprint is documented in `docs/UPDATE_SIGNING.md`.

CI only produces a persistent in-place-update artifact when the required GitHub signing secrets are present. Otherwise the artifact is explicitly named `Aiham-Quotes-ci-debug-NOT-FOR-UPDATES`.

Never distribute a CI-debug artifact as the long-term tablet build.

## 10. Known risks / technical debt

Priority order:

1. **Android 11 physical runtime acceptance is not fully closed.**
2. WhatsApp runtime behavior is not yet accepted.
3. Android 14 guest UI surfacing is unreliable and is not the primary target.
4. `MainActivity` has excessive responsibilities.
5. duplicate Groovy/KTS build definitions are dangerous technical debt.
6. legacy fix/setup scripts can overwrite correct production files and must not be run.
7. several older docs contain stale package names, old BlackBox SHAs or outdated storage claims.
8. permission requests are currently derived from the base APK metadata; complex split-declared permission edge cases should be tested.
9. lifecycle/process restoration needs broader device testing.
10. stable signing secrets are still required in GitHub Actions before normal in-place updates can be distributed.

## 11. Architecture decision rules

When modifying this project:

- Android 11/API 30 runtime correctness outranks newer-Android convenience.
- preserve package `com.aiham.quotes` and Git history;
- keep `VirtualEngine` as the boundary around virtualization;
- make BlackBox-specific changes in `blackbox-overrides`, not directly in the submodule;
- never equate successful compile with successful guest runtime;
- preserve guest non-visibility in host PackageManager;
- do not add broad storage/install/overlay permissions without a proven requirement;
- do not replace FBlackBox until a real Android 11 failure demonstrates the current engine cannot satisfy the acceptance contract;
- UI redesign must not change the hidden-trigger semantics, package ID, targetSdk or runtime engine paths.
