package dev.jstock.media_manager

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import java.util.UUID


data class JobData(
    val identifier: UUID,
    val job: Job,
    val hostName: String,
    val port: Int,
    val musicInfo: List<MusicInfo>
)

@Serializable
data class MusicInfo (
    var source: String,
    val songName: String,
    val artist: String,
    val album: String,
    val startTime: ULong,
    val endTime: ULong,
    val position: ULong,
    val playing: Boolean,
    var artwork: ImageBitmap,
)

@Suppress("PropertyName")
@Serializable
data class ReceivedData @OptIn(ExperimentalUnsignedTypes::class) constructor(
    val source_id: String,
    val song_name: String,
    val artist: String,
    val album: String,
    val start_time: ULong,
    val end_time: ULong,
    val position: ULong,
    val playing: Boolean,
    val artwork: UByteArray,
    val artwork_changed: Boolean,
)