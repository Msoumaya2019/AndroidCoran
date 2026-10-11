package com.msoumaya.androidcoran.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private fun Context.readerActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.readerActivity()
    else -> null
}

/** Shared by free reading, learning and revision; restores the window on exit. */
@Composable
fun ReaderWindowFullscreen(enabled: Boolean) {
    val activity=LocalContext.current.readerActivity()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(enabled,activity,lifecycle) {
        if(!enabled||activity==null) return@DisposableEffect onDispose {}
        val decor=activity.window.decorView
        val controller=WindowCompat.getInsetsController(activity.window,decor)
        val previousBehavior=controller.systemBarsBehavior
        fun hide() {
            controller.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        hide()
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) hide() }
        val focus=android.view.ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if(focused&&lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) hide()
        }
        lifecycle.addObserver(observer)
        decor.viewTreeObserver.addOnWindowFocusChangeListener(focus)
        onDispose {
            lifecycle.removeObserver(observer)
            if(decor.viewTreeObserver.isAlive) decor.viewTreeObserver.removeOnWindowFocusChangeListener(focus)
            controller.systemBarsBehavior=previousBehavior
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
