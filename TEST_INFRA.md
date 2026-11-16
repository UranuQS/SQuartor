# E2E Test Infra: Squartor

## Test Philosophy
- Opaque-box, requirement-driven. Derives from user requirements in `ORIGINAL_REQUEST.md` and feature contracts in `PROJECT.md`.
- No internal module dependencies where possible: exercises parsing engines, typography algorithms, state machine transactions, and persistence contracts via public interfaces.
- Methodology: Category-Partition + Boundary Value Analysis (BVA) + Pairwise Combinatorial + Real-World Workload Testing.

## Feature Inventory
| # | Feature | Source (Requirement) | Tier 1 | Tier 2 | Tier 3 |
|---|---------|----------------------|:------:|:------:|:------:|
| 1 | ReadingNow Performance & Draw-Time Provider | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| 2 | Viewport Adaptive Responsive Layout | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| 3 | PixelCopy Theme Reveal & Fallback | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| 4 | Liquid Glass AGSL Shaders & Frosted Sampling | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| 5 | Squircle Continuous Curvature Shapes | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| 6 | Physics Springs & FlClash Dock | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| 7 | Reader 3-Chapter Window & State Machine | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| 8 | NativeStaticLayoutPager CJK Typography & Squeeze | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| 9 | Master Layout Canvas Clipping | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| 10 | Cover Flip & Vertical Scroll Flows | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| 11 | EpubStreamEngine Asset & Style Parsing | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| 12 | TxtStreamEngine Chapter Splitting & Slicing | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| 13 | ShelfScreen Series Grouping & Sorting | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |
| 14 | Settings Navigation & WebDAV Sync Models | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |
| 15 | Stats Calculation & Heatmap Data Aggregation | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |

## Test Architecture
- **Runner**: Gradle test runner (`./gradlew testDebugUnitTest`)
- **Frameworks**: JUnit 4 (`junit:junit:4.13.2`), Kotlinx Coroutines Test (`kotlinx-coroutines-test:1.10.1`), Roborazzi / Robolectric or headless JVM test harnesses.
- **Directory Layout**: `android/app/src/test/kotlin/com/squartor/reader/`

## Real-World Application Scenarios (Tier 4)
| # | Scenario | Features Exercised | Complexity |
|---|----------|--------------------|------------|
| 1 | Continuous Reading & Chapter Boundary Cross | F7, F8, F9, F10, F11 | High |
| 2 | Large Library Import & Series Grouping Sorting | F13, F14 | Medium |
| 3 | EPUB with Relative Images & Rich Inline Typography | F8, F9, F11 | High |
| 4 | WebDAV Full Sync Cycle & Conflict Handling | F14 | High |
| 5 | Multi-day Reading Stats Accumulation & Streak Check | F15 | Medium |
| 6 | Rapid Theme Switching & Orientation Viewport Re-layout | F1, F2, F3, F4, F5 | Medium |

## Coverage Thresholds
- Tier 1: ≥5 per feature (75 tests across 15 features)
- Tier 2: ≥5 per feature (75 boundary/corner tests)
- Tier 3: ≥15 cross-feature pairwise tests
- Tier 4: ≥6 realistic end-to-end workload scenarios
- **Total Minimum Target**: ≥171 test cases
