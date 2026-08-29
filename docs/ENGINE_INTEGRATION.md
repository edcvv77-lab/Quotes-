# Engine Integration Details

## Version
- **Engine:** FBlackBox/BlackBox
- **Commit SHA:** `ffe950f7d15dae671cd8e95b58c83b35aab8f141`
- **License:** Apache License 2.0 (assumed based on standard BlackBox distributions).

## Integration Approach
The engine is pulled from the remote git repository and included as a local source module (`Bcore`). This allows necessary adjustments for Android 11 compatibility while keeping the main app isolated via the `VirtualEngine` abstraction.

## Android 11 Compatibility
The selected FBlackBox commit inherently targets API 30 and compiles correctly against Android 11 after resolving repository conflicts (moving from `jcenter` to `mavenCentral`/`aliyun` due to jcenter deprecation).

## Virtual Storage
- FBlackBox stores the installed APK and data inside the host app's private directory.
- Typical path: `/data/user/0/com.aiham.privatespace/virtual/...`
- **Important:** The virtual files are NOT invisible to the host filesystem. If the device is rooted, or the host app's private directory is accessed, the virtual APK and data are fully visible. The isolation is strictly at the Android Package Manager level.

## Blockers and Next Steps
- **Proceeding to WhatsApp:** GO. The core isolation requirement is validated. However, significant blockers remain for WhatsApp (GMS dependency, push notifications, strict anti-abuse integrity checks).
