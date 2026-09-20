# Walkthrough - Enabling Draggable 3D Models

I have updated the `FilamentScreen.kt` to make each rendered 3D model draggable, rotatable, and scalable by the user.

## Changes

### 3D Model Interaction

#### [FilamentScreen.kt](file:///H:/magizh/3DModelViewer/app/src/main/java/com/a3dmodelviewer/FilamentScreen.kt)

- Added `isEditable = true` to the `ModelNode` composable.
- Added an `apply` block to set `isPositionEditable = true`, which is required in SceneView 4.x to enable dragging (as it defaults to `false` even when `isEditable` is `true`).
- Cleaned up unused variables (`modelNum`, `modelInstance`, `modelInstances`) and redundant model loading logic that was previously unused.

## Verification Results

### Automated Tests
- Ran `gradlew app:assembleDebug` and the project built successfully.

### Manual Verification Required
- Deploy the app to a device.
- Select one or more models from the dropdown.
- Verify that you can:
    - **Drag** the model to move it.
    - **Pinch** the model to scale it.
    - **Twist** the model to rotate it.
