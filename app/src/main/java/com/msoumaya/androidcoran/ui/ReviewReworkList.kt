package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.*
@Composable fun ReviewReworkList(q: Quran,tasks: List<ReviewTask>,onOpen: (ReviewTask)->Unit) {
 var all by rememberSaveable { mutableStateOf(false) }
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("À retravailler",style=MaterialTheme.typography.titleLarge)
  if(tasks.isEmpty()) Text("Aucun verset prioritaire prévu aujourd’hui.")
  (if(all) tasks else tasks.take(5)).forEach { task -> Panel(q.reference(task.range),"${task.range.ids.size} versets · marqué lors d’une révision précédente",{onOpen(task)}) }
  if(tasks.size>5) TextButton(onClick={all=!all}) { Text(if(all) "Réduire" else "Voir tous les versets prioritaires") }
 }
}
