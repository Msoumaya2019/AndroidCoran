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
        return repo.rpc("admin_learning_accounts",json("p_offset" to offset,"p_search" to search.trim())).jsonArray.map { it.jsonObject }
    }
    suspend fun notificationHistory(): List<JsonObject> {
        requireAdmin();return repo.query("admin_notifications",size=20)
    }
}
