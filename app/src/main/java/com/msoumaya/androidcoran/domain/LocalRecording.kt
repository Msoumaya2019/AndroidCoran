package com.msoumaya.androidcoran.domain
import java.io.File
import java.net.URI
fun localRecordingFile(root: File,uri: String): File? = try {
    val parsed=URI(uri)
    if(parsed.scheme!="file") null else File(parsed).canonicalFile.takeIf { it.isFile&&it.toPath().startsWith(root.canonicalFile.toPath()) }
} catch(e: Exception) { null }
