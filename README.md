# P-Explorer

P-Explorer is a modern, local-first Android file manager built with Kotlin,
Jetpack Compose, and Material 3. It is designed for fast folder navigation,
safe file operations, storage analysis, and optional P-Hub file transfer over
the local Wi-Fi network.

## Features

- Home screen with storage usage, categories, recent files, and quick actions.
- Local storage browsing with breadcrumbs and Android back navigation.
- List and grid views with persistent sorting and view preferences.
- File type icons and efficient image thumbnails.
- Create folders, rename, delete, copy, move, share, and open files.
- Multi-selection with progress and cancellation for file operations.
- Safe duplicate-name handling such as `file (1).pdf`.
- File properties including asynchronous folder size and permissions.
- Global cached file search with recent search history.
- Favorites and recent files persisted locally with unavailable-item handling.
- Hidden-file and file-extension display settings.
- Image preview with zoom and pan.
- Text preview limited to 2 MB.
- ZIP creation and extraction with archive path-traversal protection.
- Storage analyzer with category usage and progress reporting.
- Large-file finder with 100 MB, 500 MB, and 1 GB thresholds.
- Duplicate finder based on normalized name and file size.
- Reusable transfer queue state model.
- P-Hub discovery, authentication, remote browsing, downloads, and uploads.

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- MVVM and StateFlow
- Kotlin Coroutines
- Navigation Compose
- Android Storage Access Framework-compatible architecture
- Local filesystem provider behind repository interfaces
- DataStore Preferences for settings and metadata
- Coil for image loading and thumbnails
- Android NSD and HTTP for P-Hub connectivity

## Requirements

- Android Studio with JDK 17.
- Android SDK 36.
- Android 10 or newer, minimum API 29.

The application uses `MANAGE_EXTERNAL_STORAGE` on Android 11 and newer because
full local file-manager operations are a core feature. The user is sent to the
system access screen only after choosing to grant storage access. This
permission has Google Play policy restrictions and should be reviewed before
publishing.

## Build

From the project directory on Windows:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_SDK_ROOT = "C:\Users\Prakash\AppData\Local\Android\Sdk"
& .\gradlew.bat testDebugUnitTest
& .\gradlew.bat assembleDebug
& .\gradlew.bat assembleRelease
& .\gradlew.bat lintDebug
```

Generated APKs are written to:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

The release build is currently unsigned and has minification disabled while
the feature set is still being stabilized.

## Architecture

The UI does not manipulate files directly:

```text
Compose UI
    -> ViewModel
        -> Repository
            -> FileSystemProvider
```

Important areas:

- `data/filesystem`: local and future network filesystem boundaries.
- `data/network`: P-Hub discovery and HTTP protocol implementation.
- `data/preferences`: DataStore-backed application metadata.
- `data/transfer`: reusable transfer queue manager.
- `domain/model`: filesystem, preferences, scan, and network models.
- `domain/usecase`: sorting, validation, and duplicate-name logic.
- `presentation`: Compose screens, navigation, dialogs, and StateFlow state.

## P-Hub

P-Explorer integrates with the P-Hub protocol already used in this workspace:

- NSD service: `_phub._tcp`.
- Default port: `8080`.
- Challenge endpoint: `/challenge`.
- Storage roots: `/roots?auth=`.
- Directory listing: `/ls?auth=&path=`.
- File download: `/download?auth=&path=`.
- File upload: `/upload?auth=&path=&name=`.

Both devices must be connected to the same Wi-Fi network. Start the P-Hub
server, open the P-Hub entry from the P-Explorer More menu, discover the
server, select the authentication code displayed by P-Hub, and connect.

Downloaded files are saved under `Download/P-Hub`.

## Tests

Unit tests cover:

- File sorting.
- File-name validation.
- Collision-safe file naming.
- P-Hub protocol mapping.
- Transfer queue completion and failure states.

## Current Limitations

- RAR and other archive formats are identified but only ZIP extraction is
  implemented.
- Hash-based duplicate verification is not enabled; duplicate matching uses
  name and size.
- P-Hub runtime behavior requires a live P-Hub server and Wi-Fi device test.
- SMB, FTP, and WebDAV providers are not implemented yet.
- The release APK requires signing before distribution.

## Privacy

P-Explorer is local-first. It does not upload files automatically, add
advertising, or collect analytics. Network transfers occur only when the user
opens the P-Hub feature and starts a transfer.
