package com.msoumaya.androidcoran.ui
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val EraserIcon: ImageVector by lazy {
 ImageVector.Builder("Gomme",24.dp,24.dp,24f,24f).apply {
  path(fill=androidx.compose.ui.graphics.SolidColor(Color.Black),pathFillType=PathFillType.EvenOdd) {
   moveTo(15f,2f);lineTo(23f,10f);lineTo(12f,21f);lineTo(6f,21f);lineTo(1f,16f);close()
   moveTo(4f,16f);lineTo(7f,19f);lineTo(11f,19f);lineTo(15f,15f);lineTo(10f,10f);close()
   moveTo(14f,20f);lineTo(23f,20f);lineTo(23f,22f);lineTo(14f,22f);close()
  }
 }.build()
}
