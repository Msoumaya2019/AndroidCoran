package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msoumaya.androidcoran.domain.*

@Composable fun tabletReaderMode(p: TabletPreferences): Boolean {
 val c=LocalConfiguration.current;val context=LocalContext.current;val density=context.resources.displayMetrics.density
 val shortest=if(android.os.Build.VERSION.SDK_INT>=30) context.getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.let {minOf(it.width(),it.height())/density} else c.smallestScreenWidthDp.toFloat()
 return shortest>=600&&tabletLayout(p,if(shortest>=600) "tablet" else "phone",c.screenWidthDp.toFloat(),c.screenHeightDp.toFloat()).enabled
}
@Composable fun CompactTabletReaderHeader(title: String,active: String,onBack: ()->Unit,onChoosePage: ()->Unit,onFullscreen: ()->Unit,onOptions: ()->Unit,onTab: (String)->Unit) {
 Surface(Modifier.fillMaxWidth().testTag("tablet-compact-header"),color=MaterialTheme.colorScheme.surface) {
  Column {
   Row(Modifier.fillMaxWidth().height(48.dp).testTag("tablet-header-navigation"),verticalAlignment=Alignment.CenterVertically) {
    IconButton(onClick=onBack) {Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour")}
    TextButton(onClick=onChoosePage,modifier=Modifier.weight(1f)) {Text(title,maxLines=1,fontSize=17.sp)}
    IconButton(onClick=onFullscreen) {Icon(Icons.Default.Fullscreen,"Lecture plein écran")}
    IconButton(onClick=onOptions,modifier=Modifier.testTag("tablet-reader-options")) {Icon(Icons.Default.MoreVert,"Options de lecture")}
   }
   Row(Modifier.fillMaxWidth().height(48.dp).testTag("tablet-header-tabs"),verticalAlignment=Alignment.CenterVertically) {
    val tabs=listOf(Triple("lecture","Lecture",Icons.Default.MenuBook),Triple("annoter","Annoter",Icons.Default.Edit),Triple("notes","Mes notes",Icons.Default.StickyNote2),Triple("revisions","Révisions",Icons.Default.EventNote))
    tabs.forEach { (key,label,icon) ->
     val selected=active==key
     Surface(Modifier.weight(1f).padding(horizontal=5.dp,vertical=4.dp),shape=RoundedCornerShape(24.dp),color=if(selected) MaterialTheme.colorScheme.primary else Color.Transparent) {
      TextButton(onClick={onTab(key)},modifier=Modifier.fillMaxSize().testTag("tablet-tab-"+key).semantics {this.selected=selected},contentPadding=PaddingValues(horizontal=4.dp),colors=ButtonDefaults.textButtonColors(contentColor=if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)) {
       Icon(icon,null,Modifier.size(20.dp));Spacer(Modifier.width(6.dp));Text(label,maxLines=1,fontSize=14.sp)
      }
     }
    }
   }
  }
 }
}
