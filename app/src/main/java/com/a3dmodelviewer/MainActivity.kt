package com.a3dmodelviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.a3dmodelviewer.ui.theme._3DModelViewerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _3DModelViewerTheme {
                if (supportsRequiredGles(this)) {
                    Workspace()
                } else {
                    GPUUnsupportedScreen(context = this)
                }
            }
        }
    }
}