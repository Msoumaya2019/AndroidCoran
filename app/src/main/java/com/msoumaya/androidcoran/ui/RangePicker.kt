package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.msoumaya.androidcoran.domain.*

@Composable fun RangePicker(q: Quran,actionLabel: String,onRange: (VerseRange)->Unit) {
    var type by rememberSaveable { mutableStateOf("Sourate") }
    var number by rememberSaveable { mutableStateOf("114") }
    var first by rememberSaveable { mutableStateOf("6231") }
    var last by rememberSaveable { mutableStateOf("6236") }
    var error by rememberSaveable { mutableStateOf("") }
    Column {
        Row { listOf("Sourate","Hizb","Juz","Versets").forEach { item ->
            FilterChip(selected=type==item,onClick={type=item;number=when(item) { "Sourate"->"114";"Hizb"->"60";else->"30" };error=""},label={Text(item)})
        } }
        if(type=="Versets") {
            OutlinedTextField(first,{first=it},label={Text("Premier verset global (1–6236)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            OutlinedTextField(last,{last=it},label={Text("Dernier verset global")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        } else OutlinedTextField(number,{number=it},label={Text("Numéro de ${type.lowercase()}")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        Button(onClick={
            val range=runCatching {
                when(type) {
                    "Sourate" -> q.surahs[number.toInt()-1].range
                    "Hizb" -> q.hizbs[number.toInt()-1]
                    "Juz" -> q.juzs[number.toInt()-1]
                    else -> VerseRange(first.toInt(),last.toInt())
                }
            }.getOrNull()
            if(range==null) error="Sélection invalide : vérifie les numéros." else { error="";onRange(range) }
        }) { Text(actionLabel) }
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
    }
}

@Composable fun KnowledgePicker(vm: CoranViewModel) {
    var mastery by rememberSaveable { mutableStateOf("perfect") }
    Text("Connaissances existantes")
    Row { listOf("perfect" to "Acquis","review" to "À réviser","learning" to "À apprendre").forEach { (key,label) ->
        FilterChip(selected=mastery==key,onClick={mastery=key},label={Text(label)})
    } }
    RangePicker(vm.repo.quran,"Appliquer aux connaissances") { range ->
        vm.action { vm.repo.mutate { markKnowledge(it,range,mastery) };vm.repo.feedback("Connaissances enregistrées") }
    }
}
