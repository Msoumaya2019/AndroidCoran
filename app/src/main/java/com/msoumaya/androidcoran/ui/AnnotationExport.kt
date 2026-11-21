package com.msoumaya.androidcoran.ui
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.msoumaya.androidcoran.domain.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.File
import java.util.UUID
suspend fun renderAnnotationPage(context: Context,q: Quran,s: JsonObject,source: String,page: Int): Bitmap {
 val payload=withContext(Dispatchers.IO) { loadPrintedPage(context,q,source,page) }
 try {
  require(payload.first!=null||payload.second!=null) { "Télécharge d’abord cette édition" }
  return withContext(Dispatchers.Main) {
   val w=payload.second?.width?:1000;val h=payload.second?.height?:2120
   val view=MushafView(context);view.load(q,page,source,payload.first,payload.second,payload.third);view.paper=paperColor(s.obj("reader").str("paper"));view.annotations=allReaderInk(s,source);val prefs=TabletPreferences.from(s.obj("reader").obj("tablet"));val density=context.resources.displayMetrics.density;val shortSide=if(android.os.Build.VERSION.SDK_INT>=30) context.getSystemService(android.view.WindowManager::class.java).maximumWindowMetrics.bounds.let { minOf(it.width(),it.height())/density } else context.resources.configuration.smallestScreenWidthDp.toFloat();view.marginEnabled=prefs.mode=="on"||(prefs.mode=="auto"&&shortSide>=600)||hasMarginInk(view.annotations.filter { it.page==page });val workspace=inkWorkspace(view.annotations.filter { it.page==page },view.marginEnabled);view.workspaceBounds=RectF(workspace.left,workspace.top,workspace.right,workspace.bottom);val size=(workspace.right-workspace.left)*w to (workspace.bottom-workspace.top)*h;val exportScale=minOf(1f,4096f/maxOf(size.first,size.second),kotlin.math.sqrt(8_000_000f/(size.first*size.second)));val outW=kotlin.math.ceil(size.first*exportScale).toInt().coerceAtLeast(1);val outH=kotlin.math.ceil(size.second*exportScale).toInt().coerceAtLeast(1);view.layout(0,0,outW,outH)
   Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
  }
 } finally { payload.second?.recycle() }
}
fun shareAnnotationFile(context: Context,file: File,mime: String) {
 val uri=FileProvider.getUriForFile(context,context.packageName+".annotation-exports",file)
 val intent=Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
 intent.clipData=android.content.ClipData.newRawUri("Annotation",uri)
 context.startActivity(Intent.createChooser(intent,"Exporter les annotations"))
}
fun annotationExportFile(context: Context,extension: String): File { require(extension in listOf("png","pdf","txt"));return File(File(context.cacheDir,"annotation-exports").apply { mkdirs() },"annotations-"+UUID.randomUUID()+"."+extension) }
suspend fun exportAnnotationPages(context: Context,q: Quran,s: JsonObject,source: String,pages: List<Int>,pdf: Boolean): File {
 require(pages.isNotEmpty()&&pages.size<=20&&pages.all { it in 1..604 });require(source!="tajweed")
 val file=annotationExportFile(context,if(pdf) "pdf" else "png")
 try {
  if(!pdf) { require(pages.size==1);val bitmap=renderAnnotationPage(context,q,s,source,pages.single());try { withContext(Dispatchers.IO) { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) } } } finally { bitmap.recycle() } }
  else { val doc=PdfDocument();try {
   pages.forEachIndexed { index,page -> currentCoroutineContext().ensureActive();val bitmap=renderAnnotationPage(context,q,s,source,page)
    try { val sheet=doc.startPage(PdfDocument.PageInfo.Builder(595,842,index+1).create());val canvas=sheet.canvas;canvas.drawColor(Color.WHITE);val scale=minOf(595f/bitmap.width,842f/bitmap.height);val w=bitmap.width*scale;val h=bitmap.height*scale;canvas.drawBitmap(bitmap,null,RectF((595-w)/2,(842-h)/2,(595+w)/2,(842+h)/2),Paint(Paint.FILTER_BITMAP_FLAG));doc.finishPage(sheet) } finally { bitmap.recycle() }
   }
   withContext(Dispatchers.IO) { file.outputStream().use { doc.writeTo(it) } }
  } finally { doc.close() } }
  return file
 } catch(e: Throwable) { file.delete();throw e }
}
@Composable fun AnnotationExportDialog(vm: CoranViewModel,s: JsonObject,source: String,page: Int,onActive: (Boolean)->Unit,onClose: ()->Unit) {
 val context=androidx.compose.ui.platform.LocalContext.current
 val expectedOwner=vm.repo.user.value
 var input by remember { mutableStateOf(page.toString()) };var busy by remember { mutableStateOf(false) };var error by remember { mutableStateOf("") }
 fun export(pdf: Boolean) { if(busy) return;val pages=try { if(pdf) exportPageNumbers(input) else listOf(page) } catch(e: Exception) { error=e.message.orEmpty();return };busy=true;onActive(true);vm.action { try { val file=exportAnnotationPages(context,vm.repo.quran,s,source,pages,pdf);withContext(Dispatchers.Main) { if(vm.repo.user.value==expectedOwner) shareAnnotationFile(context,file,if(pdf) "application/pdf" else "image/png") else file.delete();onClose() } } finally { busy=false;onActive(false) } } }
 AlertDialog(onDismissRequest={if(!busy) onClose()},title={Text("Exporter les annotations")},text={Column { Text("Le rendu original et les tracés visibles sont conservés. Les commentaires ne sont pas ajoutés en annexe.");OutlinedTextField(input,{input=it},label={Text("Pages PDF · 1 à 20 pages")});if(error.isNotBlank()) Text(error);if(busy) LinearProgressIndicator() }},confirmButton={TextButton(enabled=!busy,onClick={export(true)}) { Text("PDF") }},dismissButton={Row { TextButton(enabled=!busy,onClick={export(false)}) { Text("PNG · page actuelle") };TextButton(enabled=!busy,onClick=onClose) { Text("Fermer") } }})
}
