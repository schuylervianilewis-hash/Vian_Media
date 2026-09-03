# Master Plan & Blueprint
## Phase 1: Core Setup
- [x] Basic layout, Exoplayer, file selection.
## Phase 2: Editor Tools
- [x] Trim & Double Trim
- [x] Crop & Aspect Ratio
- [x] Rotate & Flip
- [x] Speed & Volume
- [x] Captions
## Phase 3: Live Preview Sync
- [x] Sync all edits (Cut/Trim, Captions, Effects) dynamically back to the Main Preview UI.
## Phase 4: Output Pipeline
- [x] FFmpeg command generation
- [x] Progress reporting & background service
- [x] Output video preview size display below player
- [x] Resilient batch image compression lifecycle and interruption handling
- [x] Mini Player "Open With" intent overlay presentation & permission management
- [x] Lock/unlock playback recovery, persistable URI permission retention, and orientation state restoration
- [x] Popup player lowered compact seekbar and unified bottom control row with sticky corner resize handle
- [x] Activity exit & back navigation orientation reset (preventing launcher crash on external playback return)
- [x] GitHub Actions APK pipeline configuration and Gradle lint error suppression (abortOnError=false)
- [x] Gradle wrapper pinning to 8.11.1 and compileSdk/targetSdk stabilization to API 35
- [x] Archived CI/CD GitHub Actions workflow reference to `/reference/github-actions/` and purged redundant `/Vianmedia-main` snapshot and transient root scripts
- [x] Zero-Memory Cache Architecture: Configured Option 0 (memory-only Coil thumbnail cache, 0 MB disk footprint), automated lifecycle temp deletion across VideoEditor/Compression/FFmpeg pipelines, startup orphaned cache purge, and added "Clear Unused Data" manual trigger in Settings (Storage & Output).
- [x] Lock Screen 0:00 Playback Resumption Fix: Eliminated redundant `seekTo(0)` calls on `STATE_IDLE` across `PlayerScreen.kt` and `FloatingVideoPlayerOverlay.kt`, preserved `playWhenReady` intent across lifecycle via `wasPlayingBeforePause` (rememberSaveable), and preserved PlayerView player binding across view resets (`onReset`) to prevent surface renderer detachment.
- [x] Heavy Resource Exclusivity Architecture: Enforced strict single-task execution across hardware decoders, CPU transcoding, and storage I/O. Added automatic background playback pausing on VideoEditorScreen, PhotoEditorScreen, AudioTrimmerScreen, FFmpegService, and CompressionService start. Deferral and exclusion of device-wide MediaStore playlist scans during active FFmpeg/Compression tasks or initial video playback buffering.
- [x] Player Controls Area Tap Retention: Wrapped topbar and bottombar in pointer-consuming container Boxes with gesture interception (`awaitFirstDown(false)` and `detectTapGestures`), preventing control area taps from bubbling down to the background toggle listener (`showControls = !showControls`), and resetting the 4000ms auto-hide countdown on user interaction.
- [x] Continuous Smooth Vertical Volume Gesture: Replaced chunky integer-quantized volume scaling with continuous normalized float ratio tracking. Enables seamless single-unit progression (1, 2, 3, 4...) and fluid vertical fill bar animations while cleanly triggering underlying AudioManager hardware steps and LoudnessEnhancer boost thresholds.
- [x] Application-Wide Crash Resilience & Scoped Storage LogKeeper: Initialized `LogKeeper` at application startup (`VianApplication`), implemented dual-storage logging targeting public `MediaStore.Downloads` (API 29+) and app-specific external files dir fallback (bypassing legacy scoped storage EACCES).
- [x] External Intent Playback Backstack Stabilization: Fixed fatal `IllegalArgumentException` in Navigation Compose by anchoring `NavHost` root to `startDest` (`main`/`welcome`), adding unpadded Base64 route safety with decoding try/catch fallbacks, and protecting intent parcelable bundle unpacking against `BadParcelableException`.
- [x] Database & MediaStore Query Hardening: Configured `fallbackToDestructiveMigration()` in Room builder to prevent launch lockout on version mismatches, and replaced `getColumnIndexOrThrow` with guarded `getColumnIndex` lookups for optional MediaStore columns.


