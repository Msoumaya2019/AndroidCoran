package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.roundToInt

/** Overlay only: opening verse actions never participates in the paper's measurement. */
@Composable fun VerseFloatingBar(reference: String,anchor: Rect?,difficult: Boolean,onListen: ()->Unit,onDifficulty: ()->Unit,onNote: ()->Unit,onRecord: ()->Unit,onClose: ()->Unit,extra: (@Composable ()->Unit)?=null) {
 val density=LocalDensity.current
 BoxWithConstraints(Modifier.fillMaxSize()) {
  val compact=maxWidth<600.dp
  val barWidth=minOf(maxWidth-16.dp,850.dp)
  val barHeight=if(compact) {if(density.fontScale>1.3f) 96.dp else 68.dp} else 56.dp
  val widthPx=with(density) { barWidth.toPx() };val heightPx=with(density) { barHeight.toPx() }
  val gap=with(density) { 10.dp.toPx() };val edge=with(density) { 8.dp.toPx() }
  val availableWidth=with(density) { maxWidth.toPx() };val availableHeight=with(density) { maxHeight.toPx() }
  val a=anchor?:Rect(availableWidth/2,availableHeight*.55f,availableWidth/2,availableHeight*.55f)
  val x=(a.center.x-widthPx/2).coerceIn(edge,maxOf(edge,availableWidth-widthPx-edge))
  val preferredY=if(a.top-heightPx-gap>=edge) a.top-heightPx-gap else a.bottom+gap
  val y=preferredY.coerceIn(edge,maxOf(edge,availableHeight-heightPx-edge))
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
   Surface(Modifier.offset { IntOffset(x.roundToInt(),y.roundToInt()) }.width(barWidth).height(barHeight).testTag("verse-floating-bar"),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surface,shadowElevation=8.dp,border=BorderStroke(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=.12f))) {
    Row(Modifier.fillMaxSize().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
     if(!compact) Text(reference,Modifier.padding(horizontal=12.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
     @Composable fun action(label: String,icon: ImageVector,weight: Float=1f,active: Boolean=false,onClick: ()->Unit) {
      TextButton(onClick=onClick,contentPadding=PaddingValues(horizontal=if(compact) 2.dp else 8.dp),modifier=Modifier.weight(weight).fillMaxHeight().semantics { if(label=="Difficile") { selected=active;stateDescription=if(active) "Verset difficile" else "Verset non marqué difficile" } }) {
       if(compact) Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(icon,null,Modifier.size(18.dp));Text(label,fontSize=if(label=="Enregistrer ma récitation") 12.sp else 10.sp,maxLines=if(density.fontScale>1.3f) 3 else 2,textAlign=androidx.compose.ui.text.style.TextAlign.Center) }
       else { Icon(icon,null,Modifier.size(if(compact) 18.dp else 20.dp));Spacer(Modifier.width(4.dp));Text(label,fontSize=if(compact) 12.sp else 14.sp,maxLines=2) }
      }
     }
     action("Écouter",Icons.Default.PlayArrow,onClick=onListen)
     action("Difficile",if(difficult) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,active=difficult,onClick=onDifficulty)
     action("Note",Icons.Default.EditNote,onClick=onNote)
     action("Enregistrer ma récitation",Icons.Default.Mic,if(compact) 2.5f else 2.1f,onClick=onRecord)
     if(extra!=null) extra()
     IconButton(onClick=onClose,modifier=Modifier.size(if(compact) 36.dp else 44.dp)) { Icon(Icons.Default.Close,"Fermer les options du verset") }
    }
   }
  }
 }
}
