# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android app. The app module lives in `app/`, with Kotlin source under `app/src/main/java/com/example/sayobotdownloader/`. Feature code is grouped by concern: `ui/search`, `ui/detail`, `data`, `network`, `download`, `model`, and `theme`. Android resources live in `app/src/main/res/`. Local unit tests are in `app/src/test/java/`; instrumented Compose tests are in `app/src/androidTest/java/`. CI configuration is in `.github/workflows/android.yml`, and dependency versions are centralized in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root.

- `.\gradlew.bat :app:assembleDebug` builds the debug APK.
- `.\gradlew.bat :app:lintDebug` runs Android lint for the debug variant.
- `.\gradlew.bat :app:testDebugUnitTest` runs JVM unit tests.
- `.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:assembleDebug` matches the current GitHub Actions verification job.
- `.\gradlew.bat :app:connectedDebugAndroidTest` runs instrumented tests on a connected emulator or device; this is not currently part of CI.

Run Gradle verification tasks sequentially when possible. Running multiple Gradle invocations in parallel can contend on Kotlin/Gradle cache directories and produce misleading cache errors.

## Coding Style & Naming Conventions

Write Kotlin using the existing project style: four-space indentation in Gradle Android blocks, concise Compose functions, and package paths matching feature ownership. Use `PascalCase` for composables, classes, and test classes; use `camelCase` for functions and properties. Keep ViewModel logic in `*ViewModel.kt`, UI in `*Screen.kt`, data access in repository classes, and network calls in `network/`. Prefer version-catalog entries in `gradle/libs.versions.toml` for new dependencies.

## Testing Guidelines

The project uses JUnit4 and `kotlinx-coroutines-test` for local tests, plus AndroidX/Compose test libraries for instrumented tests. Name tests after the unit under test, for example `SearchViewModelTest` or `SearchScreenTest`. Add local tests for ViewModel, repository, and state-handling changes. Add instrumented tests only when behavior requires Android framework or Compose runtime validation, and run them locally before merging.

For network request changes, add local tests around request construction when possible. The Sayobot API is sensitive to JSON value types, so `limit` and `offset` must be encoded as numbers, not strings.

## Sayobot API Notes

The beatmap list endpoint uses offset-based pagination. For `type = "new"` and `type = "hot"`, request pages with `offset = pageIndex * limit`, for example `0`, `25`, `50` when `limit = 25`. Do not rely on `endid` as a cursor. The API can loop back to the first page when offset exceeds available data, so list pagination should de-duplicate by stable identifiers such as `sid` and stop loading when a fetched page contains no new items.

Search input is intentionally explicit-submit only. Do not trigger backend search on every text change; use the keyboard Search action or the search icon.

### Advanced Search Filters

The `type = "search"` endpoint supports server-side filtering via bitmask parameters:

| Parameter | JSON key | Format | Notes |
|-----------|----------|--------|-------|
| Game mode | `mode` | bitmask int | 1=std, 2=taiko, 4=ctb, 8=mania. Sum of selected modes. Omit for "all". |
| Status | `class` | bitmask int | 1=Ranked, 2=Qualified, 4=Loved, 8=Pending, 16=Graveyard. Sum of selected statuses. Omit for "all". |

These parameters only work with `type = "search"`. For `"new"` and `"hot"` types, filtering falls back to client-side `filterItems()` in the ViewModel.

The `SearchFilterState` model (`model/SearchFilterState.kt`) uses `Set<String>` for multi-select and exposes `modeBitmask: Int?` / `statusBitmask: Int?` computed properties. When `applyFilters()` is called:
- **SEARCH mode** with active query: triggers `onSearch()` with bitmask to request server-filtered data.
- **Hot/New mode**: applies `filterItems()` client-side on existing data.

Staged filter state (in the Modal Bottom Sheet) is separate from the applied state. Dismiss discards staged changes; only "确认" applies them.

## Commit & Pull Request Guidelines

Recent history uses short imperative commit subjects such as `Add Android CI workflow` and `Remove emulator CI check`. Keep commits focused and avoid committing local files such as `.claude/`, `local.properties`, build outputs, or temporary XML dumps. Pull requests should describe the user-visible change, list the verification command run, link related issues when available, and include screenshots or recordings for UI changes.

## Security & Configuration Tips

Do not commit secrets, signing keys, or machine-specific SDK paths. `local.properties` should remain local. The CI builds and uploads a debug APK artifact only; release signing or distribution requires a separate, reviewed workflow with protected secrets.

The app declares `POST_NOTIFICATIONS` and uses notifications for download progress. Keep notification permission handling centralized in `MainActivity`, and keep download notification publishing tolerant of denied or revoked notification permission.
