package com.msoumaya.androidcoran.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.work.*
import com.msoumaya.androidcoran.data.QuranDownloadWorker

@Composable fun DownloadScreen() {
    val context=LocalContext.current
    val manager=remember { WorkManager.getInstance(context) }
    val infos by remember { manager.getWorkInfosForUniqueWorkFlow(QuranDownloadWorker.NAME) }.collectAsState(initial=emptyList())
    val work=infos.firstOrNull();val complete=work?.state==WorkInfo.State.SUCCEEDED||QuranDownloadWorker.ready(context)
    PageList { Text("Coran 1441",style=MaterialTheme.typography.headlineSmall);Text("Archive originale : 102,6 Mo · 604 pages · 15 lignes par page");Text(if(complete) "Téléchargement installé" else work?.state?.name?:"Non téléchargé");Text(work?.progress?.getString("phase")?:"");LinearProgressIndicator(progress={(work?.progress?.getInt("percent",0)?:0)/100f});if(!complete) Button(onClick={QuranDownloadWorker.enqueue(context)}) { Text("Télécharger / reprendre") };if(work?.state==WorkInfo.State.RUNNING||work?.state==WorkInfo.State.ENQUEUED) TextButton(onClick={manager.cancelUniqueWork(QuranDownloadWorker.NAME)}) { Text("Suspendre") };work?.outputData?.getString("error")?.let { Text(it) } }
}
