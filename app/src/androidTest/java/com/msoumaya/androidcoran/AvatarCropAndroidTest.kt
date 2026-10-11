package com.msoumaya.androidcoran
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.msoumaya.androidcoran.data.*
import com.msoumaya.androidcoran.domain.*
import com.msoumaya.androidcoran.ui.AvatarCropDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class AvatarCropAndroidTest {
 @get:Rule val compose=createComposeRule()
 @Test fun allExifOrientationsTransformPixelsCorrectly() {
  val expected=listOf(listOf(1,2,3,4,5,6),listOf(2,1,4,3,6,5),listOf(6,5,4,3,2,1),listOf(5,6,3,4,1,2),listOf(1,3,5,2,4,6),listOf(5,3,1,6,4,2),listOf(6,4,2,5,3,1),listOf(2,4,6,1,3,5))
  for(orientation in 1..8) {
   val image=Bitmap.createBitmap(2,3,Bitmap.Config.ARGB_8888);for(i in 0..5) image.setPixel(i%2,i/2,Color.rgb(i+1,0,0))
   val result=orientAvatar(image,orientation)
   try { assertEquals(if(orientation>=5) 3 else 2,result.width);assertEquals(if(orientation>=5) 2 else 3,result.height);assertEquals(expected[orientation-1],(0..5).map { Color.red(result.getPixel(it%result.width,it/result.width)) }) } finally { result.recycle() }
  }
 }
 @Test fun selectedRegionProduces512PixelJpegWithoutRecyclingPreview() {
  val image=Bitmap.createBitmap(1024,512,Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(image);canvas.drawColor(Color.RED);val paint=android.graphics.Paint().apply { color=Color.BLUE };canvas.drawRect(512f,0f,1024f,512f,paint)
  try { val bytes=encodeAvatar(image,AvatarCrop(x=1f));assertTrue(bytes.size<=2097152);assertFalse(image.isRecycled);val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size);try { assertEquals(512,decoded.width);assertEquals(512,decoded.height);assertTrue(Color.blue(decoded.getPixel(256,256))>230);assertTrue(Color.red(decoded.getPixel(256,256))<20) } finally { decoded.recycle() } } finally { image.recycle() }
 }
 @Test fun selectionSurvivesRestorationAndCancelDoesNotSubmit() {
  val image=Bitmap.createBitmap(20,10,Bitmap.Config.ARGB_8888);var selected: AvatarCrop?=null;var cancelled=0
  val restore=StateRestorationTester(compose)
  try {
   restore.setContent { MaterialTheme { AvatarCropDialog(image,false,{cancelled++},{selected=it}) } }
   compose.onNodeWithContentDescription("Position horizontale").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
   compose.onNodeWithContentDescription("Zoom").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
   restore.emulateSavedInstanceStateRestore()
   compose.onNodeWithText("Annuler").performClick();assertEquals(1,cancelled);assertNull(selected)
   compose.onNodeWithText("Utiliser cette photo").performClick();assertEquals(AvatarCrop(1f,0.5f,2f),selected)
   compose.onNodeWithText("Recentrer").performScrollTo().performClick();compose.onNodeWithText("Utiliser cette photo").performClick();assertEquals(AvatarCrop(),selected)
  } finally { image.recycle() }
 }
}
