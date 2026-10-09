package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
class SessionPostponeTest {
 private val q=Quran { Json.parseToJsonElement(File("src/main/assets/$it").readText()) }
 private val program=Program(q)
 private fun state()=defaultState().with("custom" to json("retained" to true),"sessions" to element(listOf(
  json("id" to "x","start" to 1,"end" to 7,"date" to "2026-10-09","scheduledDate" to "2026-10-08","status" to "todo"),
  json("id" to "y","start" to 8,"end" to 10,"date" to "2026-10-10","status" to "todo"))))
 @Test fun unstartedSessionIsReportedWithoutChangingScheduleOrKnowledge() {
  val original=state();val next=program.postpone(original,"x")
  assertEquals("postponed",next.arr("sessions")[0].jsonObject.str("status"))
  assertEquals(original.arr("sessions")[0].jsonObject.with("status" to JsonPrimitive("postponed")),next.arr("sessions")[0])
  assertEquals(original.arr("sessions")[1],next.arr("sessions")[1])
  listOf("knowledge","memorizedAt","revisions","studyProgress","custom").forEach { assertEquals(original[it],next[it]) }
 }
 @Test fun partiallyLearnedSessionRemainsResumableWithoutLosingValidation() {
  val partial=program.complete(state(),"x",3,LocalDate.of(2026,10,9));val next=program.postpone(partial,"x")
  assertEquals("todo",next.arr("sessions")[0].jsonObject.str("status"))
  listOf("sessions","knowledge","memorizedAt","revisions","studyProgress","custom").forEach { assertEquals(partial[it],next[it]) }
  assertEquals(3,next.obj("studyProgress").obj("learning:x").num("through"));assertFalse(known(next,4))
 }
}
