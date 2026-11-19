package com.msoumaya.androidcoran.ui

import android.content.Context
import android.graphics.*
import android.view.*
import android.util.LruCache
import com.msoumaya.androidcoran.domain.*
import kotlinx.serialization.json.*

/** Native Canvas painter: keeps the reference's words, 122-unit lines and page proportions. */
class MushafView(context: Context): View(context) {
    var annotationOwner="";set(value) { if(field!=value) { inkDraft.clear();inkDrawing=false };field=value }
    var annotationMode=false
    var stylusOnly=false
    var inkTool="pen";var inkColor="#16A34A";var inkWidth=.003f;var inkPenStyle="ink"
    var onSelection: (JsonObject)->Unit={}
    var onInkErase: (ReaderInk)->Unit={}
    var onInkActive: (Boolean)->Unit={}
    private val wordRegions=mutableMapOf<Int,RectF>()
    private val wordDetails=mutableMapOf<Int,JsonObject>()
    private fun paperWidth()=bitmap?.width?.toFloat()?:1000f
    private fun paperHeight()=bitmap?.height?.toFloat()?:2120f
    private fun zones(ink: ReaderInk): List<RectF> {
        val anchor=ink.anchor?:return emptyList()
        if(anchor["wordId"]!=null) return wordRegions[anchor.num("wordId")]?.let { listOf(it) }?:emptyList()
        return regions.filter { it.second==anchor.num("verseId") }.map { it.first }.groupBy { it.top }.values.map { line -> RectF(line.minOf { it.left },line.first().top,line.maxOf { it.right },line.first().bottom) }
    }
    fun selectionAt(x: Float,y: Float): JsonObject? {
        val px=(x-left-offsetX)/(ratio*zoom);val py=(y-top-offsetY)/(ratio*zoom)
        val hit=regions.lastOrNull { it.first.contains(px,py) }?:return null
        fun region(r: RectF)=json("x" to r.left/paperWidth(),"y" to r.top/paperHeight(),"width" to r.width()/paperWidth(),"height" to r.height()/paperHeight())
        val verses=regions.filter { it.second==hit.second }.map { it.first }.groupBy { it.top }.values.map { line -> region(RectF(line.minOf { it.left },line.first().top,line.maxOf { it.right },line.first().bottom)) }
        val word=wordRegions.entries.lastOrNull { it.value.contains(px,py) }
        val details=word?.let { wordDetails[it.key] }?:json()
        return details.with("verseId" to JsonPrimitive(hit.second),"regions" to element(if(word!=null) listOf(region(word.value)) else verses),"verseRegions" to element(verses))
    }
    var annotations: List<ReaderInk> = emptyList()
    var onInkStroke: (InkStroke,List<Int>)->Unit={_,_->}
    var onInkClick: (ReaderInk)->Unit={}
    private val inkDraft=mutableListOf<InkPoint>()
    private var inkDrawing=false
    private fun inkPoint(x: Float,y: Float,pressure: Float)=InkPoint(((x-left-offsetX)/(ratio*zoom)/paperWidth()).coerceIn(0f,1f),((y-top-offsetY)/(ratio*zoom)/paperHeight()).coerceIn(0f,1f),pressure.coerceIn(.1f,1f))
    private fun annotationAt(x: Float,y: Float): ReaderInk? {
        val p=inkPoint(x,y,1f)
        return annotations.filter { it.source==source }.lastOrNull { ink ->
            val areas=zones(ink)
            if(areas.isNotEmpty()) areas.any { it.contains(p.x*paperWidth(),p.y*paperHeight()) }
            else if(ink.anchor!=null) false
            else ink.page==page&&ink.outlines.any { stroke ->
                val xs=stroke.points.map { it.x };val ys=stroke.points.map { it.y }
                xs.isNotEmpty()&&p.x>=xs.min()-.008f&&p.x<=xs.max()+.008f&&p.y>=ys.min()-.008f&&p.y<=ys.max()+.008f
            }
        }
    }
    var paper="#faf7f2"
    var page=1;var source="coranTest";var selected: Int?=null;var playing: Int?=null
    var bookmarks=emptySet<Int>();var difficulties=emptySet<Int>();var onVerse: (Int)->Unit={};var onPage: (Int)->Unit={}
    private var data: JsonObject?=null;private var bitmap: Bitmap?=null;private var bounds: JsonArray?=null
    private val fonts=LruCache<String,Typeface>(4);private val regions=mutableListOf<Pair<RectF,Int>>();private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var zoom=1f;private var offsetX=0f;private var offsetY=0f;private var ratio=1f;private var left=0f;private var top=0f
    private val scale=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener() { override fun onScale(detector: ScaleGestureDetector): Boolean { zoom=(zoom*detector.scaleFactor).coerceIn(1f,3f);invalidate();return true } })
    private val gestures=GestureDetector(context,object: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: android.view.MotionEvent)=true
        override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean { annotationAt(e.x,e.y)?.let { onInkClick(it);return true };val x=(e.x-left-offsetX)/(ratio*zoom);val y=(e.y-top-offsetY)/(ratio*zoom);regions.lastOrNull { it.first.contains(x,y) }?.let { onVerse(it.second) };return true }
        override fun onLongPress(e: MotionEvent) { selectionAt(e.x,e.y)?.let(onSelection) }
        override fun onDoubleTap(e: android.view.MotionEvent): Boolean { zoom=if(zoom>1f) 1f else 2f;offsetX=0f;offsetY=0f;invalidate();return true }
        override fun onScroll(e1: android.view.MotionEvent?,e2: android.view.MotionEvent,distanceX: Float,distanceY: Float): Boolean { if(zoom>1f) { offsetX-=distanceX;offsetY-=distanceY;invalidate() };return true }
        override fun onFling(e1: android.view.MotionEvent?,e2: android.view.MotionEvent,velocityX: Float,velocityY: Float): Boolean { if(!inkDrawing&&zoom==1f&&e1!=null&&kotlin.math.abs(e2.x-e1.x)>80&&kotlin.math.abs(velocityX)>150) onPage(if(e2.x>e1.x) page+1 else page-1);return true }
    })
    fun load(q: Quran,page: Int,source: String,document: JsonObject?,image: Bitmap?,regions: JsonArray?) { this.page=page;this.source=source;inkDrawing=false;inkDraft.clear();data=document;bitmap=image;bounds=regions;zoom=1f;offsetX=0f;offsetY=0f;invalidate();contentDescription="Mushaf page $page" }
    private fun font(name: String): Typeface { val file=when(name) { "surah-name"->"surah-name-v4";"basmala"->"vertopal.com_QCF_Bismillah-Regular";else->name };return fonts[file] ?: Typeface.createFromAsset(context.assets,"fonts/$file.ttf").also { fonts.put(file,it) } }
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if(inkDrawing&&stylusOnly&&event.getToolType(0)!=MotionEvent.TOOL_TYPE_STYLUS) return true
        if(annotationMode&&(data!=null||bitmap!=null)&&(!stylusOnly||event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS)) {
            if(event.pointerCount>1) { inkDrawing=false;inkDraft.clear();onInkActive(false);scale.onTouchEvent(event);invalidate();return true }
            fun add(x: Float,y: Float,p: Float) { if(inkDraft.size<12000) inkDraft+=inkPoint(x,y,p) }
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if(inkTool=="eraser") { inkDrawing=true;onInkActive(true);parent?.requestDisallowInterceptTouchEvent(true);annotationAt(event.x,event.y)?.let(onInkErase);return true }
                    inkDrawing=true;onInkActive(true);parent?.requestDisallowInterceptTouchEvent(true);inkDraft.clear();add(event.x,event.y,event.pressure) }
                MotionEvent.ACTION_MOVE -> { if(!inkDrawing) return true;if(inkTool=="eraser") { annotationAt(event.x,event.y)?.let(onInkErase);return true };for(i in 0 until event.historySize) add(event.getHistoricalX(i),event.getHistoricalY(i),event.getHistoricalPressure(i));add(event.x,event.y,event.pressure) }
                MotionEvent.ACTION_UP -> {
                    if(!inkDrawing) return true;inkDrawing=false;onInkActive(false)
                    if(inkTool=="eraser") { parent?.requestDisallowInterceptTouchEvent(false);return true }
                    add(event.x,event.y,event.pressure);val stroke=InkStroke(inkDraft.toList());inkDraft.clear()
                    if(stroke.points.isNotEmpty()) {
                        val rect=RectF(stroke.points.minOf { it.x }*paperWidth(),stroke.points.minOf { it.y }*paperHeight(),stroke.points.maxOf { it.x }*paperWidth(),stroke.points.maxOf { it.y }*paperHeight())
                        onInkStroke(stroke,regions.filter { RectF.intersects(it.first,rect) }.map { it.second }.distinct())
                    }
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                MotionEvent.ACTION_CANCEL -> { inkDrawing=false;onInkActive(false);inkDraft.clear();parent?.requestDisallowInterceptTouchEvent(false) }
            }
            invalidate();return true
        }
        scale.onTouchEvent(event);if(!scale.isInProgress) gestures.onTouchEvent(event);return true
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas);val b=bitmap;val w=b?.width?.toFloat()?:1000f;val h=b?.height?.toFloat()?:2120f
        ratio=minOf(width/w,height/h);left=(width-w*ratio)/2;top=(height-h*ratio)/2
        canvas.save();canvas.translate(left+offsetX,top+offsetY);canvas.scale(ratio*zoom,ratio*zoom);regions.clear();wordRegions.clear();wordDetails.clear()
        if(b!=null) { paint.color=Color.WHITE;canvas.drawBitmap(b,0f,0f,paint)
            bounds?.forEach { raw -> val r=raw.jsonArray;if(r.size>=7) { val surah=r[0].jsonPrimitive.int;val ayah=r[1].jsonPrimitive.int;val q=(context.applicationContext as com.msoumaya.androidcoran.CoranApplication).repository.quran;val id=q.id(surah,ayah);val rect=RectF(r[3].jsonPrimitive.float,r[5].jsonPrimitive.float,r[4].jsonPrimitive.float,r[6].jsonPrimitive.float);regions+=rect to id;overlay(canvas,rect,id) } }
        } else data?.let { d ->
            paint.color=Color.parseColor(paper);canvas.drawRect(0f,0f,w,h,paint);paint.color=Color.rgb(121,94,80);paint.typeface=Typeface.DEFAULT;paint.textSize=30f;canvas.drawText("Juz ${d.num("juz")}",40f,135f,paint);canvas.drawText(page.toString(),480f,2070f,paint)
            val lines=d.arr("lines");val start=175f+(1830f-lines.size*122f)/2
            lines.forEachIndexed { i,raw -> val line=raw.jsonObject;val cy=start+i*122f+61f
                when(line.str("type")) {
                    "ayah" -> { paint.typeface=font(page.toString());paint.textSize=d.qcfFontSize();val words=line.arr("words").map { it.jsonArray };val widths=words.map { paint.measureText(it[4].jsonPrimitive.content) };val sum=widths.sum();val centered=line.flag("centered");val gap=if(centered) 8f else if(words.size>1) ((940f-sum)/(words.size-1)).coerceAtLeast(0f) else 0f;var x=if(centered) 500f+(sum+gap*(words.size-1))/2 else 970f
                        words.forEachIndexed { wi,word -> val width=widths[wi];x-=width;val q=(context.applicationContext as com.msoumaya.androidcoran.CoranApplication).repository.quran;val id=q.id(word[1].jsonPrimitive.int,word[2].jsonPrimitive.int);val rect=RectF(x,cy-61f,x+width,cy+61f);regions+=rect to id;wordRegions[word[0].jsonPrimitive.int]=rect;wordDetails[word[0].jsonPrimitive.int]=json("wordId" to word[0].jsonPrimitive.int,"wordIndex" to word[3].jsonPrimitive.int,"wordText" to word.getOrNull(5)?.jsonPrimitive?.content);overlay(canvas,rect,id);paint.color=Color.BLACK;paint.typeface=font(page.toString());paint.textSize=d.qcfFontSize();val baseline=cy-(paint.fontMetrics.ascent+paint.fontMetrics.descent)/2;canvas.drawText(word[4].jsonPrimitive.content,x,baseline,paint);x-=gap }
                    }
                    "surah_name" -> { paint.color=Color.rgb(216,195,159);paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;canvas.drawRoundRect(RectF(30f,cy-50f,970f,cy+50f),16f,16f,paint);paint.style=Paint.Style.FILL;paint.color=Color.BLACK;paint.typeface=font("surah-name");paint.textSize=90f;val text="surah${line.num("surah").toString().padStart(3,'0')}";canvas.drawText(text,500f-paint.measureText(text)/2,cy-(paint.fontMetrics.ascent+paint.fontMetrics.descent)/2,paint) }
                    "basmallah" -> { paint.color=Color.BLACK;paint.typeface=font("basmala");paint.textSize=80f;val surah=lines.drop(i+1).firstOrNull { it.jsonObject.arr("words").isNotEmpty() }?.jsonObject?.arr("words")?.first()?.jsonArray?.get(1)?.jsonPrimitive?.int?:d.num("surah");val text=when(surah) { 2->"ﲚﲛﲞﲤ";95,97->"ﭗﲫﲮﲴ";else->"ﲪﲫﲮﲴ" };canvas.drawText(text,500f-paint.measureText(text)/2,cy-(paint.fontMetrics.ascent+paint.fontMetrics.descent)/2,paint) }
                }
            }
        };annotations.filter { it.source==source&&(it.page==page||it.anchor!=null) }.forEach { ink -> val areas=zones(ink);if(ink.anchor==null||areas.isNotEmpty()) drawReaderMark(canvas,ink,areas,w,h) }
        if(inkDraft.isNotEmpty()) drawReaderMark(canvas,ReaderInk("draft",page,listOf(InkStroke(inkDraft)),emptyList(),emptyList(),"",source=source,tool=inkTool,color=inkColor,width=inkWidth,penStyle=inkPenStyle),emptyList(),w,h)
        canvas.restore()
    }
    private fun overlay(canvas: Canvas,r: RectF,id: Int) { val color=when { id==playing -> 0x5554734E;id==selected -> 0x557B285C;id in difficulties -> 0x33A85353;id in bookmarks -> 0x33C89A52;else -> return };paint.color=color;canvas.drawRoundRect(r,8f,8f,paint) }
}
