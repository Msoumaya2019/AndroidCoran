package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.Quran
@Composable fun SelectedVerseActions(q: Quran,id: Int,userMarked: Boolean,onBookmark: ()->Unit,onListen: ()->Unit,onDifficulty: ()->Unit,onClose: ()->Unit) {
 Column(Modifier.fillMaxWidth()) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Text("${q.verse(id).surah}:${q.verse(id).ayah}",Modifier.padding(8.dp))
   TextButton(onClick=onBookmark) { Text("Marquer") }
   TextButton(onClick=onListen) { Text("Écouter") }
   TextButton(onClick=onClose) { Text("Fermer") }
  }
  TextButton(onClick=onDifficulty,modifier=Modifier.fillMaxWidth()) { Text(if(userMarked) "Retirer des révisions prioritaires" else "Marquer comme difficile") }
 }
}
@Composable fun ReviewValidationActions(onGrade: (String)->Unit) {
 Column(Modifier.fillMaxWidth().padding(8.dp)) {
  Button(onClick={onGrade("perfect")},modifier=Modifier.fillMaxWidth()) { Text("Valider jusqu’au verset sélectionné") }
  Row(Modifier.fillMaxWidth()) {
   TextButton(onClick={onGrade("hesitant")},modifier=Modifier.weight(1f)) { Text("Quelques hésitations") }
   TextButton(onClick={onGrade("rework")},modifier=Modifier.weight(1f)) { Text("À retravailler") }
  }
 }
}
