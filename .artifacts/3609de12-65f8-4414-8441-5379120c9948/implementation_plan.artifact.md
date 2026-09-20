# Enable Draggable 3D Models in FilamentScreen

This plan outlines the changes required to make each 3D model in the `FilamentScreen` draggable, scalable, and rotatable by the user using built-in `SceneView` gestures.

## User Review Required

> [!NOTE]
> Enabling `isEditable = true` on a `ModelNode` in `SceneView` 4.x automatically enables several gestures:
> - **One-finger drag**: Moves the model.
> - **Pinch-to-zoom**: Scales the model.
> - **Two-finger twist**: Rotates the model.
>
> If you only wanted dragging and not scaling/rotation, please let me know, as further customization of gesture listeners might be needed.

## Proposed Changes

### 3D Model Viewer Component

#### [MODIFY] [FilamentScreen.kt](file:///H:/magizh/3DModelViewer/app/src/main/java/com/a3dmodelviewer/FilamentScreen.kt)

Update each `ModelNode` instantiation for the three models (Washing Machine, Watch, and Helmet) to include the `isEditable = true` property.

- **Model 1 (Washing Machine)**: Set `isEditable = true` in the `ModelNode` constructor.
- **Model 2 (Watch)**: Set `isEditable = true` in the `ModelNode` constructor.
- **Model 3 (Helmet)**: Set `isEditable = true` in the `ModelNode` constructor.

## Verification Plan

### Automated Tests
- Not applicable for this UI/Gesture change.

### Manual Verification
1. Deploy the app to a physical device or an emulator with sensor support.
2. Select each model (Wash Machine, Watch, Helmet).
3. Try dragging each model with one finger.
4. Try scaling each model with a pinch gesture.
5. Try rotating each model with two fingers.
