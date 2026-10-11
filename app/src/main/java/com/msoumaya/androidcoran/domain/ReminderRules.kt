package com.msoumaya.androidcoran.domain

import java.time.ZonedDateTime

data class ReminderNavigation(val account: String,val id: String=java.util.UUID.randomUUID().toString())

fun nextLearningReminder(now: ZonedDateTime): ZonedDateTime {
    val today=now.toLocalDate().atTime(19,0).atZone(now.zone)
    return if(today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(19,0).atZone(now.zone)
}
fun canOpenLearningReminder(account: String,currentUser: String?): Boolean =
    account.isNotBlank()&&account==(currentUser?:"guest")
