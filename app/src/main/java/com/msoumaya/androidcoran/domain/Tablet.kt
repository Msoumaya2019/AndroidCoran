package com.msoumaya.androidcoran.domain
import kotlinx.serialization.json.*

data class TabletPreferences(val mode: String="auto",val toolsVisible: Boolean=true,val stylusOnly: Boolean=true,val color: String="#16A34A",val penWidth: Float=.003f,val highlightWidth: Float=.018f,val penStyle: String="ink",val spread: String="single") {
 fun toJson()=json("mode" to mode,"toolsVisible" to toolsVisible,"stylusOnly" to stylusOnly,"color" to color,"penWidth" to penWidth,"highlightWidth" to highlightWidth,"penStyle" to penStyle,"spread" to spread)
 companion object { fun from(s: JsonObject): TabletPreferences {
  fun width(key: String,default: Float,min: Float,max: Float)=(s[key] as? JsonPrimitive)?.floatOrNull?.takeIf { it.isFinite() }?.coerceIn(min,max)?:default
  return TabletPreferences(s.str("mode").takeIf { it in listOf("auto","on","off") }?:"auto",s.flag("toolsVisible",true),s.flag("stylusOnly",true),s.str("color").takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }?:"#16A34A",width("penWidth",.003f,.001f,.012f),width("highlightWidth",.018f,.006f,.06f),if(s.str("penStyle")=="pencil") "pencil" else "ink",if(s.str("spread")=="double") "double" else "single")
 } }
}
data class TabletLayout(val enabled: Boolean,val landscape: Boolean,val sidebars: Boolean,val doublePage: Boolean,val centerWidth: Float)
fun tabletLayout(p: TabletPreferences,device: String,width: Float,height: Float,notesOpen: Boolean=false): TabletLayout {
 val enabled=p.mode=="on"||p.mode!="off"&&device!="phone"&&width>=(if(device=="tablet") 600 else 768)&&height>=360
 val landscape=width>height;val sidebars=enabled&&landscape&&width>=1000&&height>=480
 val center=(width-(if(sidebars&&p.toolsVisible) 72 else 0)-(if(sidebars&&notesOpen) 280 else 0)).coerceAtLeast(0f)
 return TabletLayout(enabled,landscape,sidebars,enabled&&landscape&&p.spread=="double"&&center>=640&&height>=460,center)
}
fun spreadPages(page: Int,double: Boolean): List<Int> { val p=page.coerceIn(1,604);if(!double) return listOf(p);val right=if(p%2==1) p else p-1;return listOf(right+1,right) }
fun nextSpread(page: Int,direction: Int,double: Boolean): Int = if(!double) (page+direction).coerceIn(1,604) else ((if(page%2==1) page else page-1)+direction*2).coerceIn(1,603)
val tabletColors=listOf("Jaune" to "#EAB308","Bleu" to "#2563EB","Vert" to "#16A34A","Rouge" to "#DC2626","Orange" to "#EA580C","Rose" to "#DB2777")
fun exportPageNumbers(input: String,max: Int=20): List<Int> {
 val pages=sortedSetOf<Int>()
 input.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
  val m=Regex("([0-9]{1,3})(?:\\s*-\\s*([0-9]{1,3}))?").matchEntire(part)?:error("Utilise des numéros ou des plages : 3, 5-8")
  val start=m.groupValues[1].toInt();val end=m.groupValues[2].toIntOrNull()?:start
  require(start>=1&&end<=604&&end>=start) { "Les pages doivent être comprises entre 1 et 604" }
  for(p in start..end) { pages+=p;require(pages.size<=max) { "Choisis au maximum 20 pages" } }
 }
 require(pages.isNotEmpty()) { "Choisis au moins une page" };return pages.toList()
}
