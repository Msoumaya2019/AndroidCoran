package com.msoumaya.androidcoran.domain
import kotlin.math.floor
data class AvatarCrop(val x: Float=0.5f,val y: Float=0.5f,val zoom: Float=1f)
data class AvatarCropRect(val left: Int,val top: Int,val side: Int)
fun avatarCropRect(width: Int,height: Int,crop: AvatarCrop): AvatarCropRect {
 require(width>0&&height>0&&crop.x.isFinite()&&crop.y.isFinite()&&crop.zoom.isFinite())
 val side=floor(minOf(width,height)/crop.zoom.coerceIn(1f,3f)).toInt().coerceAtLeast(1)
 return AvatarCropRect(((width-side)*crop.x.coerceIn(0f,1f)).toInt(),((height-side)*crop.y.coerceIn(0f,1f)).toInt(),side)
}
