package com.msoumaya.androidcoran.ui
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

@Composable fun SoloQuizPanel(vm: CoranViewModel,solo: SoloQuiz,authenticated: Boolean) {
    val s by solo.state.collectAsState();var busy by remember { mutableStateOf(false) }
    fun act(block: suspend ()->Unit) { if(busy) return;busy=true;vm.action { try { block() } finally { busy=false } } }
    Text("Quiz solo",style=MaterialTheme.typography.headlineSmall)
    val run=s.obj("run");val questions=run.arr("questions");val index=s.num("index")
    if(run.isEmpty()) Button(enabled=authenticated&&!busy,onClick={act { solo.start() }}) { Text("Commencer · 10 questions aléatoires") }
    else if(index>=questions.size) Panel("Quiz terminé") {
        Text(s.obj("answers").values.count { it.jsonObject.flag("isCorrect") }.toString()+" / "+questions.size+" bonnes réponses")
        Button(onClick={solo.reset()}) { Text("Nouvelle série") }
    } else {
        val q=questions[index].jsonObject;val response=s.obj("answers")[q.str("id")] as? JsonObject
        Text((index+1).toString()+" / "+questions.size+" · "+q.str("category"))
        Text(q.str("question"),style=MaterialTheme.typography.titleLarge)
        QuizAnswers(response?.obj("question")?:q,response?.str("selectedAnswerId"),authenticated&&!busy&&response==null) { answer -> act { solo.answer(answer) } }
        if(response!=null) {
            QuizCorrection(response.obj("question"),response)
            Button(onClick={solo.next()}) { Text(if(index+1==questions.size) "Voir mon résultat" else "Question suivante") }
        }
    }
}
