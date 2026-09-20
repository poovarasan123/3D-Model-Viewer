package com.a3dmodelviewer

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.a3dmodelviewer.ui.theme._3DModelViewerTheme

@Composable
fun AppButton(
    title: String,
    modifier: Modifier  = Modifier,
    isChecked:Boolean = false,
    onClickModel:()->Unit
){
    if (isChecked){

        OutlinedButton(
            onClick = {
                onClickModel.invoke()
            },
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Text(title)
        }
    }else{
        TextButton(
            onClick = {
                onClickModel.invoke()
            },
            modifier = modifier,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
        ) {
            Text(title)
        }
    }
}

@Preview
@Composable
private fun AppButtonPreview() {
    _3DModelViewerTheme {
        AppButton(title = "Model", onClickModel = {})
    }
}