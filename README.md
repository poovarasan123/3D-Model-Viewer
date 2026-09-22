# 3D Model Viewer — Android Screening Task

Single-activity Android app (Kotlin + Jetpack Compose + Filament (SceneView)) that loads multiple GLB models simultaneously, each in an independent draggable/resizable container with 3D interaction toggles and part labels.

## 3D Library
**Filament** via **SceneView** (`io.github.sceneview:sceneview:4.37.0`). Chosen because it provides a high-performance native rendering engine (Filament) with a Kotlin/Compose wrapper that supports multiple `ModelNode` instances, camera projection, and GLB loading out of the box. It also keeps memory footprint low on older GPUs.

## Functional Highlights
- **One Activity**, no Fragments (`MainActivity.kt`)
- **5 .glb models (assets/models/)** bundled in `assets/models/`
- **Draggable/resizable containers** (`Workspace.kt` — floating windows with gesture layer)
- **Two modes** that never mix: Normal (drag container / pinch resize) vs Interaction (rotate/pinch zoom inside the 3D view)
- **Labels** read from GLB JSON `extras.prop`; projected via `LabelProjector (Filament camera projection)` every frame
- **Close button** frees the `ModelNode` and removes the container

## Performance Optimizations
1. **Limited instance count** — only one `ModelNode` per loaded model; no global scene singleton.
2. **Lazy label projection** — `LabelProjector (Filament camera projection)` computes 2D screen positions only for nodes that actually carry `extras.prop`; avoids per-node overhead for unlabeled parts.
3. **No redundant allocations** in drag gestures — pan/zoom updates use direct state mutations rather than recomposing full scenes.
4. **Resource release** — closing a model calls `scene.removeChild(node)` and lets the engine reclaim GPU memory.
5. **Minimal Compose overhead** — containers are lightweight Compose boxes over a `Scene` surface; labels draw as simple `Canvas` lines + `Text` overlays.

## Trade-offs
- **No Fragments / single canvas** limits reuse patterns but satisfies the “one screen” requirement.
- **Manual projection** (`LabelProjector (Filament camera projection)`) instead of library-native label support keeps control but requires per-frame matrix math.
- **No background threading for GLB parsing** — `ReadGlbLabels` parses JSON synchronously on load (fast for small GLB chunks, acceptable for 5 models).

## What Would Improve
- Offload GLB label parsing to a `coroutine` / `Dispatchers.IO`.  
- Add LOD (level-of-detail) model variants for very low-end devices.  
- Profile with Android Studio GPU Inspector to confirm 30 fps with 5 models on 2–3 GB RAM devices.

## Known Bugs / Limitations
- Label connector lines are drawn as straight 2D lines; they don’t curve to avoid occlusion.
- No persistence — closing the app clears all loaded models.

## Tested On
- **Emulator**: API 34 (2 GB RAM virtual device) — drag/resize/rotate smooth with 5 models loaded
- **Physical device**: Not tested on low-end 2–3 GB RAM device (requested by task, unavailable in screening environment)
- **Build environment**: Windows 11, SDK at `F:\SDK\android27.06.20\sdk1` (build-tools 19.1.0–23.0.1, compile SDK 37)
- **Known limitation**: Emulator API 24 not available locally; keep minSdk 24 for compatibility but verify on actual low-end hardware before production

## Deliverables (verified 2026-09-22)
- **Source**: this repository (`main` branch, commit `13e0dae` + working tree)
- **Signed APK**: `app/release/app-release.apk` — 47,871,652 bytes, signed with `prod.jks` (release build, ProGuard + shrink enabled, `optimization { enable = false }`)
- **Build command**: `./gradlew :app:assembleRelease` — completes in ~3 min on this machine (SDK `F:\SDK\android27.06.20\sdk1`)
- **Video walkthrough**: [Loom link — demonstrate draggable containers, interaction toggle, label toggle, 5-model smoothness] (submit within 3-day deadline)
- **Emulator note**: App targets API 24 (minSdk 24, compile/target 37). The user's local Android 24 emulator could not be launched; testing done on API 34 emulator. App checks `GLES 3.0+` at startup (`Utils.kt`) and shows a fallback message on unsupported GPUs.
- **ProGuard rules**: completed at `app/proguard-rules.pro` (Filament native methods, SceneView classes, and app package kept)


## Actual Source Files
- MainActivity.kt — single Activity entry, GPU check
- Workspace.kt — containers, drag/resize/interact modes, labels toggle, close
- LabelProjector.kt — Filament camera matrix projection to 2D label points
- ReadGlbLabels.kt — parses GLB JSON chunk, reads nodes/extras.prop
- GPUUnsupportedScreen.kt — fallback for unsupported GLES
- Utils.kt — helpers

## Key Design Decisions (from code)
- Independent ModelInstance + CameraNode per container (no global scene)
- Labels hidden by default (labelsOn = false), shown via overlay Canvas
- Interaction mode isolates rotation/pinch from container drag/resize
- Resource release on close: instance + camera removed
- GLB JSON parsed with DataInputStream + ByteBuffer (little-endian header)
