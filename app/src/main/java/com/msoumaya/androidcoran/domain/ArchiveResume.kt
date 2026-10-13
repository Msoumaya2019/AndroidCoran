package com.msoumaya.androidcoran.domain

/** Validate the exact byte interval before appending to an existing archive. */
fun archiveResumeOffset(status: Int,contentRange: String?,stored: Long,total: Long): Long {
    require(total>0&&stored in 0 until total)
    if(status==200) return 0
    require(status==206) { "Réponse de téléchargement $status" }
    val match=Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matchEntire(contentRange.orEmpty())
    require(match!=null) { "Reprise de téléchargement incohérente" }
    val start=match.groupValues[1].toLongOrNull()
    val end=match.groupValues[2].toLongOrNull()
    val size=match.groupValues[3].toLongOrNull()
    require(start==stored&&size==total&&end!=null&&end>=stored&&end<total) { "Reprise de téléchargement incohérente" }
    return stored
}
