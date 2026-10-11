package com.msoumaya.androidcoran.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

@Composable fun ConversationTypingIndicator(active: Boolean) {
    if(active) Text("Écrit un message…",Modifier.padding(vertical=4.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
}
