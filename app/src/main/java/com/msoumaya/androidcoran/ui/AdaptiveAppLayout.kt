package com.msoumaya.androidcoran.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun AdaptiveAppContent(wide: Boolean,fullscreen: Boolean,showNavigation: Boolean,tab: String,onTab: (String)->Unit,content: @Composable ()->Unit) {
    Row(Modifier.fillMaxSize()) {
        if(wide&&!fullscreen&&showNavigation) NavigationRail(Modifier.testTag("tablet-navigation").fillMaxHeight()) {
            listOf("Accueil" to Icons.Default.Home,"Coran" to Icons.Default.MenuBook,"Programme" to Icons.Default.DateRange,"Progrès" to Icons.Default.Insights,"Amis" to Icons.Default.People).forEach { (label,icon) ->
                NavigationRailItem(selected=tab==label,onClick={onTab(label)},icon={Icon(icon,label)},label={Text(label)})
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight(),contentAlignment=Alignment.TopCenter) {
            Box(Modifier.then(if(fullscreen) Modifier.fillMaxSize() else Modifier.widthIn(max=1040.dp).fillMaxSize()).testTag("app-content")) { content() }
        }
    }
}
