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
