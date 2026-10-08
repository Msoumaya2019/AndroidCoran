package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msoumaya.androidcoran.domain.*

@Composable fun ReaderAudioControls(q: Quran,current: AudioPosition?,isPlaying: Boolean,progress: AudioProgress?,preferences: RepeatPreferences?,onSettings: ()->Unit,onCommand: (String)->Unit) {
    var dock by rememberSaveable(current!=null) { mutableStateOf("expanded") }
    fun settings() { dock="expanded";onSettings() }
    if(current!=null&&dock=="hidden") {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) { IconButton(onClick={dock="mini"}) { Icon(Icons.Default.Headphones,"Rouvrir le lecteur audio") } }
        return
    }
    if(current!=null&&dock=="mini") {
        Row(Modifier.fillMaxWidth().padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { dock="expanded" }.semantics { contentDescription="Développer le lecteur" }.padding(6.dp)) {
                Text(q.surah(current.verseId).name,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=13.sp)
                Text("Verset "+q.verse(current.verseId).ayah,fontSize=11.sp)
            }
            IconButton(onClick={onCommand("PREVIOUS_VERSE")}) { Icon(Icons.Default.SkipPrevious,"Verset précédent") }
            IconButton(onClick={onCommand("TOGGLE")}) { Icon(if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,"Lecture / pause") }
            IconButton(onClick={onCommand("NEXT_VERSE")}) { Icon(Icons.Default.SkipNext,"Verset suivant") }
            IconButton(onClick={settings()}) { Icon(Icons.Default.Tune,"Répétition et vitesse") }
            IconButton(onClick={dock="hidden"}) { Icon(Icons.Default.ExpandMore,"Masquer le lecteur") }
        }
        return
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Button(onClick={settings()},modifier=Modifier.padding(6.dp)) { Text("Audio") }
        if(current!=null) {
            val total=if(preferences?.count==null||preferences?.autoStop==false) "∞" else preferences.count.toString()
            Text("Verset "+q.verse(current.verseId).ayah+" · "+current.repetition+"/"+total,Modifier.weight(1f),fontSize=13.sp)
            IconButton(onClick={onCommand("TOGGLE")}) { Icon(if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,"Lecture / pause") }
            IconButton(onClick={onCommand("STOP")}) { Icon(Icons.Default.Stop,"Arrêter") }
            IconButton(onClick={dock="mini"}) { Icon(Icons.Default.ExpandMore,"Réduire le lecteur") }
        }
    }
    if(current!=null&&progress!=null) Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        val seconds=progress.elapsedMs/1000
        Text((seconds/60).toString()+":"+(seconds%60).toString().padStart(2,'0'),fontSize=11.sp)
        LinearProgressIndicator(progress={progress.fraction},modifier=Modifier.weight(1f))
        Text(progress.verseIndex.toString()+"/"+progress.verseCount,fontSize=11.sp)
    }
    if(current!=null) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
        IconButton(onClick={onCommand("PREVIOUS_VERSE")}) { Icon(Icons.Default.SkipPrevious,"Verset précédent") }
        IconButton(onClick={onCommand("RESTART")}) { Icon(Icons.Default.Replay,"Recommencer le passage") }
        IconButton(onClick={onCommand("NEXT_VERSE")}) { Icon(Icons.Default.SkipNext,"Verset suivant") }
        IconButton(onClick={dock="hidden"}) { Icon(Icons.Default.VisibilityOff,"Masquer le lecteur") }
    }
}
