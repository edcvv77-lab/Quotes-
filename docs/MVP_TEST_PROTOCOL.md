# MVP Test Protocol

This document outlines the procedure to test the Aiham Private Space MVP.

## Prerequisites
- Android 11 emulator or device.
- The `com.aiham.privatespace` (Host App) built and installed.

## Testing Steps
1. **Host App Launch:**
   - Launch "Aiham Private Space" on the device.
   - Verify the Engine Status reads: `Initialized (FBlackBox)`.
2. **Installation (C):**
   - Click "Install AihamVirtualTest APK".
   - Check the Logs for `Install result: true`.
3. **Virtual Package List (B):**
   - Click "List Virtual Apps".
   - Verify `com.aiham.virtualtest` appears in the logs.
4. **Launch (D):**
   - Click "Launch AihamVirtualTest".
   - Verify the test application opens showing "Aiham Private Space Test".
5. **Host Visibility Validation (A, G, H):**
   - Press the Home button.
   - Open Android Settings -> Apps.
   - Verify that `Aiham Private Space Test` (`com.aiham.virtualtest`) is **NOT** listed.
   - Open the device Launcher/App Drawer.
   - Verify the test application is **NOT** listed.
6. **Uninstall (E):**
   - Return to the Host App.
   - Click "Uninstall AihamVirtualTest".
   - Verify the log shows `Uninstall result: true`.
   - List the virtual apps again to confirm removal.
7. **Reboot/Persistence (F):**
   - Re-install the test app.
   - Restart the Android device.
   - Re-open the host app and click "List Virtual Apps".
   - The test application should still be listed.
