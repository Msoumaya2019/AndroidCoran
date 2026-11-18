package com.msoumaya.androidcoran.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

class SoloQuiz(private val owner: String?,private val currentOwner: ()->String?,private val rpc: suspend (String,JsonObject)->JsonElement) {
    private val lock=Mutex()
    private val mutable=MutableStateFlow(json())
    val state=mutable.asStateFlow()
    private fun checkOwner() { check(owner!=null&&currentOwner()==owner) { "Connecte-toi pour jouer en solo" } }
    suspend fun start()=lock.withLock {
        checkOwner();val run=rpc("quiz_start_solo",json()).jsonObject;checkOwner()
        require(run.str("id").isNotBlank()&&run.arr("questions").isNotEmpty()) { "Aucune question disponible" }
        mutable.value=json("run" to run,"index" to 0,"answers" to json())
    }
    suspend fun answer(answer: String)=lock.withLock {
        checkOwner();val s=mutable.value;val q=s.obj("run").arr("questions").getOrNull(s.num("index"))?.jsonObject?:return@withLock
        if(s.obj("answers")[q.str("id")]!=null) return@withLock
        require(q.arr("answers").any { it.jsonObject.str("id")==answer })
        val result=rpc("quiz_answer_solo",json("p_run" to s.obj("run").str("id"),"p_question" to q.str("id"),"p_answer" to answer)).jsonObject
        checkOwner();mutable.value=s.with("answers" to s.obj("answers").with(q.str("id") to result))
    }
    fun next() { checkOwner();val s=mutable.value;val q=s.obj("run").arr("questions").getOrNull(s.num("index"))?.jsonObject?:return
        if(s.obj("answers")[q.str("id")]!=null) mutable.value=s.with("index" to JsonPrimitive(s.num("index")+1))
    }
    fun reset() { mutable.value=json() }
}
