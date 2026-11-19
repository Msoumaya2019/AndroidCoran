package com.msoumaya.androidcoran.ui
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*
fun loadPrintedPage(context: Context,q: Quran,source: String,page: Int): Triple<JsonObject?,Bitmap?,JsonArray?> {
 if(source=="coranTest") return Triple(q.qcfData(context,page),null,null)
 if(source=="coran_1441") return madaniImage(context,page)
 val folder=if(source=="tajweedPages") "mushaf-tajweed" else "mushaf"
 val bitmap=context.assets.open(folder+"/page"+page.toString().padStart(3,'0')+".png").use { BitmapFactory.decodeStream(it) }
 val bounds=context.assets.open(if(folder=="mushaf") "bounds.json" else "mushaf-tajweed-bounds.json").bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject[page.toString()]?.jsonArray }
 return Triple(null,bitmap,bounds)
}
