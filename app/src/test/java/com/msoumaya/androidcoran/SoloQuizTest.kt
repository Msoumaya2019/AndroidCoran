package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class SoloQuizTest {
 private val question=json("id" to "q","answers" to listOf(json("id" to "A","text" to "A"),json("id" to "B","text" to "B")))
 @Test fun correctionComesOnlyAfterServerAnswerAndFirstChoiceIsKept()=runBlocking {
  var answered=0
  val solo=SoloQuiz("owner",{"owner"}) { name,args -> if(name=="quiz_start_solo") json("id" to "run","questions" to listOf(question)) else {
   assertEquals("quiz_answer_solo",name);assertEquals("run",args.str("p_run"));assertEquals("q",args.str("p_question"));assertEquals("A",args.str("p_answer"));answered++
   json("question" to question,"selectedAnswerId" to "A","isCorrect" to true)
  } }
  solo.start();assertTrue(solo.state.value.obj("answers").isEmpty());solo.next();assertEquals(0,solo.state.value.num("index"))
  solo.answer("A");solo.answer("B");assertEquals(1,answered);solo.next();assertEquals(1,solo.state.value.num("index"))
 }
 @Test fun changedAccountCannotReceivePreviousRun()=runBlocking {
  var owner="one";val solo=SoloQuiz(owner,{owner}) { _,_ -> owner="two";json("id" to "run","questions" to listOf(question)) }
  assertTrue(runCatching { solo.start() }.isFailure);assertTrue(solo.state.value.isEmpty())
 }
 @Test fun failedAnswerCanBeRetriedWithoutInventedCorrection()=runBlocking {
  var fail=true;val solo=SoloQuiz("one",{"one"}) { name,_ -> if(name=="quiz_start_solo") json("id" to "run","questions" to listOf(question)) else { if(fail) error("offline");json("selectedAnswerId" to "A","question" to question,"isCorrect" to false) } }
  solo.start();assertTrue(runCatching { solo.answer("A") }.isFailure);assertTrue(solo.state.value.obj("answers").isEmpty());fail=false;solo.answer("A");assertEquals("A",solo.state.value.obj("answers").obj("q").str("selectedAnswerId"))
 }
}
