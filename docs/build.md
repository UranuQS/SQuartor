# Build notes

## Flutter SDK

This repo builds against Flutter `^3.12.0` (see `pubspec.yaml`). On the
maintainer's machine the SDK lives at:

```
D:\antigravity_projects\flutter\bin\flutter.bat
```

Adjust to your path or rely on `flutter` being on `$PATH`.

## Android: AGP 9 pub-cache patches

Two transitive plugins do not yet build cleanly on Android Gradle Plugin 9.
We carry small `build.gradle` overrides in
[`tooling/pub-cache-patches/`](../tooling/pub-cache-patches/) and apply them
into `pub-cache` before building.

| Plugin | Issue | Fix |
| --- | --- | --- |
| `file_picker-11.0.2` | When `android.builtInKotlin=false` on AGP 9+ the plugin skips applying `org.jetbrains.kotlin.android`, so its Kotlin sources can't be referenced from the generated Java. | Always apply the Kotlin plugin. |
| `flutter_inappwebview_android-1.1.3` | References `proguard-android.txt`, which AGP 9 removed. | Switch both build types to `proguard-android-optimize.txt`. |

### Apply the patches

After every `flutter pub get` (and after `flutter pub cache repair`), run
**one** of these:

```bash
# Bash / Git Bash / WSL
bash tooling/apply_pub_cache_patches.sh
```

```powershell
# Windows PowerShell
pwsh -File tooling\apply_pub_cache_patches.ps1
```

The script auto-detects pub-cache via `$PUB_CACHE`, `$LOCALAPPDATA\Pub\Cache`,
or `~/.pub-cache`. Override with `PUB_CACHE=...` if your layout differs.

If you'd rather skip the patch step entirely, downgrade the Android Gradle
Plugin to 8.x in `android/settings.gradle.kts` — both upstream issues only
affect AGP 9.

## Android: ABI filters for emulators

The Android module ships with `abiFilters x86_64` so MuMu/Genymotion x86_64
emulators don't fall back to ARM translation. Drop or extend the filter in
`android/app/build.gradle.kts` if you need other ABIs (e.g. for a real
device).

## Verifying

```bash
flutter analyze
flutter test
```

Both should report no issues / all tests passing on `main`.
