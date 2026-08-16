# Release Checklist

This project does not store signing keys or passwords. Release signing is
enabled only when all four values below are supplied through Gradle properties
or environment variables:

- `P_EXPLORER_STORE_FILE`
- `P_EXPLORER_STORE_PASSWORD`
- `P_EXPLORER_KEY_ALIAS`
- `P_EXPLORER_KEY_PASSWORD`

## Create A Local Keystore

Run this once outside source control. The `.jks` file is ignored by Git.

```powershell
keytool -genkeypair -v `
  -keystore p-explorer-release.jks `
  -alias p-explorer `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

Back up the keystore and passwords securely. Losing the signing key prevents
future updates to an already published application.

## Configure A Local Build

Use a temporary shell environment rather than committing passwords:

```powershell
$env:P_EXPLORER_STORE_FILE = (Resolve-Path .\p-explorer-release.jks).Path
$env:P_EXPLORER_STORE_PASSWORD = "your-store-password"
$env:P_EXPLORER_KEY_ALIAS = "p-explorer"
$env:P_EXPLORER_KEY_PASSWORD = "your-key-password"
```

The same values can be provided as `-P` Gradle properties in CI. Configure
those values through the CI secret store.

## Verify A Release

```powershell
& .\gradlew.bat clean testDebugUnitTest lintDebug assembleRelease
```

When signing values are configured, verify the resulting APK with the Android
SDK build tools:

```powershell
apksigner verify --verbose app\build\outputs\apk\release\app-release.apk
```

Without signing values, `assembleRelease` intentionally produces an unsigned
APK named `app-release-unsigned.apk`.

## Publishing Review

- Confirm the application ID and version code/version name.
- Review the `MANAGE_EXTERNAL_STORAGE` Google Play policy requirements.
- Review the cleartext local-network requirement for P-Hub.
- Test storage access denial and recovery.
- Test image/text preview and ZIP extraction with malformed input.
- Test P-Hub discovery, authentication, upload, and download on Wi-Fi.
- Check the signed APK with Play Console pre-launch reports.
- Keep the release keystore and passwords outside the repository.
