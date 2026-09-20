package com.a3dmodelviewer

import android.content.Context
import org.json.JSONObject
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PartLabel(val nodeName: String, val text: String)

/** GLB layout: 12-byte header, then chunk 0 = [length:u32][type:u32 "JSON"][json bytes]. */
fun readGlbLabels(context: Context, assetPath: String): List<PartLabel> =
    runCatching {
        DataInputStream(context.assets.open(assetPath).buffered()).use { input ->
            val head = ByteArray(20).also { input.readFully(it) }
            val bb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
            require(bb.getInt(0) == 0x46546C67)          // "glTF"
            require(bb.getInt(16) == 0x4E4F534A)         // "JSON"
            val json = ByteArray(bb.getInt(12)).also { input.readFully(it) }

            val nodes = JSONObject(String(json, Charsets.UTF_8)).optJSONArray("nodes")
                ?: return@use emptyList<PartLabel>()
            (0 until nodes.length()).mapNotNull { i ->
                val node = nodes.getJSONObject(i)
                val name = node.optString("name")
                val text = node.optJSONObject("extras")?.optString("prop").orEmpty()
                if (name.isNotEmpty() && text.isNotEmpty()) PartLabel(name, text) else null
            }
        }
    }.getOrDefault(emptyList())