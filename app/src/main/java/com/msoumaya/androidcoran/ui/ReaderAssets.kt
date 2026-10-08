package com.msoumaya.androidcoran.ui

import android.content.Context
import android.graphics.*
import com.msoumaya.androidcoran.data.QuranDownloadWorker
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
import java.io.File

fun madaniImage(context: Context,page: Int): Triple<JsonObject?,Bitmap?,JsonArray?> {
    if(!File(QuranDownloadWorker.root(context),"ready-v1").exists()) return Triple(null,null,null)
    val bitmap=Bitmap.createBitmap(1440,2320,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.drawColor(Color.WHITE);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    for(line in 0..14) { val image=BitmapFactory.decodeFile(File(QuranDownloadWorker.root(context),"%03d-%02d.png".format(page,line+1)).path)?:error("Ligne de Mushaf manquante");val y=(2320f-232f)/14*line;canvas.drawBitmap(image,0f,y,paint);image.recycle() }
    val markers=context.assets.open("quran-tests/coran_1441-markers.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject[page.toString()]?.jsonArray }
    markers?.forEach { raw -> val a=raw.jsonArray;val line=a[2].jsonPrimitive.int;val x=a[3].jsonPrimitive.float*1440;val y=(2320f-232f)/14*line+a[4].jsonPrimitive.float*232;paint.color=Color.rgb(236,253,245);canvas.drawCircle(x,y,36f,paint);paint.color=Color.rgb(4,120,87);paint.style=Paint.Style.STROKE;paint.strokeWidth=4f;canvas.drawCircle(x,y,36f,paint);paint.style=Paint.Style.FILL;paint.textSize=36f;val ayah=a[1].jsonPrimitive.content.map { if(it.isDigit()) "٠١٢٣٤٥٦٧٨٩"[it.digitToInt()] else it }.joinToString("");canvas.drawText(ayah,x-paint.measureText(ayah)/2,y-(paint.fontMetrics.ascent+paint.fontMetrics.descent)/2,paint) }
    val bounds=context.assets.open("quran-tests/coran_1441-bounds.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject[page.toString()]?.jsonArray }
    return Triple(null,bitmap,bounds)
}
