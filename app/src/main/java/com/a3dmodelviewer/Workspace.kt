package com.a3dmodelviewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.Engine
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MODEL_DIR = "models"

private class PlacedModel(val id: Int, val assetPath: String, val title: String)

@Composable
fun Workspace() {
    val context = LocalContext.current

    // ONE engine, loader and environment shared by every container
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val environment = rememberEnvironment(environmentLoader) {
        environmentLoader.createHDREnvironment("envs/sky_2k.hdr")!!
    }

    val available = remember {
        context.assets.list(MODEL_DIR).orEmpty().filter { it.endsWith(".glb", true) }.sorted()
    }
    val placed = remember { mutableStateListOf<PlacedModel>() }
    var nextId by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        placed.forEachIndexed { index, model ->
            key(model.id) {
                ModelContainer(
                    model = model,
                    index = index,
                    engine = engine,
                    modelLoader = modelLoader,
                    environment = environment,
                    screenW = screenW,
                    screenH = screenH,
                    onClose = { placed.remove(model) }
                )
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(16.dp)) {
            Button(onClick = { menuOpen = true }) { Text("Add model") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                available.forEach { file ->
                    DropdownMenuItem(
                        text = { Text(file.removeSuffix(".glb")) },
                        onClick = {
                            placed += PlacedModel(nextId++, "$MODEL_DIR/$file", file.removeSuffix(".glb"))
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelContainer(
    model: PlacedModel,
    index: Int,
    engine: Engine,
    modelLoader: ModelLoader,
    environment: Environment,
    screenW: Float,
    screenH: Float,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val minSize = with(density) { 120.dp.toPx() }
    val maxSize = minOf(screenW, screenH)

    // ---- container state ----
    var sizePx by remember { mutableFloatStateOf(with(density) { 220.dp.toPx() }) }
    var offset by remember {
        mutableStateOf(Offset(40f + 48f * (index % 6), 160f + 48f * (index % 6)))
    }
    fun clamp(o: Offset, size: Float) = Offset(
        o.x.coerceIn(0f, (screenW - size).coerceAtLeast(0f)),
        o.y.coerceIn(0f, (screenH - size).coerceAtLeast(0f))
    )

    // ---- mode + 3D state ----
    var interaction by remember { mutableStateOf(false) }
    var labelsOn by remember { mutableStateOf(false) }      // hidden on load
    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var distance by remember { mutableFloatStateOf(2.5f) }  // camera zoom
    var labelPoints by remember { mutableStateOf(emptyList<LabelPoint>()) }

    // ---- 3D resources ----
    val instance = rememberModelInstance(modelLoader, model.assetPath)
    val cameraNode = rememberCameraNode(engine) { position =
        io.github.sceneview.math.Position(0f, 0f, 2.5f)
    }
    val projector = remember(engine) { LabelProjector(engine) }

    val labels by produceState(emptyList<PartLabel>(), model.assetPath) {
        value = withContext(Dispatchers.IO) { readGlbLabels(context, model.assetPath) }
    }
    val targets = remember(instance, labels) {
        instance?.let { inst ->
            labels.mapNotNull { l ->
                inst.asset.getFirstEntityByName(l.nodeName).takeIf { it != 0 }?.let { l to it }
            }
        }.orEmpty()
    }

    Box(
        Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }  // layout phase only
            .size(with(density) { sizePx.toDp() })
            .border(
                2.dp,
                if (interaction) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
    ) {
        // ---- 3D view (touch handled by the overlay below, not by SceneView) ----
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            environment = environment,
            onGestureListener = null,
            onFrame = {
                if (abs(cameraNode.position.z - distance) > 1e-4f) {
                    cameraNode.position = io.github.sceneview.math.Position(0f, 0f, distance)
                }
                if (labelsOn && targets.isNotEmpty()) {
                    labelPoints = projector.project(cameraNode.camera, targets, sizePx, sizePx)
                } else if (labelPoints.isNotEmpty()) {
                    labelPoints = emptyList()
                }
            }
        ) {
            if (instance != null) {
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.0f,
                    centerOrigin = io.github.sceneview.math.Position(0f, 0f, 0f),
                    rotation = io.github.sceneview.math.Rotation(x = pitch, y = yaw)
                )
            }
        }

        // ---- part labels, 2D, drawn over the 3D view ----
        if (labelsOn) LabelOverlay(labelPoints, Modifier.fillMaxSize())

        // ---- gesture layer: the ONLY place where the two modes are decided ----
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1f) {
                                if (interaction) {
                                    distance = (distance / zoom).coerceIn(0.8f, 8f)
                                } else {
                                    val newSize = (sizePx * zoom).coerceIn(minSize, maxSize)
                                    val d = newSize - sizePx
                                    offset = clamp(offset - Offset(d / 2f, d / 2f), newSize)
                                    sizePx = newSize                    // content scales with the view
                                }
                            }
                        } else if (fingers == 1) {
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                if (interaction) {
                                    yaw += pan.x * 0.4f
                                    pitch = (pitch + pan.y * 0.4f).coerceIn(-89f, 89f)
                                } else {
                                    offset = clamp(offset + pan, sizePx)
                                }
                            }
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
        )

        // ---- three always-visible buttons (above the gesture layer) ----
        Row(
            Modifier.align(Alignment.TopEnd).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RoundButton("↻", active = interaction) { interaction = !interaction }
            RoundButton("Aa", active = labelsOn) { labelsOn = !labelsOn }
            RoundButton("✕", active = false, onClick = onClose)
        }
    }
}

@Composable
private fun RoundButton(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 12.sp,
            color = if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LabelOverlay(points: List<LabelPoint>, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 11.sp, color = Color.White)
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val reach = 36.dp.toPx()
        val pad = 4.dp.toPx()

        points.forEach { p ->
            // push the label away from the container centre so it doesn't cover the part
            val dir = (p.anchor - center).let {
                val len = it.getDistance()
                if (len < 1f) Offset(0f, -1f) else it / len
            }
            val end = p.anchor + dir * reach
            val text = measurer.measure(p.text, style)
            val w = text.size.width + pad * 2
            val h = text.size.height + pad * 2
            val left = (if (dir.x < 0) end.x - w else end.x).coerceIn(0f, size.width - w)
            val top = (end.y - h / 2f).coerceIn(0f, size.height - h)

            drawLine(Color.White, p.anchor, end, strokeWidth = 1.5.dp.toPx())
            drawCircle(Color.White, 3.dp.toPx(), p.anchor)
            drawRoundRect(
                Color.Black.copy(alpha = 0.7f), Offset(left, top),
                Size(w, h), CornerRadius(4.dp.toPx())
            )
            drawText(text, topLeft = Offset(left + pad, top + pad))
        }
    }
}