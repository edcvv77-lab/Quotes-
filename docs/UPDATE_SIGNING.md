# Android update signing

The Quotes host package must keep the same Android signing certificate across every user-facing APK.
Without a stable certificate Android rejects an APK as an update even when the application ID is unchanged.

## Permanent update certificate

Expected SHA-256 certificate fingerprint:

`F6:2D:BD:1D:DC:85:CD:99:83:42:80:84:52:5D:F3:43:CA:DC:24:F3:A9:4F:45:1C:E7:B6:71:3C:26:8D:10:B7`

Alias:

`aiham_quotes_update`

The private keystore must never be committed to this public repository.

## Required GitHub Actions secrets

- `AIHAM_SIGNING_KEYSTORE_B64`: base64 of the private JKS keystore.
- `AIHAM_SIGNING_STORE_PASSWORD`: keystore password.
- `AIHAM_SIGNING_KEY_ALIAS`: `aiham_quotes_update`.
- `AIHAM_SIGNING_KEY_PASSWORD`: key password.

When all four values exist, `.github/workflows/build-apk.yml` signs the debug host artifact with the permanent update certificate and verifies its SHA-256 certificate fingerprint.

The workflow uses `github.run_number` as Android `versionCode`, so each subsequent workflow build has a monotonically increasing update version.

If signing secrets are missing, CI deliberately falls back to the runner debug key for build/test verification and emits a warning. Such an artifact must not be distributed as the persistent user-facing update build.

## Migration

Builds distributed before this certificate was introduced used CI debug signing. Android cannot update from an old, differently signed APK to the permanent certificate. One final uninstall/install is therefore required when switching to the permanent key. After that migration, all future APKs signed with this certificate can be installed in place and retain application data.
