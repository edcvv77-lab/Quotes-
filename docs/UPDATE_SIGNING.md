# Android update signing

The Quotes host package must keep the same Android signing certificate across every user-facing APK.
Without a stable certificate Android rejects an APK as an update even when the application ID is unchanged.

## Permanent update certificate

Expected SHA-256 certificate fingerprint:

`30:BC:12:9E:7B:A9:85:19:86:FB:52:D9:94:8C:0A:35:8D:69:0E:30:58:E2:0A:6C:A5:61:DB:3E:B2:33:56:F5`

Alias:

`aiham_quotes_update`

The private keystore must never be committed to this public repository. The permanent key was regenerated on 2026-08-31 because the earlier planned certificate had never been configured in GitHub Actions or distributed to a device.

## Required GitHub Actions secrets

Only two repository secrets are required:

- `AIHAM_SIGNING_KEYSTORE_B64`: base64 of the private JKS keystore.
- `AIHAM_SIGNING_STORE_PASSWORD`: keystore password.

The alias is fixed in source as `aiham_quotes_update`, and the private key uses the same password as the keystore. This keeps the one-time setup minimal without exposing private signing material.

When both values exist, `.github/workflows/build-apk.yml` signs the debug host artifact with the permanent update certificate and verifies its SHA-256 certificate fingerprint.

The workflow derives Android `versionCode` from `git rev-list --count HEAD`. Because project history is preserved and force-push is prohibited, each subsequent distributed build receives a higher update version even if the workflow itself is recreated.

If signing secrets are missing, CI deliberately falls back to the runner debug key for build/test verification and emits a warning. Such an artifact must not be distributed as the persistent user-facing update build.

## Migration

Builds distributed before this certificate was introduced used CI debug signing. Android cannot update from an old, differently signed APK to the permanent certificate. One final uninstall/install is therefore required when switching to the permanent key. After that migration, all future APKs signed with this certificate can be installed in place and retain application data.
