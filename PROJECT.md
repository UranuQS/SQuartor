# Project: Squartor (Android E-Reader with Vitro 0.1.0 Liquid Glass & iOS 27 / Apple Books 27.2 Design)

## Tech Stack
- **Build & Tooling**: Android Gradle Plugin (AGP) `9.1.1`, Gradle `9.3.1`, Kotlin `2.4.21` (with `org.jetbrains.kotlin.plugin.compose`), `compileSdk = 37` (`targetSdk = 36`, `minSdk = 26`)
- **UI & Graphics**: Jetpack Compose BOM `2026.09.00` (`androidx.activity:activity-compose:1.10.0`), `io.github.itshedimisawi:vitro:0.1.0`, Android Graphics Shading Language (AGSL)
- **Parsing & Async**: `org.jsoup:jsoup:1.18.3`, `kotlinx-coroutines-android:1.10.1`, `androidx.documentfile:documentfile:1.0.1`

## Architecture
Squartor is a modern native Android reading application built with Kotlin, Jetpack Compose, Vitro 0.1.0, and Android Graphics Shading Language (AGSL).
- **Presentation Layer (iOS 27 / Apple Books 27.2 Liquid Glass + Vitro 0.1.0)**:
  - Real-time optical refraction, Gaussian softening, specular rim lighting, and SDF distance-based liquid merging (`glassBackdrop`, `glassEffect`, `glassEffectContainer`, `scrollEdgeEffect`) driven by `io.github.itshedimisawi:vitro:0.1.0`.
  - User-tunable glass clarity (`GlassClarityMode`: Clear / Regular / Tinted) and `glassOpacityLevel` (`0f..1f`) exposed through `ReadingStyle`, `LocalReadingStyle`, and `rememberIos27Glass`.
  - Apple Books-style shared-bounds book open/close container zoom transition in `MainActivity.kt` backed by `LocalBookLaunchTracker` and subtle retreating background scale/dimming, alongside PixelCopy circular day/night reveal and edge-to-edge `SystemBarStyle`.
  - GPU-friendly diagonal book cover waterfall (`CoverAssetCache.drawDiagonalCoverWaterfallToCanvas`) with multi-stage bilinear cover pre-filtering and zero per-frame CPU pixel blur loops.
- **Reading & Typography Engine**: Custom `NativeStaticLayoutPager` combining Android `StaticLayout` with CLREQ CJK punctuation squeeze rules, master layout canvas clipping, logical character offset anchoring (`startCharOffset` / `endCharOffset`), and oversized text slicing.
- **Parsing & Data Flow**: `EpubStreamEngine` and `TxtStreamEngine` streaming book structure, inline styles, relative/hash image assets, and chapter blocks into `BookRepository`.
- **State Machine**: 3-chapter window pager state machine coordinating asynchronous background pagination with zero page-bounce UI transitions and continuous vertical scroll flow.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | R1.1 Recomposition Bottleneck Eradication | Decouple `withFrameNanos` / `flowTimeSeconds` State in `ReadingNowScreen.kt` to draw-phase time provider, eradicating frame drops at 120Hz/144Hz | M1 | ORIGINAL_REQUEST §R1 |
| 2 | R1.2 Responsive Viewport Layout | Replace static `636f` scaling with Compose weight-based responsive layout with min/max height bounds | M1 | ORIGINAL_REQUEST §R1 |
| 3 | R1.3 Smooth PixelCopy Theme Toggle | Clamp `srcRect` to window decorView bounds with software canvas capture fallback and zero-flicker transition | M1 | ORIGINAL_REQUEST §R1 |
| 4 | R4.1 Dual-Channel & Vitro Liquid Glass | Upgrade `LiquidGlassKit.kt` from legacy dual-channel AGSL to Vitro 0.1.0 (`ios27GlassSurface`, `rememberIos27Glass`, `LocalVitroBackdrop`, `Ios27GlassVariant`) with iOS 27 high-contrast borders | M1 / Vitro | ORIGINAL_REQUEST §R4 |
| 5 | R4.2 Continuous Curvature & `CornerBasedShape` Rules | Maintain legacy `squircleShape` fallback while enforcing `CornerBasedShape` (`RoundedCornerShape` / `CircleShape`) on all Vitro `glassEffect` and `ios27GlassSurface` surfaces | M1 / Vitro | ORIGINAL_REQUEST §R4 |
| 6 | R4.3 Physics Spring & Vitro FlClash Dock | Migrate `FlClashNavigationDock.kt` to Vitro `glassEffectContainer(spacing = 10.dp)` + `glassEffect` with 4-Tab pill + contextual `DockContextualAction` orb fusion/separation, analytical spring scrub lens, AGSL content refraction, and haptics | M1 / Vitro | ORIGINAL_REQUEST §R4 |
| 7 | R2.1 Pager State Machine & Bounce Fix | Decouple `PagerState` from window rebuilds, implement synchronous slot offset compensation, atomic `ReadingPosition`, and `anchorCharOffset` reflow stability | M2 | ORIGINAL_REQUEST §R2 |
| 8 | R2.2 CJK Punctuation Squeeze & Kinsoku | Implement CLREQ punctuation squeeze (避头尾 / 标点挤压) in `NativeStaticLayoutPager.kt` | M2 | ORIGINAL_REQUEST §R2 |
| 9 | R2.3 Master Layout Canvas Clipping | Translate master `StaticLayout` on canvas with expanded horizontal clip margin to eliminate substring slice re-layout mismatch and glyph clipping | M2 | ORIGINAL_REQUEST §R2 |
| 10 | R2.4 Cover Flip Shadow & Reader UX | Fix backward Cover flip shadow geometry, add Apple Books 27.2 floating top/bottom glass clusters (`glassEffectContainer`), undoable chapter scrubber, and real-time Vitro sheets (`ReaderScreen.kt`) | M3 / Vitro | ORIGINAL_REQUEST §R2 |
| 11 | R2.5 Continuous Vertical Scroll Flow | Replace single-chapter silo with seamless 3-chapter continuous scroll list and resolve nested scroll / gesture conflicts | M3 | ORIGINAL_REQUEST §R2 |
| 12 | R2.6 Robust Content Parsing Engines | Fix EPUB relative/`file://`/`sq-epub://` image resolution, preserve inline styles (`<b>`, `<i>`), slice oversized blocks, and prevent chapter boundary overshoot | M3 | ORIGINAL_REQUEST §R2 |
| 13 | R3.1 Shelf Screen Bugs & Vitro Polish | Fix series comparator (`seriesOverride` / `normalizeSeriesBaseTitle`), add stacked series covers, expandable search bar, Vitro `LiquidGlassDialog` / `ModalBottomSheet`, and `LocalBookLaunchTracker` bounds reporting | M4 / Vitro | ORIGINAL_REQUEST §R3 |
| 14 | R3.2 Settings iOS 27 Navigation & Live Preview | Keep iOS HIG edge swipe navigation, live font preview, WebDAV sync feedback, and add interactive `LiquidGlassStyleCard` preview with `GlassClarityMode` + `glassOpacityLevel` controls | M4 / Vitro | ORIGINAL_REQUEST §R3 |
| 15 | R3.3 Stats Interactive Gestures & Haptics | Continuous horizontal drag scrubbing with haptics on 14-week calendar heatmap and bar charts, Vitro floating chrome, `StatsDetailScreen` swipe-back, and `LocalBookLaunchTracker` bounds reporting | M4 / Vitro | ORIGINAL_REQUEST §R3 |
| 16 | Shell Launch Transition & GPU Ambient Optimization | Apple Books shared-bounds `ReaderScreen` transition in `MainActivity.kt`, `SystemBarStyle` edge-to-edge cleanup, and removal of per-frame CPU `boxBlurInPlace` in `AmbientEffects.kt` | Vitro | Design Update |
| 17 | Unit, Compose UI & Release Verification | 23/23 JVM + Robolectric Compose UI tests passing (`StyleModelsTest`, `BookModelsTest`, `GlassDockAndReadingNowComposeTest`, `ReaderTypographyAndShelfTest`, `EpubStreamEngineTest`, `TxtStreamEngineTest`) and Release APK (`app-release.apk`) built cleanly | TEST_TRACK / M5 | Verification |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Visual Foundation & Main Screen | R1 (Performance, Responsive Viewport, PixelCopy) + R4 (Liquid Glass, `CornerBasedShape` / `Squircle` rules, Dock Springs) | none | DONE |
| M2 | Reader Core Engine & State Machine | R2 (Pager state machine, page bounce fix, `NativeStaticLayoutPager` CJK squeeze, master layout & char offset anchoring) | none | DONE |
| M3 | Reader Flip/Scroll Flows & Parsers | R2 (Cover flip shadow fix, continuous vertical scroll, `EpubStreamEngine` & `TxtStreamEngine` hardening) | M2 | DONE |
| M4 | Secondary Pages & Interactions | R3 (`ShelfScreen` search/series/dialogs, `SettingsScreen` glass preview & navigation, `StatsScreen` gestures) | M1 | DONE |
| M5 | Vitro 0.1.0 / iOS 27 Upgrade & Build Verification | Complete Vitro 0.1.0 + Apple Books 27.2 migration across all screens/shell, 23/23 JVM + Compose UI tests (`org.json` + Robolectric), `:app:lintVitalRelease`, `:app:assembleRelease`, and WSL Android 15 AVD `squartor_api35` (`0` bytes on `C:\`) | M1, M2, M3, M4 | DONE (Build & Unit/UI Verified; AVD awaiting `/dev/kvm` group) |

## Interface Contracts

### ReaderState ↔ PagerEngine Contract
- **Atomic Reading Coordinate**:
  ```kotlin
  data class ReadingPosition(val chapterIndex: Int, val pageIndex: Int)
  ```
- **PagerWindowState**:
  ```kotlin
  data class PagerWindowState(
      val centerChapterIndex: Int,
      val slots: List<PagerSlot>,
      val prevCount: Int,
      val currCount: Int,
      val nextCount: Int,
      val initialWindowIndex: Int,
  )
  ```
- **Pagination Callback Contract**:
  When background pagination completes for Chapter $K$, updates to `chapterPagesMap[K]` must NOT destroy the active `PagerState`. If $K < \text{currentChapter}$, `pagerState.requestScrollToPage(currentPage + delta)` must be dispatched synchronously in the same frame.

### Vitro 0.1.0 / LiquidGlassKit ↔ Composable Screens Contract
- **Core Style & CompositionLocals**:
  - `GlassClarityMode` (`clear`, `regular`, `tinted`, `fromName(name)`) & `ReadingStyle.glassClarityMode` / `ReadingStyle.glassOpacityLevel` (`0f..1f` persisted in `toJson()`/`fromJson()`).
  - `LocalVitroBackdrop`: root/page/dialog/sheet `GlassBackdrop?` (note: separate Compose `Window` overlays such as `Dialog` / `ModalBottomSheet` MUST create and provide their own `rememberGlassBackdrop()` so `localPositionOf` remains in-window).
  - `LocalReadingStyle`: current `ReadingStyle`.
  - `LocalGlassDarkTheme`: `!isLight` (or dark paper luminance in `ReaderScreen`).
  - `LocalBookLaunchTracker`: `((BookEntry, Rect?) -> Unit)?` recording cover bounds in root coordinates (`boundsInRoot()`), collapsing overlays (`showStatsDetail`), and driving the shared-bounds Reader launch animation cleanly.
- **Backdrop & Shape Rules**:
  - `.glassBackdrop(backdrop)` MUST be placed BEFORE `.background(...)` on the recorded content container; floating glass chrome renders outside/after it.
  - All shapes passed to Vitro `Modifier.glassEffect` or `Modifier.ios27GlassSurface` MUST be a `CornerBasedShape` (e.g., `RoundedCornerShape(...)`, `CircleShape`), NEVER `SquircleShape`.
- **Styling Helpers (`LiquidGlassKit.kt`)**:
  - `Ios27GlassVariant` (`Chrome`, `Card`, `SubtlePill`, `AccentPill`, `ReaderControl`, `ReaderSheet`)
  - `rememberIos27Glass(...)`, `ios27ContrastBorderColor(...)`, `Modifier.ios27GlassSurface(...)`
- **Legacy Compatibility (`LiquidGlassKit.kt`)**:
  - **Time Provider**: `val timeProvider: () -> Float` (elapsed seconds without triggering Compose recomposition).
  - **Squircle Shapes**: `fun squircleShape(radius: Dp): Shape = SquircleShape(radius)` and `SimpleSpring` preserved for legacy callers and tests.

## Code Layout
```
android/app/src/main/kotlin/com/squartor/reader/
├── MainActivity.kt                      # Entry point, CompositionLocals, root .glassBackdrop, SystemBarStyle, PixelCopy reveal, Reader shared-bounds zoom
├── data/
│   ├── BookRepository.kt                # Persistence, WebDAV cloud sync, metadata
│   ├── EpubStreamEngine.kt              # EPUB streaming parser and asset extractor
│   └── TxtStreamEngine.kt               # TXT streaming parser and chapter detector
├── model/
│   ├── BookModels.kt                    # BookEntry, ShelfBookBlock, ReadingPosition
│   └── StyleModels.kt                   # AppPalette, ReaderPalette, GlassClarityMode, ReadingStyle, CloudSyncSettings
└── ui/
    ├── glass/
    │   ├── AmbientEffects.kt            # CoverAssetCache, GPU diagonal cover waterfall, bilinear soft backdrops, BookCoverView
    │   ├── FlClashNavigationDock.kt     # Vitro glassEffectContainer dock + contextual action orb, DockSpring scrub & haptics
    │   └── LiquidGlassKit.kt            # Vitro iOS 27 helpers, CompositionLocals, Dialog/CircleButton, legacy fallbacks
    ├── reader/
    │   ├── NativeStaticLayoutPager.kt   # StaticLayout typography, CJK squeeze, char-offset anchoring, layout cache
    │   └── ReaderScreen.kt              # Reader screen, 3-chapter window, Apple Books 27.2 Vitro bars/sheets, flip/scroll flows
    ├── screens/
    │   ├── ReadingNowScreen.kt          # Home tab, GPU cover waterfall, Vitro glass hero/footprint/recent cards
    │   ├── SettingsScreen.kt            # Settings catalog, live glass preview card, font preview, WebDAV UI, edge-swipe subpages
    │   ├── ShelfScreen.kt               # Library tab, search filter, series stacking, batch selection, LiquidGlassDialog/sheets
    │   └── StatsScreen.kt               # StatsScreen & StatsDetailScreen, interactive calendar heatmap/bar charts, edge-swipe detail
    └── state/
        └── AppViewModel.kt              # Application state & repository coordination

android/app/src/test/kotlin/com/squartor/reader/
├── data/
│   ├── EpubStreamEngineTest.kt          # EPUB streaming & asset tests (5 tests)
│   └── TxtStreamEngineTest.kt           # TXT streaming & chapter detection tests (3 tests)
├── model/
│   ├── BookModelsTest.kt                # BookEntry/ImportedFont JSON round-trip & helpers tests (3 tests)
│   └── StyleModelsTest.kt               # GlassClarityMode, ReadingStyle & CloudSyncSettings JSON tests (5 tests)
└── ui/
    ├── GlassDockAndReadingNowComposeTest.kt # Robolectric Compose UI tests for FlClashNavigationDock & ReadingNowScreen (2 tests)
    └── ReaderTypographyAndShelfTest.kt  # SimpleSpring, pagination char-offset & shelf series tests (5 tests)
```
