package com.a3dmodelviewer

import androidx.compose.ui.geometry.Offset
import com.google.android.filament.Camera
import com.google.android.filament.Engine

data class LabelPoint(val text: String, val anchor: Offset)

class LabelProjector(private val engine: Engine) {
    private val view = FloatArray(16)
    private val proj = DoubleArray(16)
    private val world = FloatArray(16)

    fun project(
        camera: Camera,
        targets: List<Pair<PartLabel, Int>>,   // label + Filament entity
        width: Float,
        height: Float
    ): List<LabelPoint> {
        camera.getViewMatrix(view)
        camera.getProjectionMatrix(proj)
        val tm = engine.transformManager
        val out = ArrayList<LabelPoint>(targets.size)

        for ((label, entity) in targets) {
            val ti = tm.getInstance(entity)
            if (ti == 0) continue
            tm.getWorldTransform(ti, world)
            val x = world[12]; val y = world[13]; val z = world[14]   // node world position

            // world -> view (column-major)
            val vx = view[0] * x + view[4] * y + view[8] * z + view[12]
            val vy = view[1] * x + view[5] * y + view[9] * z + view[13]
            val vz = view[2] * x + view[6] * y + view[10] * z + view[14]
            // view -> clip
            val cx = proj[0] * vx + proj[4] * vy + proj[8] * vz + proj[12]
            val cy = proj[1] * vx + proj[5] * vy + proj[9] * vz + proj[13]
            val cw = proj[3] * vx + proj[7] * vy + proj[11] * vz + proj[15]
            if (cw <= 0.0) continue                                     // behind camera

            val ndcX = (cx / cw).toFloat()
            val ndcY = (cy / cw).toFloat()
            out += LabelPoint(
                label.text,
                Offset((ndcX * 0.5f + 0.5f) * width, (1f - (ndcY * 0.5f + 0.5f)) * height)
            )
        }
        return out
    }
}

