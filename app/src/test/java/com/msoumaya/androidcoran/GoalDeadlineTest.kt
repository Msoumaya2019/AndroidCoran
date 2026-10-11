package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import kotlinx.serialization.json.JsonObject
import org.junit.Test
class GoalDeadlineTest {
 @Test fun dateIsOptionalAndLeapYearsAreStrict() { assertTrue(validGoalDeadline(""));assertTrue(validGoalDeadline("2028-02-29"));assertFalse(validGoalDeadline("2026-02-29"));assertFalse(validGoalDeadline("2026-04-31"));assertFalse(validGoalDeadline("2026-2-03")) }
 @Test fun deadlineChangePreservesOtherSourceFields() { val original=json("label" to "Objectif","ranges" to listOf(json("start" to 1,"end" to 7)),"direction" to "fromNas","extension" to json("value" to 9));val result=withGoalDeadline(original,"2027-01-01");assertEquals(original,JsonObject(result.filterKeys { it!="deadline" }));assertEquals("2027-01-01",result.str("deadline")) }
 @Test fun clearingOnlyRemovesDeadline() { val original=json("label" to "Objectif","extra" to true);assertEquals(original,withGoalDeadline(withGoalDeadline(original,"2027-01-01"),"")) }
 @Test(expected=IllegalArgumentException::class) fun invalidDateCannotBeSaved() { withGoalDeadline(json(),"2026-13-01") }
}
