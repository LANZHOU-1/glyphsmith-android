package com.lanzhou.zj

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val viewModel: ArtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TextArtTheme {
                TextArtScreen(viewModel)
            }
        }
    }
}

private val ParamsSaver = listSaver<Params, Any>(
    save = {
        listOf(
            it.mode.name, it.width, it.contrast, it.autoLevel,
            it.invert, it.backgroundBlack, it.blockStyle, it.fontSize,
        )
    },
    restore = { values ->
        Params(
            mode = Mode.valueOf(values[0] as String),
            width = values[1] as Int,
            contrast = values[2] as Float,
            autoLevel = values[3] as Boolean,
            invert = values[4] as Boolean,
            backgroundBlack = values[5] as Boolean,
            blockStyle = values[6] as Boolean,
            fontSize = values[7] as Int,
        )
    },
)

@Composable
fun TextArtScreen(viewModel: ArtViewModel) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var params by rememberSaveable(stateSaver = ParamsSaver) { mutableStateOf(Params()) }

    AnimatedContent(
        targetState = showAbout,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(320)) + fadeIn(tween(240, delayMillis = 40)))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = tween(320)) + fadeOut(tween(220)))
            } else {
                (slideInHorizontally(initialOffsetX = { -it / 5 }, animationSpec = tween(320)) + fadeIn(tween(240, delayMillis = 40)))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = tween(320)) + fadeOut(tween(220)))
            }
        },
        label = "screen",
    ) { about ->
        if (about) {
            AboutScreen(onBack = { showAbout = false })
        } else {
            MainScreen(
                bitmap = viewModel.bitmap,
                sourceName = viewModel.sourceName,
                params = params,
                onBitmap = { viewModel.bitmap = it },
                onSourceName = { viewModel.sourceName = it },
                onParams = { params = it },
                onAbout = { showAbout = true },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    bitmap: Bitmap?,
    sourceName: String,
    params: Params,
    onBitmap: (Bitmap) -> Unit,
    onSourceName: (String) -> Unit,
    onParams: (Params) -> Unit,
    onAbout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var result by remember { mutableStateOf<ArtResult?>(null) }
    var busy by remember { mutableStateOf(true) }
    var showProgress by remember { mutableStateOf(false) }

    fun toast(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(Unit) {
        if (bitmap == null) {
            onBitmap(withContext(Dispatchers.Default) { Converter.makeSample() })
        }
    }

    LaunchedEffect(bitmap, params) {
        val source = bitmap ?: return@LaunchedEffect
        busy = true
        result = withContext(Dispatchers.Default) { Converter.convert(source, params) }
        busy = false
    }

    LaunchedEffect(busy) {
        if (busy) {
            delay(150)
            showProgress = true
        } else {
            showProgress = false
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val loaded = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
                if (loaded != null) {
                    onBitmap(loaded)
                    onSourceName(queryName(context, uri))
                } else {
                    toast("图片加载失败")
                }
            }
        }
    }

    val saveTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val text = (result as? ArtResult.Plain)?.text
            if (text == null) {
                toast("当前没有可保存的文本")
            } else {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val stream = context.contentResolver.openOutputStream(uri)
                                ?: error("无法打开输出流")
                            stream.use { it.write(text.toByteArray()) }
                        }.isSuccess
                    }
                    toast(if (ok) "已保存 TXT" else "保存失败")
                }
            }
        }
    }

    val saveHtml = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if (uri != null) {
            val colored = result as? ArtResult.Colored
            if (colored == null) {
                toast("当前没有可保存的网页")
            } else {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val page = Converter.htmlPage(colored.html, params.backgroundBlack, params.fontSize * 2)
                            val stream = context.contentResolver.openOutputStream(uri)
                                ?: error("无法打开输出流")
                            stream.use { it.write(page.toByteArray()) }
                        }.isSuccess
                    }
                    toast(if (ok) "已保存网页" else "保存失败")
                }
            }
        }
    }

    val saveImage = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) {
            val current = result
            if (current == null) {
                toast("当前没有可保存的图片")
            } else {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val bitmap = withContext(Dispatchers.Default) { PngExport.render(current, params) }
                            try {
                                val stream = context.contentResolver.openOutputStream(uri)
                                    ?: error("无法打开输出流")
                                stream.use { out ->
                                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                                        error("PNG 编码失败")
                                    }
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }.isSuccess
                    }
                    toast(if (ok) "已保存图片" else "保存失败")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Glyphsmith") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    actions = {
                        IconButton(onClick = onAbout) {
                            Icon(Icons.Outlined.Info, contentDescription = "关于")
                        }
                    },
                )
                AnimatedVisibility(
                    visible = showProgress,
                    enter = fadeIn(tween(150)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SourceCard(
                bitmap = bitmap,
                sourceName = sourceName,
                onPick = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onSample = {
                    scope.launch {
                        onBitmap(withContext(Dispatchers.Default) { Converter.makeSample() })
                        onSourceName("示例图")
                    }
                },
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Mode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = params.mode == mode,
                        onClick = { onParams(params.copy(mode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Mode.entries.size),
                    ) {
                        Text(modeLabel(mode))
                    }
                }
            }

            ParamsCard(params = params, onChange = onParams)

            ActionsRow(
                result = result,
                onCopy = { text ->
                    copyText(context, text)
                    toast("已复制到剪贴板")
                },
                onSaveTxt = { saveTxt.launch("Glyphsmith.txt") },
                onSaveHtml = { saveHtml.launch("Glyphsmith.html") },
                onSaveImage = { saveImage.launch("Glyphsmith.png") },
                onShareText = { shareText(context, it) },
                onShareImage = {
                    result?.let { current ->
                        scope.launch {
                            if (!sharePng(context, current, params)) toast("生成图片失败")
                        }
                    }
                },
            )

            PreviewCard(result = result, params = params)

            Text(
                text = result?.let { "输出 ${it.cols} × ${it.rows} 字符" } ?: "正在生成…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun modeLabel(mode: Mode): String = when (mode) {
    Mode.CHINESE -> "中文"
    Mode.ASCII -> "ASCII"
    Mode.COLOR -> "彩色"
}

@Composable
private fun SourceCard(
    bitmap: Bitmap?,
    sourceName: String,
    onPick: () -> Unit,
    onSample: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = bitmap?.let { "${it.width} × ${it.height} 像素" } ?: "未选择图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPick) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("选择图片")
                }
                OutlinedButton(onClick = onSample) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("示例图")
                }
            }
        }
    }
}

@Composable
private fun ParamsCard(params: Params, onChange: (Params) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SliderRow(
                label = "宽度",
                value = params.width.toFloat(),
                range = 30f..240f,
                valueText = params.width.toString(),
            ) { onChange(params.copy(width = it.roundToInt())) }

            SliderRow(
                label = "对比度",
                value = params.contrast,
                range = 1f..3f,
                valueText = "%.1f".format(params.contrast),
            ) { onChange(params.copy(contrast = it)) }

            AnimatedVisibility(
                visible = params.mode == Mode.COLOR,
                enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SliderRow(
                        label = "预览字号",
                        value = params.fontSize.toFloat(),
                        range = 6f..22f,
                        valueText = params.fontSize.toString(),
                    ) { onChange(params.copy(fontSize = it.roundToInt())) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "背景",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = params.backgroundBlack,
                                onClick = { onChange(params.copy(backgroundBlack = true)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) { Text("黑色") }
                            SegmentedButton(
                                selected = !params.backgroundBlack,
                                onClick = { onChange(params.copy(backgroundBlack = false)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) { Text("白色") }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "样式",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = !params.blockStyle,
                                onClick = { onChange(params.copy(blockStyle = false)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) { Text("字符") }
                            SegmentedButton(
                                selected = params.blockStyle,
                                onClick = { onChange(params.copy(blockStyle = true)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) { Text("色块") }
                        }
                    }
                }
            }

            SwitchRow(label = "自动色阶", checked = params.autoLevel) {
                onChange(params.copy(autoLevel = it))
            }

            AnimatedVisibility(
                visible = params.mode != Mode.COLOR,
                enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220)),
            ) {
                SwitchRow(label = "深色背景反相", checked = params.invert) {
                    onChange(params.copy(invert = it))
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun AnimatedContentTransitionScope<ArtResult?>.artFade(): ContentTransform {
    val wasColored = initialState is ArtResult.Colored
    val isColored = targetState is ArtResult.Colored
    return if (wasColored != isColored) {
        fadeIn(tween(200)) togetherWith fadeOut(tween(120))
    } else {
        EnterTransition.None togetherWith ExitTransition.None
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsRow(
    result: ArtResult?,
    onCopy: (String) -> Unit,
    onSaveTxt: () -> Unit,
    onSaveHtml: () -> Unit,
    onSaveImage: () -> Unit,
    onShareText: (String) -> Unit,
    onShareImage: () -> Unit,
) {
    AnimatedContent(
        targetState = result,
        transitionSpec = { artFade() },
        label = "actions",
    ) { current ->
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (current) {
                is ArtResult.Plain -> {
                    FilledTonalButton(onClick = { onCopy(current.text) }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("复制")
                    }
                    OutlinedButton(onClick = onSaveTxt) { Text("保存 TXT") }
                    OutlinedButton(onClick = onSaveImage) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("保存图片")
                    }
                    OutlinedButton(onClick = { onShareText(current.text) }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("分享")
                    }
                }
                is ArtResult.Colored -> {
                    FilledTonalButton(onClick = onSaveHtml) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("保存网页")
                    }
                    OutlinedButton(onClick = onSaveImage) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("保存图片")
                    }
                    OutlinedButton(onClick = onShareImage) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("分享图片")
                    }
                }
                null -> {}
            }
        }
    }
}

@Composable
private fun PreviewCard(result: ArtResult?, params: Params) {
    val background by animateColorAsState(
        targetValue = when {
            params.mode == Mode.COLOR -> if (params.backgroundBlack) Color.Black else Color.White
            params.invert -> Color(0xFF101010)
            else -> Color.White
        },
        animationSpec = tween(250),
        label = "previewBackground",
    )
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 430.dp)
                .background(background)
                .padding(12.dp),
        ) {
            AnimatedContent(
                targetState = result,
                transitionSpec = { artFade() },
                label = "preview",
            ) { current ->
                when (current) {
                    null -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    is ArtResult.Plain -> ScrollableText {
                        Text(
                            text = current.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            color = if (params.invert) Color(0xFFF2F2F2) else Color(0xFF111111),
                            softWrap = false,
                        )
                    }
                    is ArtResult.Colored -> ScrollableText {
                        Text(
                            text = current.annotated,
                            fontFamily = FontFamily.Monospace,
                            fontSize = params.fontSize.sp,
                            lineHeight = params.fontSize.sp,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollableText(content: @Composable () -> Unit) {
    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            content()
        }
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Glyphsmith", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享 Glyphsmith"))
}

private suspend fun sharePng(context: Context, result: ArtResult, params: Params): Boolean =
    runCatching {
        val bitmap = withContext(Dispatchers.Default) { PngExport.render(result, params) }
        val file = File(context.cacheDir, "Glyphsmith.png")
        try {
            withContext(Dispatchers.IO) {
                file.outputStream().use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        error("PNG 编码失败")
                    }
                }
            }
        } finally {
            bitmap.recycle()
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享 Glyphsmith"))
    }.isSuccess

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val size = info.size
        val longest = max(size.width, size.height)
        if (longest > 1600) {
            val scale = 1600f / longest
            decoder.setTargetSize(
                (size.width * scale).roundToInt().coerceAtLeast(1),
                (size.height * scale).roundToInt().coerceAtLeast(1),
            )
        }
    }
}.getOrNull()

private fun queryName(context: Context, uri: Uri): String {
    var name = "已选图片"
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index) ?: name
            }
        }
    }
    return name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val logoScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "logoScale",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(300),
        label = "logoAlpha",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(360, delayMillis = 80),
        label = "cardAlpha",
    )
    val cardOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 24f,
        animationSpec = tween(360, delayMillis = 80),
        label = "cardOffset",
    )
    val uriHandler = LocalUriHandler.current
    val version = BuildConfig.VERSION_NAME
    BackHandler { onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(16.dp))
            Text("Glyphsmith", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "字匠 · 图片转文字画",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "版本 $version",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = cardAlpha
                        translationY = cardOffset.dp.toPx()
                    },
            ) {
                ListItem(
                    leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    headlineContent = { Text("作者") },
                    supportingContent = { Text("蓝昼lanzhou") },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Rounded.Public, contentDescription = null) },
                    headlineContent = { Text("个人网站") },
                    supportingContent = { Text("lanzhou-1.github.io") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.clickable {
                        runCatching { uriHandler.openUri("https://lanzhou-1.github.io") }
                    },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Rounded.Code, contentDescription = null) },
                    headlineContent = { Text("开源仓库") },
                    supportingContent = { Text("暂空") },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
