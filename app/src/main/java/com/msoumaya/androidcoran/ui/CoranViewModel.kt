package com.msoumaya.androidcoran.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msoumaya.androidcoran.CoranApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class CoranViewModel(app: Application): AndroidViewModel(app) {
    val repo=(app as CoranApplication).repository
    fun action(block: suspend ()->Unit) { viewModelScope.launch(Dispatchers.IO) { try { block() } catch(e: Exception) { repo.feedback(e.message?:"Opération impossible") } } }
}
