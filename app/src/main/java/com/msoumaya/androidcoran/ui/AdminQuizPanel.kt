package com.msoumaya.androidcoran.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import com.msoumaya.androidcoran.data.AdminService
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun AdminQuizPanel(vm: CoranViewModel) {
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var sets by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var themed by rememberSaveable { mutableStateOf(false) };var draft by rememberSaveable(stateSaver=Saver<JsonObject?,String>(save={it?.toString()},restore={Json.parseToJsonElement(it).jsonObject})) { mutableStateOf<JsonObject?>(null) };var busy by remember { mutableStateOf(false) };var deleting by remember { mutableStateOf<JsonObject?>(null) }
    fun act(block: suspend ()->Unit) { if(busy) return;busy=true;vm.action { try { check(AdminService(vm.repo).authorized());block();rows=vm.repo.rpc("quiz_admin_list").jsonArray.map { it.jsonObject };sets=vm.repo.rpc("quiz_admin_sets").jsonArray.map { it.jsonObject } } finally { busy=false } } }
    fun save(d: JsonObject) { act { vm.repo.rpc(if(themed) "quiz_admin_save_set" else "quiz_admin_save",json((if(themed) "p_set" else "p_question") to if(themed) adminSetPayload(d) else adminQuestionPayload(d)));draft=null } }
    LaunchedEffect(Unit) { act {} }
    TextButton(onClick={draft=null;themed=!themed}) { Text(if(themed) "Questions" else "Quiz thématiques · 10 questions") }
    val d=draft
    if(d==null) {
        Button(onClick={draft=if(themed) json("id" to "","title" to "","category" to "Coran","questionIds" to listOf<String>(),"isActive" to true) else freshAdminQuestion()}) { Text("Créer") }
        (if(themed) sets else rows).forEach { q -> Panel(q.str(if(themed) "title" else "question"),q.str("category")) {
            TextButton(onClick={draft=q}) { Text("Modifier") }
            TextButton(onClick={draft=if(themed) q.with("id" to JsonPrimitive(""),"title" to JsonPrimitive(q.str("title")+" (copie)")) else q.with("id" to JsonPrimitive(""),"publicationDate" to JsonPrimitive(java.time.LocalDate.now().toString()),"isDailyQuestion" to JsonPrimitive(false))}) { Text("Dupliquer") }
            TextButton(onClick={save(q.with("isActive" to JsonPrimitive(!q.flag("isActive"))))},enabled=!busy) { Text(if(q.flag("isActive")) "Désactiver" else "Activer") }
            TextButton(onClick={deleting=q},enabled=!busy) { Text("Supprimer") }
        } }
    } else {
        fun change(key: String,value: JsonElement) { draft=d.with(key to value) }
        TextButton(onClick={draft=null}) { Text("Retour à la liste") }
        quizCategories.forEach { c -> FilterChip(selected=d.str("category")==c,onClick={change("category",JsonPrimitive(c))},label={Text(c)}) }
        if(themed) {
            OutlinedTextField(d.str("title"),{change("title",JsonPrimitive(it))},label={Text("Nom du quiz")})
            Text("${d.arr("questionIds").size} / 10 questions")
            rows.filter { it.flag("isActive")&&it.flag("availableForChallenges") }.forEach { q -> val ids=d.arr("questionIds");val id=JsonPrimitive(q.str("id"));FilterChip(selected=id in ids,onClick={change("questionIds",JsonArray(if(id in ids) ids.filter { it!=id } else if(ids.size<10) ids+id else ids))},label={Text(q.str("question"))}) }
        } else {
            listOf("question" to "Question","explanation" to "Explication","sourceTitle" to "Source obligatoire","sourceReference" to "Référence","sourceUrl" to "Lien HTTPS","arabic" to "Arabe","translation" to "Traduction","publicationDate" to "Date AAAA-MM-JJ").forEach { (key,label) -> OutlinedTextField(d.str(key),{change(key,JsonPrimitive(it))},label={Text(label)}) }
            listOf("surah" to "Sourate facultative","ayah" to "Verset facultatif").forEach { (key,label) -> OutlinedTextField((d[key] as? JsonPrimitive)?.contentOrNull?:"",{v -> if(v.isBlank()) change(key,JsonNull) else v.toIntOrNull()?.let { change(key,JsonPrimitive(it)) }},label={Text(label)}) }
            val answers=d.arr("answers").map { it.jsonObject }.let { if(it.size==3) it+json("id" to "D","text" to "") else it }
            answers.forEach { a -> OutlinedTextField(a.str("text"),{v -> change("answers",JsonArray(answers.map { if(it.str("id")==a.str("id")) it.with("text" to JsonPrimitive(v)) else it }))},label={Text("Réponse ${a.str("id")}")});FilterChip(selected=d.str("correctAnswerId")==a.str("id"),onClick={change("correctAnswerId",JsonPrimitive(a.str("id")))},label={Text("Bonne réponse ${a.str("id")}")}) }
            listOf("isDailyQuestion" to "Question du jour","availableForChallenges" to "Disponible pour les défis").forEach { (key,label) -> FilterChip(selected=d.flag(key),onClick={change(key,JsonPrimitive(!d.flag(key)))},label={Text(label)}) }
        }
        FilterChip(selected=d.flag("isActive"),onClick={change("isActive",JsonPrimitive(!d.flag("isActive")))},label={Text("Actif")})
        Button(onClick={save(d)},enabled=!busy&&runCatching { if(themed) adminSetPayload(d) else adminQuestionPayload(d) }.isSuccess) { Text("Enregistrer") }
    }
    deleting?.let { q -> AlertDialog(onDismissRequest={deleting=null},title={Text("Supprimer ?")},text={Text("Les réponses et défis déjà enregistrés conservent leur contenu.")},confirmButton={TextButton(onClick={act { vm.repo.rpc(if(themed) "quiz_admin_delete_set" else "quiz_admin_delete",json("p_id" to q.str("id")));deleting=null }}) { Text("Supprimer") }},dismissButton={TextButton(onClick={deleting=null}) { Text("Annuler") }}) }
}
