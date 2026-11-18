package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
class UsageRulesTest {
 @Test fun elapsedTimeIgnoresPartialSecondsAndNeverBecomesNegative() { assertEquals(3L,elapsedUsageSeconds(1000,4999));assertEquals(0L,elapsedUsageSeconds(2000,1000)) }
 @Test fun durationMatchesReferenceLabels() { assertEquals("0 s",formatUsageDuration(-1));assertEquals("59 s",formatUsageDuration(59));assertEquals("1 min 1 s",formatUsageDuration(61));assertEquals("1 h 1 min",formatUsageDuration(3661)) }
 @Test fun moderationDistinguishesPrivateRecipientsAndGroups() { assertEquals("À : Amin",adminMessageDestination(json("recipient_name" to "Amin")));assertEquals("Groupe : Révisions",adminMessageDestination(json("group_id" to "g","group_name" to "Révisions","recipient_name" to "Amin")));assertEquals("À : Destinataire indisponible",adminMessageDestination(json())) }
}
