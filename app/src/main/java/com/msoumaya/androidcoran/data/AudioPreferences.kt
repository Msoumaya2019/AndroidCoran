package com.msoumaya.androidcoran.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
private val Context.audioRepeat by preferencesDataStore("audio_repeat_preferences")
class AudioPreferences(private val context: Context) {
    suspend fun load(): RepeatPreferences=try { RepeatPreferences.from(Json.parseToJsonElement(context.applicationContext.audioRepeat.data.first()[stringPreferencesKey("settings")]?:"{}").jsonObject) } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e;RepeatPreferences() }
    suspend fun save(preferences: RepeatPreferences) { context.applicationContext.audioRepeat.edit { it[stringPreferencesKey("settings")]=preferences.json().toString() } }
}
