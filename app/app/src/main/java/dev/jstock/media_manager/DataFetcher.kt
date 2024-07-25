package dev.jstock.media_manager

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.toJavaAddress
import io.ktor.util.network.hostname
import io.ktor.utils.io.core.readBytes
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class DataFetcher(
) : ViewModel() {
    private val _uiState = MutableStateFlow(mutableStateListOf<JobData>())
    val uiState = _uiState.asStateFlow()

    private val client = HttpClient {
        install(WebSockets)
    }

    private val datagramSock = aSocket(SelectorManager(Dispatchers.IO)).udp()
        .bind(InetSocketAddress(hostname = "0.0.0.0", port = 45777))
    private var datagramJob: Job? = null

    init {
        startDatagramMonitoring()
    }

    fun startDatagramMonitoring() {
        Log.d("Datagram", "Started")
        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                outer@ while (true) {
                    val datagramPacket = datagramSock.receive()
                    val hostname = datagramPacket.address.toJavaAddress().hostname

                    if (datagramPacket.packet.readBytes()
                            .contentEquals(byteArrayOf(80, 80, 80, 80))
                    ) {

                        for (job in _uiState.value) {
                            if (job.hostName == hostname) continue@outer
                        }

                        runFetcher(
                            hostname, 45777
                        ) //toJavaAddress().hostname does not seem to be main safe :/

                    }
                    delay(500)
                }
            } catch (e: Exception) {
                Log.e("Datagram", e.toString())
            }
        }

        datagramJob = job
    }

    fun stopDatagramMonitoring() {
        datagramJob?.cancel()
        datagramJob = null
        Log.d("Datagram", "Cancelled")

    }

    //Note -> The update method was from debugging and I realised it was a completely different bug. I am too lazy to change it back

    private fun addNewJob(jobData: JobData) {
        _uiState.update { data ->
            data.add(jobData)

            data
        }
    }

    private fun findOldData(jobUUID: UUID): JobData? {
        return _uiState.value.find { item -> item.identifier == jobUUID }
    }

    private fun updateJobDataMusicInfo(jobData: JobData, newMusicInfoList: List<MusicInfo>) {
        _uiState.update { data ->
            val i = data.indexOf(jobData)
            data[i] = data[i].copy(musicInfo = newMusicInfoList)
            data
        }
    }

    private fun removeJob(jobUUID: UUID) {
        val foundJob = _uiState.value.find { item -> item.identifier == jobUUID }

        if (foundJob != null) {
            _uiState.update { data ->
                data.remove(foundJob)

                data
            }
        }
    }

    fun killFetchers() {
        viewModelScope.launch {
            for (job in _uiState.value) {
                job.job.cancel()
            }
            _uiState.value.clear()
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun runFetcher(host: String, port: Int) {
        Log.d("Soc", "Fetcher started")
        val jobUUID = UUID.randomUUID()

        val job = viewModelScope.launch(Dispatchers.IO) {
            try {


                client.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                ) {
                    while (true) {

                        val frame = (incoming.receive() as? Frame.Text ?: continue).readText()
                        val receivedData = Json.decodeFromString<List<ReceivedData>>(frame)

                        val oldMusicInfoList = findOldData(jobUUID) ?: continue

                        val newMusicInfo = receivedData.map { rawData ->
                            val newMusicInfo = MusicInfo(
                                source = rawData.source_id,
                                album = rawData.album,
                                artist = rawData.artist,
                                artwork = ImageBitmap(1, 1),
                                endTime = rawData.end_time,
                                playing = rawData.playing,
                                position = rawData.position,
                                songName = rawData.song_name,
                                startTime = rawData.start_time,
                            )

                            if (rawData.artwork_changed) {
                                val bitmap = BitmapFactory.decodeByteArray(
                                    rawData.artwork.toByteArray(),
                                    0,
                                    rawData.artwork.size
                                )!!.asImageBitmap()

                                newMusicInfo.artwork = bitmap

                            } else {
                                for (oldMusicInfo in oldMusicInfoList.musicInfo) {
                                    if (oldMusicInfo.source == newMusicInfo.source) {
                                        newMusicInfo.artwork = oldMusicInfo.artwork
                                    }
                                }
                            }

                            newMusicInfo
                        }

                        updateJobDataMusicInfo(oldMusicInfoList, newMusicInfo)
                    }
                }
            } catch (e: Exception) {
                Log.d("Soc", "$host has exited")
                Log.e("Soc", e.toString())
                removeJob(jobUUID)
                coroutineContext.job.cancel()
            }
        }

        addNewJob(JobData(jobUUID, job, host, port, listOf()))
    }
}