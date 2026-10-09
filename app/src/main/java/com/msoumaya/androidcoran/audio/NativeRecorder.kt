package com.msoumaya.androidcoran.audio
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.util.UUID
/** Owns one microphone draft. Saved files are transferred to the account store. */
class NativeRecorder(private val context: Context,private val folder: File) : AutoCloseable {
 enum class Phase { Idle,Recording,Paused,Preview,Saving,Saved }
 var phase=Phase.Idle;private set
 var file: File?=null;private set
 private var recorder: MediaRecorder?=null
 private var closed=false
 private var elapsed=0L;private var segmentStart=0L
 val duration: Long get()=elapsed+if(phase==Phase.Recording) SystemClock.elapsedRealtime()-segmentStart else 0L
 fun start() {
  check(phase==Phase.Idle);closed=false;check(folder.mkdirs()||folder.isDirectory)
  val output=File(folder,"${UUID.randomUUID()}.m4a")
  val m=if(Build.VERSION.SDK_INT>=31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
  file=output;recorder=m
  try {
   m.setAudioSource(MediaRecorder.AudioSource.MIC);m.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);m.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
   m.setAudioChannels(1);m.setAudioSamplingRate(44100);m.setAudioEncodingBitRate(64000);m.setOutputFile(output.path);m.prepare();m.start()
   elapsed=0;segmentStart=SystemClock.elapsedRealtime();phase=Phase.Recording
  } catch(e: Exception) { close();throw e }
 }
 fun pause() { check(phase==Phase.Recording);recorder!!.pause();elapsed=duration;phase=Phase.Paused }
 fun resume() { check(phase==Phase.Paused);recorder!!.resume();segmentStart=SystemClock.elapsedRealtime();phase=Phase.Recording }
 fun finish(): File {
  check(phase==Phase.Recording||phase==Phase.Paused);val length=duration
  try { recorder!!.stop();recorder!!.release();recorder=null;elapsed=length;phase=Phase.Preview;return file!! }
  catch(e: Exception) { close();throw IllegalStateException("Enregistrement trop court ou interrompu.",e) }
 }
 fun beginSave() { check(phase==Phase.Preview);phase=Phase.Saving }
 fun saved() { check(phase==Phase.Preview||phase==Phase.Saving);phase=Phase.Saved }
 fun saveFailed() { if(phase==Phase.Saving) { phase=Phase.Preview;if(closed) close() } }
 fun restart() { check(phase!=Phase.Saving);close();file=null;elapsed=0;phase=Phase.Idle }
 override fun close() {
  closed=true
  recorder?.let { runCatching { it.stop() };it.release() };recorder=null
  if(phase!=Phase.Saved&&phase!=Phase.Saving) file?.delete()
  if(phase!=Phase.Saved&&phase!=Phase.Saving) { file=null;elapsed=0;phase=Phase.Idle }
 }
}
