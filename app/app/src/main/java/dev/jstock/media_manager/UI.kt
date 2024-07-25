@file:Suppress("DEPRECATION")

package dev.jstock.media_manager

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class
)
@Composable
fun UI(
    dataSocViewModel: DataFetcher = viewModel()
) {
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )
    val fontName = GoogleFont("Quicksand")

    val extraBoldFamily = FontFamily(
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.W700)
    )
    val boldFamily = FontFamily(
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.W600)
    )
    val normalFamily = FontFamily(
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.W500)
    )

    val dataSocState by dataSocViewModel.uiState.collectAsState()

    var datagramMonitoringState by remember {
        mutableStateOf(true)
    }

    var hostname by remember {
        mutableStateOf("")
    }
    var port by remember {
        mutableStateOf("")
    }

    val pagerState = rememberPagerState(pageCount = {
        if (dataSocState.size == 0) {
            2
        } else {
            dataSocState.size + 1
        }
    }, initialPage = 1)

    MaterialTheme {
        HorizontalPager(state = pagerState, pageSize = PageSize.Fill, pageSpacing = 0.dp) { dataSockPage ->
            val musicInfoPagerState =
                rememberPagerState {
                    if (dataSockPage > 0 && dataSocState.size > 0) {
                        dataSocState[dataSockPage - 1].musicInfo.size
                    } else {
                        0
                    }
                }

            when (dataSockPage) {
                0 ->
                    Scaffold(
                        topBar = {
                            TopAppBar(title = {
                                Text(
                                    "Media Manager",
                                    fontWeight = FontWeight.W500
                                )
                            })
                        },
                    ) { innerPadding ->
                        Column(
                            modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Settings",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Toggle UDP monitoring")
                                        Switch(
                                            checked = datagramMonitoringState,
                                            onCheckedChange = {
                                                datagramMonitoringState = it



                                                if (datagramMonitoringState) {
                                                    dataSocViewModel.startDatagramMonitoring()
                                                } else {
                                                    dataSocViewModel.stopDatagramMonitoring()
                                                }
                                            })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(value = hostname,
                                            onValueChange = { hostname = it },
                                            modifier = Modifier.fillMaxWidth(0.6f),
                                            singleLine = true,
                                            label = {
                                                Text(
                                                    "Hostname",
                                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                                )
                                            })

                                        OutlinedTextField(value = port,
                                            onValueChange = { port = it },
                                            modifier = Modifier.fillMaxWidth(0.8f),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            label = {
                                                Text(
                                                    "Port",
                                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                                )
                                            })

                                    }

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            dataSocViewModel.runFetcher(
                                                host = hostname,
                                                port = port.toInt()
                                            )

                                        }) {
                                        Text(
                                            "Add socket con",
                                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                        )
                                    }

                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { dataSocViewModel.killFetchers() }) {
                                        Text(
                                            "Kill sockets",
                                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Running Jobs",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    )

                                    dataSocState.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Job hostname",
                                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                            )
                                            Text(text = item.hostName)
                                        }
                                    }
                                }
                            }
                        }
                    }

                else -> {
                    if (dataSocState.size == 0) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .height(300.dp)
                                    .width(400.dp)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {

                                    Icon(imageVector = Icons.Default.Info, contentDescription = "", modifier = Modifier.size(50.dp), tint = Color.Black)

                                    Text(
                                        "Please start a stream",
                                        fontWeight = FontWeight.W700,
                                        fontSize = 30.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "This mostly requires you to open the app on a PC, and then making sure both devices are connected to the same network!",
                                        fontWeight = FontWeight.W400,
                                        fontSize = 20.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        VerticalPager(state = musicInfoPagerState) { musicInfoPage ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(Color.Black)
                                    ) {
                                        LegacyBlurImage(
                                            bitmap = dataSocState[dataSockPage - 1].musicInfo[musicInfoPage].artwork,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight()
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.8f),

                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxHeight(0.72f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(35.dp))

                                        ) {
                                            Image(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(),
                                                bitmap = dataSocState[dataSockPage - 1].musicInfo[musicInfoPage].artwork,
                                                contentDescription = "img"
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth(0.95f)
                                                .fillMaxHeight(0.72f),
                                            verticalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Text(
                                                text = dataSocState[dataSockPage - 1].musicInfo[musicInfoPage].songName,
                                                fontFamily = extraBoldFamily,
                                                fontSize = 40.sp,
                                                lineHeight = 40.sp,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                            )
                                            Text(
                                                text = dataSocState[dataSockPage - 1].musicInfo[musicInfoPage].artist,
                                                fontFamily = boldFamily,
                                                fontSize = 30.sp,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                            )

                                            LinearProgressIndicator(
                                                progress = (dataSocState[dataSockPage - 1].musicInfo[musicInfoPage].position.toFloat() / dataSocState[dataSockPage - 1].musicInfo[musicInfoPage].endTime.toFloat()),
                                                color = Color.White,
                                                trackColor = Color(180, 180, 180),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = 3.dp,
                                                        vertical = (20.dp - 2.5.dp)
                                                    )
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .height(5.dp)
                                                    .background(Color(180, 180, 180)),
                                                strokeCap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

//DEPRECATED
//WHOOP, actually thank samsung for this stackoverflow paste
//They decided to lock the bootloader for USA/Canada models, so I cannot flash a modern rom
//and as the blur api does not support android 11, this is the result
//Cheers lads
@Composable
private fun LegacyBlurImage(
    bitmap: ImageBitmap,
    modifier: Modifier
) {
    val bitmapOriginal = bitmap.asAndroidBitmap()

    val rs = RenderScript.create(LocalContext.current)

    val input = Allocation.createFromBitmap(rs, bitmapOriginal)
    val output = Allocation.createTyped(rs, input.type)
    val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    script.setRadius(5f)
    script.setInput(input)
    script.forEach(output)

    val newBitmap =
        Bitmap.createBitmap(bitmapOriginal.width, bitmapOriginal.height, bitmapOriginal.config)
    output.copyTo(newBitmap)
    output.destroy()


    val filterValue = -35
    val filter = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, filterValue.toFloat(),
            0f, 1f, 0f, 0f, filterValue.toFloat(),
            0f, 0f, 1f, 0f, filterValue.toFloat(),
            0f, 0f, 0f, 1f, 0f
        )
    )

    Image(
        bitmap = newBitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier,
        colorFilter = ColorFilter.colorMatrix(filter)
    )
}
