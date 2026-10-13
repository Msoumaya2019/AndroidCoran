package com.msoumaya.androidcoran.domain

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

/** Installs original line images; readiness is published only after every line succeeds. */
class MushafArchive(private val directory: File) {
    fun install(archive: File,validate: (File)->Boolean,checkActive: ()->Unit={},progress: (Int)->Unit={}) {
        val installed=mutableSetOf<String>();var expanded=0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while(true) {
                checkActive()
                val entry=zip.nextEntry?:break
                val match=Regex("^width_1440/([0-9]+)/([0-9]+)\\.png$").matchEntire(entry.name)
                if(!entry.isDirectory&&match!=null) {
                    val page=match.groupValues[1].toInt();val line=match.groupValues[2].toInt()
                    require(page in 1..604&&line in 1..15)
                    val name="%03d-%02d.png".format(java.util.Locale.ROOT,page,line)
                    require(installed.add(name)) { "Ligne dupliquée" }
                    val target=File(directory,name);val temp=File(directory,name+".tmp")
                    try {
                        var bytes=0L
                        temp.outputStream().use { output ->
                            val buffer=ByteArray(65536)
                            while(true) {
                                checkActive();val n=zip.read(buffer);if(n<0) break
                                bytes+=n;expanded+=n
                                require(bytes<=2*1024*1024&&expanded<=1_000_000_000) { "Image trop volumineuse" }
                                output.write(buffer,0,n)
                            }
                        }
                        require(validate(temp)) { "Image du Mushaf invalide" }
                        Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING)
                        progress(installed.size)
                    } finally { temp.delete() }
                }
                zip.closeEntry()
            }
        }
        require(installed.size==9060) { "Certaines pages sont manquantes" }
        checkActive()
        val marker=File(directory,"ready-v1.tmp")
        try { marker.writeText("9060");Files.move(marker.toPath(),File(directory,"ready-v1").toPath(),StandardCopyOption.REPLACE_EXISTING) }
        finally { marker.delete() }
    }
}
