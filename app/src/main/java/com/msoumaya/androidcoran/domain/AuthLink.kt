package com.msoumaya.androidcoran.domain

import java.net.URI
import java.net.URLDecoder
const val MOBILE_AUTH_REDIRECT="coranmemoire://auth"
class MobileAuthLink(val accessToken: String,val refreshToken: String,val recovery: Boolean,val expiresIn: Long) {
    override fun toString()="MobileAuthLink(recovery=$recovery)"
}
fun mobileAuthLink(raw: String): MobileAuthLink? {
    val uri=try { URI(raw) } catch(_: Exception) { throw IllegalArgumentException("Lien de connexion invalide") }
    if(uri.scheme!="coranmemoire"||uri.host!="auth"||uri.userInfo!=null||uri.port!=-1||uri.path !in listOf("", "/")) return null
    val pairs=(uri.rawFragment?:"").split('&').filter { it.isNotBlank() }.map {
        val parts=it.split('=',limit=2)
        URLDecoder.decode(parts[0],"UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" },"UTF-8")
    }
    require(pairs.map { it.first }.distinct().size==pairs.size) { "Lien de connexion ambigu" }
    val values=pairs.toMap()
    require(values["error"].isNullOrBlank()) { "Lien expiré ou invalide. Demande un nouveau courriel." }
    val access=values["access_token"]?.takeIf { it.isNotBlank() }?:error("Lien de connexion incomplet")
    val refresh=values["refresh_token"]?.takeIf { it.isNotBlank() }?:error("Lien de connexion incomplet")
    val expiry=values["expires_in"]?.toLongOrNull()?:0L
    require(expiry in 0..604800) { "Durée de session invalide" }
    return MobileAuthLink(access,refresh,values["type"]=="recovery",expiry)
}
