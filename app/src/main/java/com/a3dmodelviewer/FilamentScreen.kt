package com.a3dmodelviewer

import android.content.Context
import android.graphics.Color
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import io.github.sceneview.Scene
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberModelInstance
import org.json.JSONObject
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.MILLISECONDS

/**
 * FilamentScreen - 3D Model Viewer using Google Filament Engine
 *
 * FILAMENT COMPONENTS:
 * - rememberEngine(): Creates Filament rendering engine (GPU acceleration)
 * - rememberModelLoader(): Loads GLB/glTF 3D model files from assets
 * - rememberEnvironmentLoader(): Loads HDR environment maps for realistic lighting
 * - Scene: Displays 3D objects with lighting, shadows, and interactions
 * - ModelNode: Represents individual 3D model in the scene
 * - Camera: Controls viewing angle and position
 */

data class Model(val name: String, val file: String)

private val models = listOf(
//    Model("Wash Machine", "models/xyj.glb"),
//    Model("Watch", "models/watch.glb"),
//    Model("Helmet", "models/DamagedHelmet.glb"),
    Model("Bulb", "models/Bulb.glb"),
    Model("Fiagena", "models/Fiagena.glb"),
    Model("Lungs", "models/Lungs.glb"),
    Model("Microscope", "models/Microscope.glb"),
    Model("Solar System", "models/solarsystem.glb")
)


@Composable
fun FilamentScreen() {

    var isDropdownExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedFiles by rememberSaveable { mutableStateOf(listOf<String>()) }
    val selectedModels = models.filter { it.file in selectedFiles }


    Column(Modifier.fillMaxSize().systemBarsPadding()) {

        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            Row(
                Modifier
                    .fillMaxWidth().height(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .clickable { isDropdownExpanded = !isDropdownExpanded }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedModels.size) {
                        0 -> "- - - Select Model - - -"
                        1 -> selectedModels.first().name
                        else -> "${selectedModels.size} models selected"
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Icon(
                    if (isDropdownExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                models.forEach { model ->
                    val checked = model.file in selectedFiles
                    DropdownMenuItem(
                        text = { Text(model.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
//                        enabled = checked || selectedFiles.size < MAX_SELECTED,
                        onClick = {
                            selectedFiles =
                                if (checked) selectedFiles - model.file
                                else selectedFiles + model.file
                        }
                    )
                }
                if (selectedFiles.isNotEmpty()) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Clear Selection", color = MaterialTheme.colorScheme.error) },
                        onClick = { selectedFiles = emptyList() }
                    )
                }
            }
        }
        // ===== FILAMENT ENGINE INITIALIZATION =====

        // Creates the core Filament rendering engine
        // Manages GPU rendering, material processing, and frame rendering
        val engine = rememberEngine()

        // Creates model loader for 3D files (GLB/glTF format)
        // Converts model files to Filament-compatible format
        val modelLoader = rememberModelLoader(engine)

        // Creates environment loader for HDR environment maps
        // Provides realistic lighting and reflections
        val environmentLoader = rememberEnvironmentLoader(engine)

        // Created ONCE, destroyed automatically on dispose
        val environment = rememberEnvironment(environmentLoader) {
            environmentLoader.createHDREnvironment("envs/sky_2k.hdr")!!
        }

        // ===== SCENE NODES SETUP =====

        // Camera node: Defines viewer perspective
        // Position: 4 units away (z=4f)
        val cameraNode = rememberCameraNode(engine) {
            position = Position(x = 0f, y = 0f, z = 6f)
        }

        // ===== ANIMATION SETUP =====

        // Creates infinite rotation animation
        // Rotates 360° around Y-axis (vertical) every 20 seconds
        val cameraTransition = rememberInfiniteTransition(label = "CameraTransition")
        val cameraRotation by cameraTransition.animateRotation(
            initialValue = Rotation(y = 0f),
            targetValue = Rotation(y = 360f),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 20.seconds.toInt(MILLISECONDS)),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(2.seconds.toInt(MILLISECONDS))
            )
        )

        // ===== MODEL LOADING =====


        // ===== MODEL RENDERING =====

        // Scene: Main composable for 3D rendering
        // - engine: Filament rendering engine
        // - modelLoader: Loads model from assets
        // - cameraNode: Camera position and angle
        // - environmentLoader: HDR environment (assets/envs/sky_2k.hdr)
        // - onFrame: Updates camera orbit position each frame
        // - onGestureListener: Double-tap to zoom (2x scale)
        Scene(
            modifier = Modifier.wrapContentHeight()
                .weight(1f)
                .clip(RoundedCornerShape(bottomEnd = 24.dp, bottomStart = 24.dp)),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            // Camera manipulator enables touch interactions (orbit, zoom)
            cameraManipulator = rememberCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition
            ),
            // HDR environment: Provides realistic lighting and reflections
            environment = environment,
            // onFrame: Called every frame to update camera orbit
//            onFrame = {
//                // Calculate orbit position based on animation rotation
//                val rad = Math.toRadians(cameraRotation.y.toDouble())
//                cameraNode.position = Position(
//                    x = (8f * sin(rad)).toFloat(),
//                    y = 0f,
//                    z = (8f * cos(rad)).toFloat()
//                )
//                cameraNode.lookAt(Position(0f, 0f, 0f)) // Keep camera focused on center
//            },
            // Gesture interactions: Double-tap to zoom
            onGestureListener = rememberOnGestureListener(
                onDoubleTap = { _, node ->
                    node?.apply {
                        scale *= 2.0f  // Double the model size
                    }
                }
            )
        ) {

            selectedModels.forEachIndexed { index, model ->
                key(model.file) {
                    val instance = rememberModelInstance(modelLoader, model.file)
                    val positions = remember { mutableStateMapOf<String, Position>() }
                    if(instance != null) {
                        ModelNode(
                            modelInstance = instance,
                            scaleToUnits = 1.0f,
                            centerOrigin = Position(0f, -0.5f, 0f),
                            position = positions[model.file]
                                ?: Position(x = (index - (selectedModels.size - 1) / 2f) * 2f), // Position(x = (index - (selectedModels.size - 1) / 2f) * 2f),
                            isEditable = true,
                            apply = {
                                isPositionEditable = true
                            }
                        )
                    }
                }

            }
        }

    }
}