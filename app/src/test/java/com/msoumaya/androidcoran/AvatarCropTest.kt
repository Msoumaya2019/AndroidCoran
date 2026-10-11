package com.msoumaya.androidcoran
import com.msoumaya.androidcoran.domain.*
import org.junit.Assert.*
import org.junit.Test
class AvatarCropTest {
 @Test fun landscapeCanSelectEitherEdge() { assertEquals(AvatarCropRect(0,0,600),avatarCropRect(1200,600,AvatarCrop(x=0f)));assertEquals(AvatarCropRect(600,0,600),avatarCropRect(1200,600,AvatarCrop(x=1f))) }
 @Test fun portraitCenterAndZoomRemainSquare() { assertEquals(AvatarCropRect(0,300,600),avatarCropRect(600,1200,AvatarCrop()));assertEquals(AvatarCropRect(150,450,300),avatarCropRect(600,1200,AvatarCrop(zoom=2f))) }
 @Test fun cropStaysWithinImageAtAllBounds() { for(w in listOf(1,13,1024)) for(h in listOf(1,19,768)) for(x in listOf(-1f,0f,0.5f,1f,2f)) { val r=avatarCropRect(w,h,AvatarCrop(x,x,9f));assertTrue(r.left>=0&&r.top>=0&&r.side>=1&&r.left+r.side<=w&&r.top+r.side<=h) } }
 @Test(expected=IllegalArgumentException::class) fun invalidCoordinatesAreRejected() { avatarCropRect(512,512,AvatarCrop(x=Float.NaN)) }
}
