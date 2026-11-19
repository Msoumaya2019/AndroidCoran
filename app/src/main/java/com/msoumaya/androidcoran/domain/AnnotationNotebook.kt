package com.msoumaya.androidcoran.domain
import java.time.Instant

data class NotebookFilter(val surah: Int?=null,val page: Int?=null,val color: String?=null,val tool: String?=null,val favorite: Boolean=false,val difficult: Boolean=false,val sort: String="date")
fun notebookEntries(items: List<ReaderInk>,filter: NotebookFilter,difficult: Set<Int>,surahOf: (Int)->Int): List<ReaderInk> {
 fun verse(n: ReaderInk)=n.anchor?.num("verseId")?.takeIf { it in 1..6236 }?:n.verses.firstOrNull()?:0
 fun key(n: ReaderInk)=when(filter.sort) { "surah"->surahOf(verse(n)).toString().padStart(3,'0');"page"->n.source+":"+n.page.toString().padStart(3,'0');"color"->n.color;"type"->n.tool;"difficulty"->if(n.masteredAt==null&&verse(n) in difficult) "0" else "1";else->"" }
 return items.filter { it.deletedAt==null&&(filter.surah==null||surahOf(verse(it))==filter.surah)&&(filter.page==null||it.page==filter.page)&&(filter.color==null||it.color==filter.color)&&(filter.tool==null||it.tool==filter.tool)&&(!filter.favorite||it.favorite)&&(!filter.difficult||it.masteredAt==null&&verse(it) in difficult) }.sortedWith(compareBy<ReaderInk> { key(it) }.thenByDescending { it.createdAt?:it.updatedAt }.thenBy { it.id })
}
fun noteTextExport(items: List<ReaderInk>,label: (Int)->String)=items.joinToString("\n\n---\n\n") { n ->
 val verse=n.anchor?.num("verseId")?.takeIf { it in 1..6236 }?:n.verses.firstOrNull()
 n.source+" · page "+n.page+(verse?.let { " · "+label(it) }?:"")+(n.anchor?.str("wordText")?.takeIf { it.isNotBlank() }?.let { " · "+it }?:"")+"\n"+(n.createdAt?:n.updatedAt)+"\n"+n.text.ifBlank { "(Sans commentaire textuel)" }+(if(n.note.isNotEmpty()) "\n[Note manuscrite jointe dans l’application]" else "")
}
