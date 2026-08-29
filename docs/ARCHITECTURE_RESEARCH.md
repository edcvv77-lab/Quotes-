# Architecture and Feasibility Research: BlackBox/FBlackBox for Aiham Private Space

## A. Executive conclusion
Based on the feasibility research, the BlackBox/FBlackBox-style application virtualization model is **technically feasible** for Android 11 as the core engine of Aiham Private Space. It successfully achieves the critical user requirement: virtualized applications remain completely hidden from the host system's Settings, PackageManager, and Launcher. However, substantial engineering effort will be required to ensure stable WhatsApp operation, primarily due to Google Play Services dependency, background execution limits, and strict integrity checks introduced in modern Android and WhatsApp versions.

## B. Current BlackBox architecture
*(Note: The current repository contains no source code apart from the README. This analysis is based on the BlackBox/FBlackBox open-source virtualization engine standard.)*

1. **Virtualization mechanism:** BlackBox uses dynamic proxying, Binder hooking, and Reflection. It acts as a sandbox container. By intercepting calls to core system services (ActivityManager, PackageManager), it tricks the guest app into believing it is running in a normal environment.
2. **Virtual package installation:** Apps are "installed" by parsing the APK file, extracting its components (dex, native libs, resources), and storing them directly in the private data directory of the host app (`/data/data/com.aiham.privatespace/virtual/...`).
3. **Virtual app launching:** The host app registers several "Stub" Activities, Services, and Providers in its `AndroidManifest.xml`. When a virtual app launches, the framework intercepts the Intent, redirects it to a Stub Activity (e.g., `StubActivity$p1`), and at runtime, replaces the classloader and context to load the guest app's actual Activity.
4. **Virtual storage/data isolation:** IO redirection is implemented (via Java hooks or native inline hooks). Any file access attempt by the virtual app targeting `/data/data/com.whatsapp` is redirected to `/data/data/com.aiham.privatespace/virtual/data/com.whatsapp`. This achieves application-level isolation.
5. **Virtual representations:** Virtual users, services, and packages are maintained entirely in-memory within a custom `BPackageManagerService` and `BActivityManagerService` running inside the host application's daemon process. Process separation is achieved by spawning isolated host processes (e.g., `:p1`, `:p2`).
6. **What the host PackageManager sees:** The host PackageManager ONLY sees the host app (Aiham Private Space) and the stub components it declared. It has zero knowledge of the guest applications.

## C. Android 11 compatibility assessment
Android 11 introduces several restrictions that affect virtualization:
- **Scoped Storage:** Android 11 enforces scoped storage, meaning the host app cannot blindly access the entire `/sdcard` without specific permissions (`MANAGE_EXTERNAL_STORAGE`). Virtual apps attempting to read/write shared media (like WhatsApp saving photos) must have their IO calls intercepted and routed through the host's permitted directories.
- **Package Visibility:** Android 11 prevents apps from querying installed packages. The host app must declare `QUERY_ALL_PACKAGES` if it needs to import apps installed on the host. Since the goal is to install APKs independently, this is manageable.
- **Hidden API Restrictions:** Android 11 tightens restrictions on non-SDK interfaces. BlackBox utilizes bypass techniques (e.g., FreeReflection or similar meta-reflection methods) to hook system services. These hooks must be verified against Android 11/12 specific offsets.

## D. Host vs virtual application visibility analysis
Can an application installed inside the virtual engine appear in:
- **Host Settings -> Apps:** **NO**. The OS package manager is completely unaware of the APK.
- **Host PackageManager:** **NO**.
- **Host launcher:** **NO**. Unless the user explicitly creates a shortcut (which is handled and rendered by the host app).
The isolation strictly meets the critical user requirement.

## E. WhatsApp feasibility/risk analysis
Virtualizing modern WhatsApp introduces significant blockers:
- **Google Play Services (GMS):** WhatsApp relies heavily on GMS for Firebase Cloud Messaging (FCM) push notifications and location sharing. The virtual environment must either virtualize GMS (which requires microG or a virtualized GMS core) or intercept push notifications via the host app.
- **Background Execution:** Android 11 strictly limits background processes. If Aiham Private Space is killed by the OS, WhatsApp dies with it. The host must maintain a foreground service (with a persistent notification) to keep the virtual environment alive.
- **Device/App Integrity Checks:** WhatsApp actively detects modified environments, emulators, and virtual spaces to prevent spam. Running inside BlackBox risks immediate or delayed account bans.
- **WebView:** Modern WebView implementations rely on strict multi-process isolation. The virtual engine must properly map the WebView zygote and render processes to its stub processes.
- **Storage/Media:** WhatsApp requires camera and microphone access. The host app must request these permissions from the user and proxy them to the guest.
- **Notifications:** The virtual engine must intercept `NotificationManager` calls from WhatsApp, transform them, and present them on the host as coming from Aiham Private Space.

## F. Alternatives considered
1. **VirtualApp (Lody):** The pioneer of Android app virtualization. Highly stable but became commercialized and closed-source for newer Android versions. Open-source forks are mostly outdated.
2. **VirtualXposed / Epic:** Primarily focused on Xposed module support rather than pure application hiding. Overhead is higher, and compatibility with modern apps (like WhatsApp) can be flaky due to deep ART hooking.
3. **Android Work Profile (Device Policy Manager):** Uses native Android multi-user features. While extremely stable, the apps are visible to the host system as "Work" apps in Settings and Launchers. This **violates** the critical requirement.

## G. Recommended architecture
Proceed with **BlackBox/FBlackBox** as the foundation. It provides the necessary application-level isolation without requiring root, and natively keeps the installed applications completely hidden from the host system. It supports the dynamic proxying needed to fake the OS environment for the guest app.

## H. MVP proof-of-concept definition
The Minimum Viable Product (MVP) should validate the core requirement without building unnecessary features:
1. Integrate the BlackBox core engine into a skeleton Android app.
2. Implement a simple file picker to select an APK (e.g., an open-source chat app or calculator) from the host filesystem.
3. Install the APK into the virtual engine (without triggering standard Android installation).
4. Launch the virtual app.
5. **Verification:** Confirm the app runs, and verify it does NOT appear in Android Settings -> Apps.

## I. Known blockers
- **GMS Dependency:** Push notifications for WhatsApp will likely fail out-of-the-box.
- **Anti-Ban Mechanisms:** High risk of WhatsApp banning accounts running in a virtual space.
- **Scoped Storage Routing:** Media sharing in Android 11 will require complex IO hooking.

## J. Explicit GO / NO-GO decision
**GO.**
The architecture is technically sound to meet the strict isolation requirement. The risks associated with WhatsApp are high, but they are application-specific hurdles to solve after the core virtualization engine is proven.

## K. Exact next implementation steps
1. Initialize a basic Android Studio project targeting API 30 (Android 11).
2. Pull the BlackBox core library (either as a git submodule or pre-compiled AAR) into the project.
3. Set up the custom `Application` class to initialize the `BlackBoxCore`.
4. Define the necessary stub Activities/Services in `AndroidManifest.xml`.
5. Build a minimalist UI with a "Select APK" and "Launch App" button to complete the MVP.