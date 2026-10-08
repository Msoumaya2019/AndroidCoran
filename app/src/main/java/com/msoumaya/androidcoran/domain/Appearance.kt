package com.msoumaya.androidcoran.domain
import kotlinx.serialization.json.*

data class AppPalette(val primary: String,val background: String,val surface: String,val text: String,val muted: String,val line: String,val gold: String,val selected: String,val soft: String)
val themePalettes=mapOf(
    "white" to AppPalette("#7B285C","#FCFBF9","#FFFFFF","#241C2B","#746D7B","#ECE8E5","#C89A52","#F5EDF2","#F8F6F4"),
    "classic" to AppPalette("#153F36","#F7F5EE","#FFFDF7","#20342E","#6C7B72","#E4E7DF","#B39559","#EAF2EC","#ECF1EA"),
    "feminine" to AppPalette("#9D496B","#FFFAFC","#FFF5F8","#3C2833","#765B69","#EBCAD8","#B39559","#F8E7EE","#F3DCE5"),
    "lilac" to AppPalette("#5F548E","#FBF9FF","#FFFCFF","#2D2943","#716B86","#E3DCF0","#B89D65","#ECE6F6","#F0EBF8"),
    "night" to AppPalette("#132B47","#F7F7F4","#FFFDF8","#1C2A3B","#68727D","#DFE3E5","#B58942","#E8EFF4","#E9EEF1")
)
val accentOptions=linkedMapOf("prune" to "Prune","rose" to "Rose","green" to "Vert","gold" to "Doré")
private val accentColors=mapOf("prune" to ("#7B285C" to "#F5EDF2"),"rose" to ("#A95069" to "#FCF0F3"),"green" to ("#54734E" to "#EDF5EA"),"gold" to ("#916825" to "#FBF4E8"))
fun appPalette(theme: String,accent: String?): AppPalette {
    val key=theme.takeIf { it in themePalettes }?:"white";val base=themePalettes.getValue(key)
    val chosen=accent?.takeIf { it in accentColors }
    val colors=accentColors[chosen?:if(key=="classic") "green" else if(key=="feminine") "rose" else "prune"]!!
    return if(chosen!=null||key=="white") base.copy(primary=colors.first,selected=colors.second) else base
}
val paperOptions=linkedMapOf("ivory" to ("Ivoire" to "#faf7f2"),"rose" to ("Rosé" to "#f5e1e7"),"sand" to ("Sable" to "#e8dcc8"),"sepia" to ("Sépia" to "#d7c5ad"))
fun paperColor(key: String?)=(paperOptions[key]?:paperOptions.getValue("ivory")).second
fun themeArtwork(theme: String)=when(theme) { "classic"->"emerald";"feminine"->"rose";"lilac"->"lilac";"night"->"night";else->"white" }

fun homeReadingVerse(state: kotlinx.serialization.json.JsonObject,at: java.time.LocalDate=java.time.LocalDate.now()): Int {
    val last=state.obj("lastRead").num("verseId")
    val session=state.arr("sessions").map { it.jsonObject }.firstOrNull { it.str("scheduledDate",it.str("date"))==at.toString()&&it.str("status")=="todo" }
    return (if(last in 1..6236) last else session?.num("start")?:state.obj("goal").arr("ranges").firstOrNull()?.jsonObject?.num("start")?:1).coerceIn(1,6236)
}
