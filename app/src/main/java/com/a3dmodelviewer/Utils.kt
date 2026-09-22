package com.a3dmodelviewer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context

@SuppressLint("ServiceCast")
fun supportsRequiredGles(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = am.deviceConfigurationInfo
    println("info.reqGlEsVersion = $info")
    return info.reqGlEsVersion >= 0x30000  // GLES 3.0+
}