package com.msoumaya.androidcoran.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
@Composable fun RecitationDeleteDialog(description: String,busy: Boolean,message: String,onDelete: ()->Unit,onDismiss: ()->Unit) {
 AlertDialog(onDismissRequest={if(!busy) onDismiss()},title={Text("Supprimer cette récitation ?")},text={Text(description+"\nLe fichier, ses corrections et les accès partagés seront supprimés. Cette action est définitive."+if(message.isBlank()) "" else "\n"+message)},confirmButton={TextButton(enabled=!busy,onClick=onDelete) { Text(if(busy) "Suppression…" else "Supprimer") }},dismissButton={TextButton(enabled=!busy,onClick=onDismiss) { Text("Annuler") }})
}
