package com.msoumaya.androidcoran.domain
import kotlinx.serialization.json.*
import java.io.File
data class RecordingRemoval(val id: String,val storagePath: String?)
fun recordingRemoval(owner: String,row: JsonObject): RecordingRemoval {
 require(owner.isNotBlank()&&row.str("user_id")==owner) { "Cette récitation ne t’appartient pas." }
 val id=row.str("id");require(id.isNotBlank()&&!id.contains('/')&&!id.contains('\\')&&id!="."&&id!="..") { "Identifiant de récitation invalide." }
 val path=row.str("storage_path").takeIf { it.isNotBlank() }
 if(path!=null) require(path.startsWith("$owner/")&&!path.contains('\\')&&path.split('/').none { it.isBlank()||it=="."||it==".." }) { "Le fichier distant appartient à un autre emplacement." }
 return RecordingRemoval(id,path)
}
fun removableRecordingFile(root: File,row: JsonObject): File? {
 val path=row.str("local_path");if(path.isBlank()) return null
 val file=File(path);if(!file.exists()) return null
 val owned=localRecordingFile(root,file.toURI().toString())?:error("Le fichier local est hors de ce compte.")
 check(owned.nameWithoutExtension==row.str("id")) { "Le fichier local ne correspond pas à cette récitation." };return owned
}
