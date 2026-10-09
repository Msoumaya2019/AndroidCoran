package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.domain.Quran
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReaderSurahPicker(q: Quran,page: Int,source: String,onClose: ()->Unit,onPage: (Int)->Unit,onSurah: (Int)->Unit) {
 var pageText by rememberSaveable(page) { mutableStateOf(page.toString()) };var error by rememberSaveable { mutableStateOf(false) }
 val current=q.verse(q.sourceRange(page,source).start).surah
 val scroll=rememberLazyListState(initialFirstVisibleItemIndex=(current-1).coerceAtLeast(0))
 ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().heightIn(max=680.dp).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Choisir une sourate",style=MaterialTheme.typography.headlineSmall);Text("Les 114 sourates du Coran")
   Row(verticalAlignment=Alignment.CenterVertically) {
    OutlinedTextField(pageText,{pageText=it;error=false},label={Text("Page 1 à 604")},isError=error,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.weight(1f))
    TextButton(onClick={val next=pageText.trim().toIntOrNull();if(next==null||next !in 1..604) error=true else onPage(next)}) { Text("Aller à la page") }
   }
   if(error) Text("Choisis une page entre 1 et 604.",color=MaterialTheme.colorScheme.error)
   LazyColumn(state=scroll,modifier=Modifier.weight(1f,false)) {
    items(q.surahs,key={it.number}) { surah ->
     Surface(color=if(surah.number==current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
      Row(Modifier.fillMaxWidth().semantics { selected=surah.number==current }.clickable { onSurah(surah.range.start) }.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
       Text(surah.number.toString(),Modifier.width(32.dp));Column(Modifier.weight(1f)) { Text(surah.name);Text("${surah.range.ids.size} versets",style=MaterialTheme.typography.bodySmall) };Text(surah.arabic,style=MaterialTheme.typography.titleLarge)
      }
     }
    }
   }
   TextButton(onClick=onClose) { Text("Fermer") }
  }
 }
}
