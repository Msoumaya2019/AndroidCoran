package com.msoumaya.androidcoran.data

import android.content.Context
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDate

class QuizService(private val repo: Repository,private val store: LocalStore,private val context: Context) {
    private val lock=Mutex()
    private val _snapshot=MutableStateFlow(emptyQuiz());val snapshot=_snapshot.asStateFlow()
    @Volatile var cacheOwner: String?=null;private set
    private fun cached(owner: String)=store.cached("$owner:quiz") as? JsonObject?:emptyQuiz()
    fun loadCached() { cacheOwner=repo.user.value;_snapshot.value=cacheOwner?.let(::cached)?:emptyQuiz() }
    suspend fun answer(question: JsonObject,answer: String) {
        val owner=repo.user.value?:error("Connecte-toi pour enregistrer ta réponse")
        val day=LocalDate.now().toString();val now=Instant.now().toString()
        store.transaction {
            val old=cached(owner);val next=recordDailyAnswer(old,question,answer,day,now)
            if(next!=old) {
                store.cache("$owner:quiz",next)
                store.enqueue(QueuedOperation("quiz:$day",owner,"quiz",json("p_question" to question.str("id"),"p_answer" to answer,"p_day" to day,"p_answered_at" to now),null,null))
                if(repo.user.value==owner) { cacheOwner=owner;_snapshot.value=next }
            }
        }
        OutboxWorker.enqueue(context,owner)
    }
    suspend fun refresh() = lock.withLock {
        val owner=repo.user.value?:return@withLock
        for(operation in store.pending(owner,"quiz")) {
            check(repo.user.value==owner) { "Le compte a changé" };repo.rpc("quiz_answer_daily",operation.payload)
            check(repo.user.value==owner) { "Le compte a changé" };store.acknowledge(owner,operation.id)
        }
        val remote=repo.rpc("quiz_snapshot",json("p_day" to LocalDate.now().toString())).jsonObject
        check(repo.user.value==owner) { "Le compte a changé" }
        store.transaction { val merged=mergeQuizSnapshot(remote,cached(owner));store.cache("$owner:quiz",merged);cacheOwner=owner;_snapshot.value=merged }
    }
    suspend fun challenge(opponent: String,count: Int,set: String?): String {
        require(count in listOf(5,10)&&opponent.isNotBlank())
        val id=repo.rpc("quiz_create_challenge",json("p_opponent" to opponent,"p_count" to count,"p_set" to set)).jsonPrimitive.content
        refresh();return id
    }
    suspend fun challengeAnswer(id: String,question: String,answer: String) { repo.rpc("quiz_answer_challenge",json("p_challenge" to id,"p_question" to question,"p_answer" to answer));refresh() }
    fun pending(owner: String)=store.pending(owner,"quiz").isNotEmpty()||cached(owner).arr("responses").any { it.jsonObject.flag("pending") }
}
