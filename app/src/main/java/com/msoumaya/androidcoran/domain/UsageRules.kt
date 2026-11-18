package com.msoumaya.androidcoran.domain
fun elapsedUsageSeconds(start: Long,now: Long)=((now-start)/1000).coerceAtLeast(0)
fun formatUsageDuration(seconds: Int): String {
 val n=seconds.coerceAtLeast(0)
 return if(n<60) n.toString()+" s" else if(n<3600) (n/60).toString()+" min "+(n%60)+" s" else (n/3600).toString()+" h "+(n/60%60)+" min"
}
fun adminMessageDestination(message: kotlinx.serialization.json.JsonObject): String = if(message.str("group_id").isNotBlank()) "Groupe : "+message.str("group_name").ifBlank { "Groupe indisponible" } else "À : "+message.str("recipient_name").ifBlank { message.str("recipient_id").ifBlank { "Destinataire indisponible" } }
