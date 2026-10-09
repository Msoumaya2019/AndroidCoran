package com.msoumaya.androidcoran.ui
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msoumaya.androidcoran.data.RecitationFriend
@Composable fun RecitationSharingDialog(description: String,friends: List<RecitationFriend>,loading: Boolean,busy: Boolean,message: String,onShare: (String)->Unit,onDismiss: ()->Unit,avatar: @Composable (RecitationFriend)->Unit={}) {
 var chosen by rememberSaveable { mutableStateOf<String?>(null) }
 val friend=friends.firstOrNull { it.id==chosen }
 AlertDialog(onDismissRequest={if(!busy) onDismiss()},title={Text(if(friend==null) "Partager avec un ami" else "Partager cette récitation ?")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
  if(friend!=null) Text("Seul "+friend.name+" pourra écouter "+description+" tant que vous restez amis.")
  else { Text("Choisis un ami. L’envoi sera confirmé avant le partage.");if(loading) CircularProgressIndicator() else if(friends.isEmpty()) Text("Aucun ami accepté pour le moment.");friends.forEach { row -> TextButton(enabled=!busy,onClick={chosen=row.id}) { androidx.compose.foundation.layout.Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) { avatar(row);Text(row.name) } } } }
  if(message.isNotBlank()) Text(message)
 }},confirmButton={if(friend!=null) TextButton(enabled=!busy&&!loading,onClick={onShare(friend.id)}) { Text(if(busy) "Envoi…" else "Partager") }},dismissButton={TextButton(enabled=!busy,onClick={if(friend!=null) chosen=null else onDismiss()}) { Text("Annuler") }})
}
