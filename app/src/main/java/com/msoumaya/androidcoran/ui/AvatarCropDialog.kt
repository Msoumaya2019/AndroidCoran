package com.msoumaya.androidcoran.ui
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.*
@Composable fun AvatarCropDialog(bitmap: Bitmap?,busy: Boolean,onClose: ()->Unit,onConfirm: (AvatarCrop)->Unit) {
 var x by rememberSaveable { mutableFloatStateOf(0.5f) };var y by rememberSaveable { mutableFloatStateOf(0.5f) };var zoom by rememberSaveable { mutableFloatStateOf(1f) }
 AlertDialog(onDismissRequest={if(!busy) onClose()},title={Text("Recadrer ma photo")},text={
  Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   if(bitmap==null) CircularProgressIndicator() else Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
    val crop=avatarCropRect(bitmap.width,bitmap.height,AvatarCrop(x,y,zoom))
    drawImage(bitmap.asImageBitmap(),srcOffset=IntOffset(crop.left,crop.top),srcSize=IntSize(crop.side,crop.side),dstSize=IntSize(size.width.toInt(),size.height.toInt()))
   }
   Text("Position horizontale");Slider(x,{x=it},enabled=!busy&&bitmap!=null)
   Text("Position verticale");Slider(y,{y=it},enabled=!busy&&bitmap!=null)
   Text("Zoom");Slider(zoom,{zoom=it},valueRange=1f..3f,enabled=!busy&&bitmap!=null)
   TextButton(onClick={x=0.5f;y=0.5f;zoom=1f},enabled=!busy) { Text("Recentrer") }
  }
 },confirmButton={TextButton(enabled=bitmap!=null&&!busy,onClick={onConfirm(AvatarCrop(x,y,zoom))}) { Text(if(busy) "Enregistrement…" else "Utiliser cette photo") }},dismissButton={TextButton(enabled=!busy,onClick=onClose) { Text("Annuler") }})
}
