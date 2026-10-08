package com.msoumaya.androidcoran.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.UUID

@Composable fun AdminContentsPanel(vm: CoranViewModel) {
    var type by remember { mutableStateOf("invocation") };var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var cats by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var dates by remember { mutableStateOf<List<JsonObject>>(emptyList()) };var draft by remember { mutableStateOf<JsonObject?>(null) };var category by remember { mutableStateOf<JsonObject?>(null) };var manage by remember { mutableStateOf(false) };var preview by remember { mutableStateOf(false) };var date by remember { mutableStateOf("") };var busy by remember { mutableStateOf(false) };var more by remember { mutableStateOf(false) };var deleting by remember { mutableStateOf<Pair<String,JsonObject>?>(null) };var uploaded by remember { mutableStateOf<List<String>>(emptyList()) };var active by remember { mutableStateOf(true) }
    val context=LocalContext.current;val media=remember(vm) { ContentMedia(vm.repo) }
    fun act(block: suspend ()->Unit) { if(busy) return;busy=true;vm.action { try { check(AdminService(vm.repo).authorized());block() } finally { busy=false } } }
    suspend fun load(append: Boolean=false) {
        cats=vm.repo.query("content_categories",orderBy="display_order",ascending=true,size=300)
        val page=vm.repo.query("daily_contents",mapOf("type" to type),offset=if(append) rows.size else 0,size=30);rows=if(append) rows+page else page;more=page.size==30
        dates=vm.repo.query("daily_content_schedule",after="display_date" to LocalDate.now().toString(),orderBy="display_date",ascending=true,size=300)
    }
    fun cancel() { val old=uploaded;uploaded=emptyList();draft=null;preview=false;vm.action { media.cleanUnused(old) } }
    fun attach(uri: android.net.Uri?,kind: String) { if(uri==null) return;act { val url=media.upload(context,uri,kind);if(active&&draft!=null) { uploaded=uploaded+url;draft=draft!!.with((if(kind=="image") "image_url" else "audio_url") to JsonPrimitive(url)) } else media.cleanUnused(listOf(url)) } }
    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { attach(it,"image") }
    val audioPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { attach(it,"audio") }
    DisposableEffect(Unit) { onDispose { active=false;val remaining=uploaded;vm.action { media.cleanUnused(remaining) } } }
    LaunchedEffect(type) { act { load() } }
    Text("Rappels et invocations",style=MaterialTheme.typography.headlineSmall)
    listOf("invocation" to "Invocations","reminder" to "Rappels").forEach { (id,label) -> FilterChip(selected=type==id,enabled=!busy,onClick={cancel();type=id},label={Text(label)}) }
    val d=draft
    if(d!=null) {
        TextButton(enabled=!busy,onClick={cancel()}) { Text("Annuler") }
        if(preview) {
            Panel(d.str("title"),d.str("french_text")) { Text(d.str("arabic_text"));Text(d.str("phonetic_text"));Text(d.str("explanation"));Text("${d.str("source")} ${d.str("reference")}");if(d.str("image_url").isNotBlank()) ContentImage(vm,d.str("image_url"),d.str("title")) }
        } else {
            fun change(key: String,value: JsonElement) { draft=d.with(key to value) }
            cats.filter { it.str("type")==type }.forEach { c -> FilterChip(selected=d.str("category_id")==c.str("id"),onClick={change("category_id",JsonPrimitive(c.str("id")))},label={Text(c.str("name"))}) }
            val fields=mutableListOf("title" to "Titre facultatif","french_text" to "Texte français obligatoire","source" to "Source obligatoire","reference" to "Référence","image_url" to "Image HTTPS","audio_url" to "Audio HTTPS")
            if(type=="invocation") fields.addAll(listOf("arabic_text" to "Arabe obligatoire","phonetic_text" to "Phonétique obligatoire","explanation" to "Explication"))
            fields.forEach { (key,label) -> OutlinedTextField(d.str(key),{change(key,JsonPrimitive(it))},label={Text(label)}) }
            TextButton(enabled=!busy,onClick={imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) { Text("Choisir une image · 5 Mo maximum") }
            if(type=="invocation") TextButton(enabled=!busy,onClick={audioPicker.launch(arrayOf("audio/*","video/mp4"))}) { Text("Choisir un audio MP3/M4A/AAC · 30 Mo maximum") }
            FilterChip(selected=d.flag("is_active"),onClick={change("is_active",JsonPrimitive(!d.flag("is_active")))},label={Text("Actif")})
            OutlinedTextField(date,{date=it},label={Text("Programmer le jour · AAAA-MM-JJ, facultatif")})
        }
        TextButton(onClick={preview=!preview}) { Text(if(preview) "Formulaire" else "Aperçu") }
        Button(enabled=!busy,onClick={act { validateAdminContent(d,date);val old=vm.repo.query("daily_contents",mapOf("id" to d.str("id")),size=1).firstOrNull();vm.repo.rpc("save_daily_content",json("p_content" to d,"p_date" to date.ifBlank { null }));val pending=uploaded;uploaded=emptyList();draft=null;preview=false;media.cleanUnused(pending+listOfNotNull(old?.str("image_url"),old?.str("audio_url")));load() }}) { Text("Enregistrer") }
    } else {
        Button(onClick={date="";draft=json("id" to UUID.randomUUID().toString(),"type" to type,"category_id" to (cats.firstOrNull { it.str("type")==type&&it.flag("is_active") }?.str("id")?:""),"french_text" to "","source" to "","is_active" to true)}) { Text("Ajouter un contenu") }
        TextButton(onClick={manage=!manage}) { Text("Gérer les catégories") }
        if(manage) {
            TextButton(onClick={category=json("id" to UUID.randomUUID().toString(),"type" to type,"name" to "","icon" to "☾","display_order" to cats.count { it.str("type")==type },"is_active" to true)}) { Text("Ajouter une catégorie") }
            category?.let { c ->
                OutlinedTextField(c.str("name"),{category=c.with("name" to JsonPrimitive(it))},label={Text("Nom obligatoire")})
                OutlinedTextField(c.str("icon"),{category=c.with("icon" to JsonPrimitive(it))},label={Text("Icône")})
                OutlinedTextField(c.num("display_order").toString(),{category=c.with("display_order" to JsonPrimitive(it.toIntOrNull()?:0))},label={Text("Ordre")})
                FilterChip(selected=c.flag("is_active"),onClick={category=c.with("is_active" to JsonPrimitive(!c.flag("is_active")))},label={Text("Active")})
                Button(enabled=!busy&&c.str("name").isNotBlank(),onClick={act { vm.repo.upsertRows("content_categories",c.with("name" to JsonPrimitive(c.str("name").trim())));category=null;load() }}) { Text("Enregistrer la catégorie") }
                TextButton(onClick={category=null}) { Text("Annuler") }
            }
            cats.filter { it.str("type")==type }.forEach { c -> Panel("${c.str("icon")} ${c.str("name")}","Ordre ${c.num("display_order")}") {
                TextButton(onClick={category=c}) { Text("Modifier") }
                TextButton(enabled=!busy,onClick={act { vm.repo.upsertRows("content_categories",c.with("is_active" to JsonPrimitive(!c.flag("is_active"))));load() }}) { Text(if(c.flag("is_active")) "Désactiver" else "Activer") }
                TextButton(onClick={deleting="content_categories" to c}) { Text("Supprimer") }
            } }
        }
        rows.forEach { row -> Panel(row.str("title",row.str("french_text").take(70)),cats.firstOrNull { it.str("id")==row.str("category_id") }?.str("name")?:"") {
            dates.filter { it.str("content_id")==row.str("id") }.forEach { schedule -> Text(schedule.str("display_date"));TextButton(enabled=!busy,onClick={act { vm.repo.deleteRows("daily_content_schedule",mapOf("type" to schedule.str("type"),"display_date" to schedule.str("display_date")));load() }}) { Text("Retirer cette programmation") } }
            TextButton(onClick={draft=row;date="";preview=false}) { Text("Modifier / programmer") }
            TextButton(onClick={draft=row;date="";preview=true}) { Text("Aperçu") }
            TextButton(enabled=!busy,onClick={act { vm.repo.updateRows("daily_contents",json("is_active" to !row.flag("is_active"),"updated_at" to java.time.Instant.now().toString()),mapOf("id" to row.str("id")));load() }}) { Text(if(row.flag("is_active")) "Désactiver" else "Activer") }
            TextButton(onClick={deleting="daily_contents" to row}) { Text("Supprimer") }
        } }
        if(more) Button(enabled=!busy,onClick={act { load(true) }}) { Text("Afficher la suite") }
    }
    deleting?.let { (table,row) -> AlertDialog(onDismissRequest={deleting=null},title={Text("Supprimer ?")},text={Text(if(table=="content_categories") "Une catégorie contenant des contenus ne peut pas être supprimée. Désactive-la pour la masquer." else "Le contenu et sa programmation seront supprimés.")},dismissButton={TextButton(onClick={deleting=null}) { Text("Annuler") }},confirmButton={TextButton(enabled=!busy,onClick={act { vm.repo.deleteRows(table,mapOf("id" to row.str("id")));deleting=null;if(table=="daily_contents") media.cleanUnused(listOf(row.str("image_url"),row.str("audio_url")));load() }}) { Text("Supprimer") }}) }
}
