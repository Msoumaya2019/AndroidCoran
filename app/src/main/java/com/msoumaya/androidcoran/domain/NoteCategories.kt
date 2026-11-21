package com.msoumaya.androidcoran.domain

val noteCategories=listOf("Erreur de mémorisation","Mauvaise prononciation","Correction Tajweed","Verset difficile","Oubli / hésitation","Confusion entre versets","Règle à apprendre","Conseil de révision","Note personnelle","Autre")
fun ReaderInk.canSaveNote()=categories.any { it.isNotBlank() }

/** Zoom around the screen focal point, preserving the same paper coordinate beneath it. */
data class ReaderTransform(val zoom: Float=1f,val x: Float=0f,val y: Float=0f) {
 fun scale(factor: Float,focusX: Float,focusY: Float,originX: Float,originY: Float): ReaderTransform {
  val next=(zoom*factor).coerceIn(1f,4f);val k=next/zoom
  return ReaderTransform(next,focusX-originX-(focusX-originX-x)*k,focusY-originY-(focusY-originY-y)*k)
 }
}
