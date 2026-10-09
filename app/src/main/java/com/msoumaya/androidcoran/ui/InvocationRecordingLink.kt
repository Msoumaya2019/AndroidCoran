package com.msoumaya.androidcoran.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.JsonObject

@Composable fun InvocationRecordingLink(recording: JsonObject,onView: ((String)->Unit)?) {
    val arabic=recording.obj("invocation_snapshot").str("arabic_text")
    if(arabic.isNotBlank()) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(arabic,Modifier.fillMaxWidth(),fontSize=24.sp,lineHeight=42.sp,textAlign=TextAlign.Center)
    }
    val id=recording.str("invocation_id")
    if(id.isNotBlank()&&onView!=null) TextButton(onClick={onView(id)}) { Text("Voir l’invocation") }
}
