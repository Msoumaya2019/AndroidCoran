package com.msoumaya.androidcoran.ui
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.msoumaya.androidcoran.data.AdminService
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun AdminQuizResponsesPanel(vm: CoranViewModel,questionId: String?,onClose: ()->Unit) {
    val owner by vm.repo.user.collectAsStateWithLifecycle()
    var rows by remember(owner,questionId) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) };var more by remember { mutableStateOf(false) }
    fun load(offset: Int) { if(busy) return;busy=true;vm.action { try {
        check(AdminService(vm.repo).authorized())
        val result=vm.repo.rpc("quiz_admin_responses",json("p_question" to questionId,"p_offset" to offset)).jsonArray.map { it.jsonObject }
        if(vm.repo.user.value==owner) { rows=if(offset==0) result else rows+result;more=result.size==50 }
    } finally { busy=false } } }
    LaunchedEffect(owner,questionId) { if(owner!=null) load(0) }
    TextButton(onClick=onClose) { Text("Gestion des quiz") }
    Text("Réponses des utilisateurs",style=MaterialTheme.typography.titleLarge)
    Text("Question du jour, solo et défis · questions historiques")
    Button(enabled=!busy,onClick={load(0)}) { Text("Actualiser les réponses") }
    rows.forEach { row -> val question=row.obj("question")
        Panel(row.str("user_name"),row.str("mode")+" · "+row.str("answered_at")) {
            Text("Compte : "+row.str("user_id")+" · Quiz : "+row.str("quiz_id"))
            Text(question.str("question"))
            Text("Réponse : "+(question.arr("answers").map { it.jsonObject }.firstOrNull { it.str("id")==row.str("answer_id") }?.str("text")?:row.str("answer_id")))
            QuizCorrection(question,row.with("isCorrect" to JsonPrimitive(row.flag("is_correct"))))
        }
    }
    if(busy) CircularProgressIndicator() else if(more) Button(onClick={load(rows.size)}) { Text("Afficher les réponses suivantes") }
    else if(rows.isEmpty()) Text("Aucune réponse enregistrée")
}
