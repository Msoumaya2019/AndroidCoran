package com.msoumaya.androidcoran.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msoumaya.androidcoran.R
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val cormorant=FontFamily(Font(R.font.cormorant_regular),Font(R.font.cormorant_semibold,FontWeight.SemiBold))
val arabicInterfaceFont=FontFamily(Font(R.font.amiri_regular))
private fun color(value: String)=Color(android.graphics.Color.parseColor(value))
@Composable fun NativeAppTheme(state: JsonObject,content: @Composable ()->Unit) {
    val p=appPalette(state.str("theme","white"),state.str("accent").ifBlank { null })
    val system=state.str("uiFont","elegant")=="system";val title=if(system) FontFamily.Default else cormorant
    val body=if(state.str("uiFont")=="classic") cormorant else FontFamily.Default
    val typography=Typography().let { t -> t.copy(
        headlineLarge=t.headlineLarge.copy(color=color(p.primary),fontFamily=title,fontWeight=FontWeight.SemiBold,fontSize=30.sp,lineHeight=35.sp),
        headlineMedium=t.headlineMedium.copy(color=color(p.primary),fontFamily=title,fontWeight=FontWeight.SemiBold),
        headlineSmall=t.headlineSmall.copy(color=color(p.primary),fontFamily=title,fontWeight=FontWeight.SemiBold),
        titleLarge=t.titleLarge.copy(color=color(p.primary),fontFamily=title,fontWeight=FontWeight.SemiBold),
        titleMedium=t.titleMedium.copy(color=color(p.primary),fontFamily=title,fontSize=17.sp,fontWeight=FontWeight.SemiBold),
        titleSmall=t.titleSmall.copy(color=color(p.primary),fontFamily=title),
        bodyLarge=t.bodyLarge.copy(fontFamily=body,fontSize=14.sp),bodyMedium=t.bodyMedium.copy(fontFamily=body,fontSize=14.sp),bodySmall=t.bodySmall.copy(fontFamily=body,fontSize=12.sp),
        labelLarge=t.labelLarge.copy(fontFamily=body),labelMedium=t.labelMedium.copy(fontFamily=body),labelSmall=t.labelSmall.copy(fontFamily=body)
    ) }
    MaterialTheme(colorScheme=lightColorScheme(primary=color(p.primary),onPrimary=Color.White,primaryContainer=color(p.selected),onPrimaryContainer=color(p.primary),secondary=color(p.gold),secondaryContainer=color(p.selected),onSecondaryContainer=color(p.primary),background=color(p.background),onBackground=color(p.text),surface=color(p.surface),onSurface=color(p.text),surfaceVariant=color(p.soft),onSurfaceVariant=color(p.muted),outline=color(p.line),outlineVariant=color(p.line),surfaceContainer=color(p.surface),surfaceContainerLow=color(p.surface),surfaceContainerHigh=color(p.soft)),typography=typography,shapes=Shapes(small=RoundedCornerShape(14.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(28.dp)),content=content)
}
@Composable fun AssetImage(path: String,description: String?,modifier: Modifier=Modifier,alpha: Float=1f,scale: ContentScale=ContentScale.Crop) {
    val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,path) { value=withContext(Dispatchers.IO) { val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true };context.assets.open(path).use { BitmapFactory.decodeStream(it,null,bounds) };val options=BitmapFactory.Options().apply { inSampleSize=1 };while(maxOf(bounds.outWidth,bounds.outHeight)/options.inSampleSize>1024) options.inSampleSize*=2;context.assets.open(path).use { BitmapFactory.decodeStream(it,null,options) } } }
    bitmap?.let { Image(it.asImageBitmap(),description,modifier,contentScale=scale,alpha=alpha) }
}
@Composable fun HomeGreeting(state: JsonObject) {
    Box(Modifier.fillMaxWidth().heightIn(min=115.dp)) {
        AssetImage("themes/${themeArtwork(state.str("theme"))}.png",null,Modifier.matchParentSize(),alpha=0.55f)
        Column(Modifier.padding(horizontal=24.dp,vertical=14.dp)) {
            Text("As-Salâm ‘Alaykoum,",fontSize=16.sp)
            Text(state.obj("profile").str("firstName","Bienvenue"),style=MaterialTheme.typography.headlineLarge)
            Text("Prêt à continuer ton apprentissage ?",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun AppearanceOptions(vm: CoranViewModel,state: JsonObject) {
    Text("Couleur d’accent",style=MaterialTheme.typography.titleMedium)
    accentOptions.forEach { (key,label)-> FilterChip(selected=state.str("accent",if(state.str("theme")=="classic") "green" else if(state.str("theme")=="feminine") "rose" else "prune")==key,onClick={vm.action { vm.repo.mutate { touch(it.with("accent" to JsonPrimitive(key))) } }},label={Text(label)}) }
    Text("Police de l’interface",style=MaterialTheme.typography.titleMedium)
    listOf("elegant" to "Élégante · titres Cormorant, texte système","system" to "Moderne · police système","classic" to "Classique · titres et textes Cormorant").forEach { (key,label)-> FilterChip(selected=state.str("uiFont","elegant")==key,onClick={vm.action { vm.repo.mutate { touch(it.with("uiFont" to JsonPrimitive(key))) } }},label={Text(label)}) }
    Text("Le texte et les pages du Mushaf conservent leur police d’origine.")
    Text("Papier du Coran",style=MaterialTheme.typography.titleMedium)
    paperOptions.forEach { (key,value) -> FilterChip(selected=state.obj("reader").str("paper","ivory")==key,onClick={vm.action { vm.repo.mutate { touch(it.with("reader" to it.obj("reader").with("paper" to JsonPrimitive(key)))) } }},label={Text(value.first)}) }
}

@Composable fun ContinueReadingCard(vm: CoranViewModel,state: JsonObject,read: ()->Unit) {
    val id=homeReadingVerse(state);val verse=vm.repo.quran.verse(id);val surah=vm.repo.quran.surah(id)
    Panel("Continuer ma lecture",onClick=read) {
        Row(horizontalArrangement=Arrangement.spacedBy(13.dp)) {
            AssetImage("illustrations/reading.png","Lecture du Coran",Modifier.weight(0.46f).height(132.dp))
            Column(Modifier.weight(0.54f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text("Sourate",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(surah.name,style=MaterialTheme.typography.titleLarge)
                Text(surah.arabic,fontFamily=arabicInterfaceFont,fontSize=22.sp,color=MaterialTheme.colorScheme.secondary)
                Text("Verset ${verse.ayah} · Page ${vm.repo.quran.sourcePage(id,state.obj("reader").str("mushaf","coranTest"))}",fontSize=11.sp)
                LinearProgressIndicator(drawStopIndicator={},progress={(verse.ayah-1).toFloat()/surah.range.ids.size})
                Button(onClick=read) { Text("Lire le Coran") }
            }
        }
    }
}
