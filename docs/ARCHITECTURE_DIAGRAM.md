# Aiham Private Space: Architecture Diagram

This diagram illustrates the isolation boundaries and component relationships of the virtualization engine.

```text
+-----------------------------------------------------------------------------------+
|                                 Android Host OS                                   |
|                                (Settings, Launcher)                               |
|                                                                                   |
|  [Host PackageManager]      [Host ActivityManager]       [Host FileSystem]        |
|          |                            |                          |                |
+----------|----------------------------|--------------------------|----------------+
           |                            |                          |
           | (Sees only Aiham App)      | (Manages Stub Processes) | (Manages Host Data)
           V                            V                          V
+===================================================================================+
|                              Aiham Private Space (App)                            |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                           Virtualization Engine                             |  |
|  |                         (BlackBox / FBlackBox Core)                         |  |
|  |                                                                             |  |
|  |   +--------------------------+           +--------------------------+       |  |
|  |   | Virtual Package Manager  |           |     Virtual Services     |       |  |
|  |   | (In-memory APK records,  |           | (ActivityManager Proxy,  |       |  |
|  |   | Hidden from Host)        |           |  Notification Proxy)     |       |  |
|  |   +--------------------------+           +--------------------------+       |  |
|  |                |                                      |                     |  |
|  |   +---------------------------------------------------------------------+   |  |
|  |   |                          Virtual App Process                        |   |  |
|  |   |                (Running inside Host Stub Components)                |   |  |
|  |   |                                                                     |   |  |
|  |   |       [Virtual Application Context (e.g., WhatsApp)]                |   |  |
|  |   |                                                                     |   |  |
|  |   +---------------------------------------------------------------------+   |  |
|  |                                       |                                     |  |
|  |   +---------------------------------------------------------------------+   |  |
|  |   |                           Virtual Storage                           |   |  |
|  |   |                 (I/O Hooking & Path Redirection)                    |   |  |
|  |   |                                                                     |   |  |
|  |   | /data/data/com.aiham.privatespace/virtual/data/[guest_package_name] |   |  |
|  |   +---------------------------------------------------------------------+   |  |
|  +-----------------------------------------------------------------------------+  |
+===================================================================================+

```

### Flow Description
1. The **Android Host** environment represents standard applications and system services. The host `PackageManager` is entirely unaware of the applications installed inside the virtualization engine.
2. **Aiham Private Space** runs as a standard host application.
3. The **Virtualization Engine** creates an isolated environment by hooking and proxying system calls.
4. **Virtual Package Manager** handles the installation and parsing of APKs directly into the app's private directory.
5. **Virtual App Processes** are spawned using declared "Stub" activities in the host's `AndroidManifest.xml`. When an app launches, its execution occurs within these boundaries.
6. **Virtual Storage** uses I/O redirection to ensure the guest app reads and writes data to a secluded path managed by the host app, isolating it from the host filesystem.