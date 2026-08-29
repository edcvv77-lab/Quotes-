# MVP Architecture

This document describes the Minimum Viable Product (MVP) architecture for Aiham Private Space.

## Abstraction Layer
The core of the integration is the `VirtualEngine` interface. It provides an abstraction to interact with the underlying virtualization mechanism without tightly coupling the host app UI to specific implementations.

```kotlin
interface VirtualEngine {
    fun initialize(context: Context)
    fun installApk(apkFile: File): Boolean
    fun uninstallApp(packageName: String): Boolean
    fun listInstalledApps(): List<String>
    fun launchApp(packageName: String, context: Context): Boolean
    fun isInstalled(packageName: String): Boolean
    fun getEngineStatus(): String
}
```

## BlackBox Integration
The `BlackBoxVirtualEngine` class implements this interface using the FBlackBox core engine:
- **Initialization:** Calls `BlackBoxCore.get().doAttachBaseContext` and `doCreate` within the `Application` class.
- **Installation:** Uses `BlackBoxCore.get().installPackageAsUser` with `InstallOption.installByStorage()`.
- **Launching:** Proxies the request to `BlackBoxCore.get().launchApk`.

This engine isolates the guest application context inside the daemon/stub processes configured by FBlackBox, keeping the host Android PackageManager completely oblivious to the virtual applications.

## Test Application
A minimal test application, `AihamVirtualTest` (`com.aiham.virtualtest`), is bundled with the host to validate the isolation properties without the complexities of third-party APK dependencies.
