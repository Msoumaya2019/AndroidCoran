package com.msoumaya.androidcoran.data

import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

class AdminService(private val repo: Repository) {
    suspend fun authorized(): Boolean {
        val owner=repo.user.value?:return false
        val rows=repo.query("app_admins",mapOf("user_id" to owner),orderBy="user_id",size=1)
        return owner==repo.user.value&&rows.any { it.str("user_id")==owner }
    }
    private suspend fun requireAdmin() { check(authorized()) { "Accès administrateur requis" } }
    suspend fun accounts(offset: Int,search: String): List<JsonObject> {
        require(offset>=0);requireAdmin()
        val rows=repo.rpc("admin_learning_accounts",json("p_offset" to offset,"p_search" to search.trim())).jsonArray.map { it.jsonObject }
        if(rows.isEmpty()) return rows
        return try {
            val usage=repo.rpc("admin_last_app_usage",json("p_users" to rows.map { it.str("user_id") })).jsonArray.map { it.jsonObject }
            rows.map { row -> row.with("last_usage" to (usage.firstOrNull { it.str("user_id")==row.str("user_id") }?:JsonNull)) }
        } catch(e: kotlinx.coroutines.CancellationException) { throw e
        } catch(e: Exception) {
            val missing=listOf("PGRST202","42883","42P01").any { e.message.orEmpty().contains(it) }
            rows.map { it.with("usage_error" to JsonPrimitive(if(missing) "Suivi des sessions non installé dans Supabase : migration app-usage.sql requise." else "Impossible de charger les durées des sessions. Vérifie la connexion et les droits administrateur.")) }
        }
    }
    suspend fun messages(): List<JsonObject> {
        requireAdmin()
        return try { repo.rpc("admin_messages_with_recipients").jsonArray.map { it.jsonObject } }
        catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(e: Exception) {
            if(listOf("PGRST202","42883").none { e.message.orEmpty().contains(it) }) throw e
            repo.query("friend_messages",size=100)
        }
    }
    suspend fun notificationHistory(): List<JsonObject> {
        requireAdmin();return repo.query("admin_notifications",size=20)
    }
}
