package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.core.MediaUtils
import com.example.core.NuxText
import com.example.data.model.AspectRatioOption
import com.example.data.model.AttachmentItem
import com.example.data.model.ComposerMode
import com.example.data.model.ImageStyleOption
import com.example.data.model.MentionTool
import com.example.data.model.NuxCatalog
import com.example.ui.theme.LocalNuxColors

@Composable
fun NuxComposer(
    strings: NuxText,
    text: String,
    mode: ComposerMode,
    imageStyle: ImageStyleOption,
    aspectRatio: AspectRatioOption,
    attachments: List<AttachmentItem>,
    replyImage: AttachmentItem?,
    activeTool: MentionTool?,
    webSearchActive: Boolean,
    deepThinkingActive: Boolean,
    isStreaming: Boolean,
    activeProjectName: String?,
    onTextChange: (String) -> Unit,
    onModeChange: (ComposerMode) -> Unit,
    onImageStyleChange: (ImageStyleOption) -> Unit,
    onAspectRatioChange: (AspectRatioOption) -> Unit,
    onToggleWebSearch: () -> Unit,
    onToggleDeepThinking: () -> Unit,
    onSelectTool: (MentionTool?) -> Unit,
    onNavigateRoute: (String) -> Unit,
    onAddUris: (List<android.net.Uri>, Boolean) -> Unit,
    onCapturedCamera: (String, android.net.Uri) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onEditImageAttachment: (AttachmentItem) -> Unit,
    onClearReplyImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenLiveVoice: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val nux = LocalNuxColors.current

    var isFocused by remember { mutableStateOf(false) }
    var plusMenuOpen by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isDictating by remember { mutableStateOf(false) }

    // Camera capture launcher
    var pendingCamFile by remember { mutableStateOf<Pair<android.net.Uri, String>?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val info = pendingCamFile
        if (success && info != null) {
            onCapturedCamera(info.second, info.first)
        }
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val pair = MediaUtils.createTempCameraUri(context)
            pendingCamFile = pair
            takePictureLauncher.launch(pair.first)
        } else {
            onShowMessage(if (strings.isArabic) "يرجى السماح بإذن الكاميرا لالتقاط الصور" else "Camera permission is required")
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        if (uris.isNotEmpty()) onAddUris(uris, true)
    }

    // Document / Audio Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onAddUris(uris, false)
    }

    // Dictation SpeechRecognizer
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }
    DisposableEffect(speechRecognizer) {
        onDispose { speechRecognizer?.destroy() }
    }

    fun startDictation() {
        if (speechRecognizer == null) {
            onShowMessage(if (strings.isArabic) "التعرّف الصوتي غير متاح على هذا الجهاز" else "Speech recognition unavailable")
            return
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isDictating = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isDictating = false }
            override fun onError(error: Int) { isDictating = false }
            override fun onResults(results: Bundle?) {
                isDictating = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) {
                    val combined = if (text.isBlank()) spoken else "$text $spoken"
                    onTextChange(combined)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (strings.isArabic) "ar-SA" else "en-US")
        }
        speechRecognizer.startListening(intent)
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startDictation()
        else onShowMessage(if (strings.isArabic) "يرجى السماح بإذن الميكروفون للإملاء" else "Microphone permission required")
    }

    val showMentionMenu = remember(text) {
        val lastAt = text.lastIndexOf('@')
        lastAt >= 0 && !text.substring(lastAt).contains(' ') && !text.substring(lastAt).contains('\n')
    }
    val mentionFilter = remember(text) {
        if (!showMentionMenu) "" else text.substringAfterLast('@').lowercase()
    }
    val filteredTools = remember(mentionFilter) {
        if (mentionFilter.isBlank()) NuxCatalog.mentionTools
        else NuxCatalog.mentionTools.filter {
            it.arName.contains(mentionFilter) || it.enName.lowercase().contains(mentionFilter)
        }
    }

    val isExpanded = isFocused ||
        text.isNotEmpty() ||
        attachments.isNotEmpty() ||
        replyImage != null ||
        mode != ComposerMode.CHAT ||
        activeTool != null ||
        webSearchActive ||
        deepThinkingActive ||
        isStreaming

    val cornerDp = if (isExpanded) 26.dp else 999.dp

    @Composable
    fun ComposerCore(inFullScreen: Boolean) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (inFullScreen) Modifier.fillMaxSize() else Modifier)
        ) {
            // Project badge above composer
            if (!activeProjectName.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📁 $activeProjectName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Reply to Image bar
            if (replyImage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    replyImage.bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (strings.isArabic) "تعديل هذه الصورة بوصف جديد" else "Edit this image with a new prompt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearReplyImage, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Rounded.Close, null, Modifier.size(16.dp))
                    }
                }
            }

            // @ Mention Popup
            AnimatedVisibility(visible = showMentionMenu && filteredTools.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .heightIn(max = 220.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = nux.glassSurfaceStrong,
                    border = androidx.compose.foundation.BorderStroke(1.dp, nux.glassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        filteredTools.take(6).forEach { tool ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val cleaned = text.substringBeforeLast('@').trimEnd()
                                        onTextChange(cleaned)
                                        if (tool.isComingSoon) {
                                            onShowMessage(if (strings.isArabic) "إنشاء الفيديو سيتوفّر قريبًا جدًا" else "Video generation is coming soon")
                                        } else if (tool.opensRoute != null) {
                                            onNavigateRoute(tool.opensRoute)
                                        } else {
                                            onSelectTool(tool)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${if (strings.isArabic) tool.arName else tool.enName}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (strings.isArabic) tool.arDescription else tool.enDescription,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (tool.isComingSoon) {
                                    Text(
                                        text = strings.comingSoon,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Morphing Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (inFullScreen) Modifier.weight(1f) else Modifier)
                    .animateContentSize(),
                shape = RoundedCornerShape(cornerDp),
                color = nux.glassSurfaceStrong,
                border = androidx.compose.foundation.BorderStroke(1.dp, nux.glassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = if (isExpanded) 10.dp else 6.dp)
                ) {
                    // Status Badges Row
                    val hasBadges = mode != ComposerMode.CHAT || activeTool != null || webSearchActive || deepThinkingActive
                    if (hasBadges) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (mode != ComposerMode.CHAT) {
                                ComposerStatusChip(
                                    label = if (strings.isArabic) mode.arLabel else mode.enLabel,
                                    onClear = { onModeChange(ComposerMode.CHAT) }
                                )
                            }
                            if (activeTool != null) {
                                ComposerStatusChip(
                                    label = "@${if (strings.isArabic) activeTool.arName else activeTool.enName}",
                                    onClear = { onSelectTool(null) }
                                )
                            }
                            if (webSearchActive) {
                                ComposerStatusChip(
                                    label = strings.webSearchChip,
                                    icon = Icons.Rounded.Public,
                                    onClear = onToggleWebSearch
                                )
                            }
                            if (deepThinkingActive) {
                                ComposerStatusChip(
                                    label = strings.deepThinkingChip,
                                    icon = Icons.Rounded.Psychology,
                                    onClear = onToggleDeepThinking
                                )
                            }
                        }
                    }

                    // Image Mode Options (Styles & Aspect Ratio)
                    if (mode == ComposerMode.IMAGE) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ImageStyleOption.entries.forEach { st ->
                                val selected = st == imageStyle
                                Text(
                                    text = if (strings.isArabic) st.arLabel else st.enLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondary
                                        )
                                        .clickable { onImageStyleChange(st) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            AspectRatioOption.entries.forEach { ar ->
                                val selected = ar == aspectRatio
                                Text(
                                    text = ar.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                            else MaterialTheme.colorScheme.secondary
                                        )
                                        .clickable { onAspectRatioChange(ar) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Attachment Chips
                    if (attachments.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            attachments.forEach { att ->
                                Box(
                                    modifier = Modifier
                                        .width(104.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.secondary)
                                        .border(1.dp, nux.glassBorder, RoundedCornerShape(14.dp))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (att.bitmap != null) {
                                                Image(
                                                    bitmap = att.bitmap.asImageBitmap(),
                                                    contentDescription = att.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (att.isAudio) Icons.Rounded.GraphicEq else Icons.Rounded.AttachFile,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = att.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = MediaUtils.formatBytes(att.sizeBytes),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.align(Alignment.TopEnd),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (att.isImage && att.bitmap != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .clickable { onEditImageAttachment(att) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.AutoFixHigh,
                                                    contentDescription = "Edit image",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .clickable { onRemoveAttachment(att.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input & Controls
                    if (!isExpanded) {
                        // Single-line Pill State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlusButtonWithMenu(
                                strings = strings,
                                plusMenuOpen = plusMenuOpen,
                                onToggleMenu = { plusMenuOpen = it },
                                webSearchActive = webSearchActive,
                                deepThinkingActive = deepThinkingActive,
                                onCamera = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        val pair = MediaUtils.createTempCameraUri(context)
                                        pendingCamFile = pair
                                        takePictureLauncher.launch(pair.first)
                                    } else {
                                        cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                onGallery = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onFiles = {
                                    filePickerLauncher.launch(arrayOf("application/pdf", "text/*", "application/json", "audio/*"))
                                },
                                onInsertAt = { onTextChange(if (text.isBlank()) "@" else "$text @") },
                                onToggleDeepThinking = onToggleDeepThinking,
                                onImageMode = { onModeChange(ComposerMode.IMAGE) },
                                onAudioMode = { onModeChange(ComposerMode.AUDIO) },
                                onToggleWebSearch = onToggleWebSearch
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (text.isEmpty()) {
                                    Text(
                                        text = strings.askNux,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BasicTextField(
                                    value = text,
                                    onValueChange = onTextChange,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("composer_input")
                                        .onFocusChanged { isFocused = it.isFocused }
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        startDictation()
                                    } else {
                                        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("composer_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Voice dictation",
                                    tint = if (isDictating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            UnifiedSendOrVoiceButton(
                                hasPayload = text.isNotBlank() || attachments.isNotEmpty() || replyImage != null,
                                isStreaming = isStreaming,
                                onSend = onSend,
                                onStop = onStop,
                                onVoiceOrb = onOpenLiveVoice
                            )
                        }
                    } else {
                        // Expanded Panel State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (inFullScreen) Modifier.weight(1f) else Modifier.heightIn(min = 64.dp, max = 220.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = when (mode) {
                                        ComposerMode.IMAGE -> if (strings.isArabic) "صف الصورة التي تريد إنشاءها..." else "Describe the image to generate..."
                                        ComposerMode.AUDIO -> if (strings.isArabic) "اكتب ما تريد نوكس أن يجيب به صوتيًا..." else "Write what Nux should answer aloud..."
                                        ComposerMode.CHAT -> strings.askNux
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = text,
                                onValueChange = onTextChange,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.5.sp,
                                    lineHeight = 26.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("composer_input_expanded")
                                    .onFocusChanged { isFocused = it.isFocused }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlusButtonWithMenu(
                                    strings = strings,
                                    plusMenuOpen = plusMenuOpen,
                                    onToggleMenu = { plusMenuOpen = it },
                                    webSearchActive = webSearchActive,
                                    deepThinkingActive = deepThinkingActive,
                                    onCamera = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            val pair = MediaUtils.createTempCameraUri(context)
                                            pendingCamFile = pair
                                            takePictureLauncher.launch(pair.first)
                                        } else {
                                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    onGallery = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onFiles = {
                                        filePickerLauncher.launch(arrayOf("application/pdf", "text/*", "application/json", "audio/*"))
                                    },
                                    onInsertAt = { onTextChange(if (text.isBlank()) "@" else "$text @") },
                                    onToggleDeepThinking = onToggleDeepThinking,
                                    onImageMode = { onModeChange(ComposerMode.IMAGE) },
                                    onAudioMode = { onModeChange(ComposerMode.AUDIO) },
                                    onToggleWebSearch = onToggleWebSearch
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (text.length >= 120 || inFullScreen) {
                                    IconButton(
                                        onClick = { isFullScreen = !isFullScreen },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (inFullScreen) Icons.Rounded.CloseFullscreen else Icons.Rounded.OpenInFull,
                                            contentDescription = "Toggle fullscreen",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            startDictation()
                                        } else {
                                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = "Voice dictation",
                                        tint = if (isDictating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                UnifiedSendOrVoiceButton(
                                    hasPayload = text.isNotBlank() || attachments.isNotEmpty() || replyImage != null,
                                    isStreaming = isStreaming,
                                    onSend = {
                                        isFullScreen = false
                                        onSend()
                                    },
                                    onStop = onStop,
                                    onVoiceOrb = onOpenLiveVoice
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(14.dp)
            ) {
                ComposerCore(inFullScreen = true)
            }
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            ComposerCore(inFullScreen = false)
        }
    }
}

@Composable
private fun PlusButtonWithMenu(
    strings: NuxText,
    plusMenuOpen: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    webSearchActive: Boolean,
    deepThinkingActive: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFiles: () -> Unit,
    onInsertAt: () -> Unit,
    onToggleDeepThinking: () -> Unit,
    onImageMode: () -> Unit,
    onAudioMode: () -> Unit,
    onToggleWebSearch: () -> Unit
) {
    Box {
        IconButton(
            onClick = { onToggleMenu(true) },
            modifier = Modifier
                .size(38.dp)
                .testTag("composer_plus_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Tools and attachments",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        DropdownMenu(
            expanded = plusMenuOpen,
            onDismissRequest = { onToggleMenu(false) },
            modifier = Modifier.width(248.dp)
        ) {
            DropdownMenuItem(
                text = { Text(if (strings.isArabic) "التقاط صورة بالكاميرا" else "Take photo") },
                leadingIcon = { Icon(Icons.Rounded.CameraAlt, null, Modifier.size(18.dp)) },
                onClick = { onToggleMenu(false); onCamera() }
            )
            DropdownMenuItem(
                text = { Text(if (strings.isArabic) "صور من المعرض" else "Photos from gallery") },
                leadingIcon = { Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)) },
                onClick = { onToggleMenu(false); onGallery() }
            )
            DropdownMenuItem(
                text = { Text(if (strings.isArabic) "إضافة ملفات" else "Add files") },
                leadingIcon = { Icon(Icons.Rounded.AttachFile, null, Modifier.size(18.dp)) },
                onClick = { onToggleMenu(false); onFiles() }
            )
            DropdownMenuItem(
                text = { Text(if (strings.isArabic) "المكوّنات الإضافية @" else "Plugins @") },
                leadingIcon = { Icon(Icons.Rounded.AlternateEmail, null, Modifier.size(18.dp)) },
                onClick = { onToggleMenu(false); onInsertAt() }
            )
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (strings.isArabic) "فكّر بعمق أكبر" else "Think deeper")
                        if (deepThinkingActive) Text("✓", color = MaterialTheme.colorScheme.primary)
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        null,
                        Modifier.size(18.dp),
                        tint = if (deepThinkingActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { onToggleMenu(false); onToggleDeepThinking() }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (strings.isArabic) "إنشاء صورة" else "Create image") },
                leadingIcon = { Icon(Icons.Rounded.AutoFixHigh, null, Modifier.size(18.dp)) },
                onClick = { onToggleMenu(false); onImageMode() }
            )
            DropdownMenuItem(
                text = { Text(if (strings.isArabic) "إنشاء ملف صوتي" else "Create audio reply") },
                leadingIcon = { Icon(Icons.Rounded.GraphicEq, null, Modifier.size(18.dp)) },
                onClick = { onToggleMenu(false); onAudioMode() }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (strings.isArabic) "البحث في الويب" else "Web search")
                        if (webSearchActive) Text("✓", color = MaterialTheme.colorScheme.primary)
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Public,
                        null,
                        Modifier.size(18.dp),
                        tint = if (webSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { onToggleMenu(false); onToggleWebSearch() }
            )
        }
    }
}

@Composable
private fun UnifiedSendOrVoiceButton(
    hasPayload: Boolean,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoiceOrb: () -> Unit
) {
    val nux = LocalNuxColors.current
    when {
        isStreaming -> {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, nux.glassBorder, CircleShape)
                    .clickable(onClick = onStop)
                    .testTag("composer_stop_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "Stop",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        hasPayload -> {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(nux.brandGradient)
                    .clickable(onClick = onSend)
                    .testTag("composer_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(nux.capsuleGradient)
                    .clickable(onClick = onVoiceOrb)
                    .testTag("composer_live_voice_button"),
                contentAlignment = Alignment.Center
            ) {
                VoiceBarsIcon(isAnimating = false, color = Color.White)
            }
        }
    }
}

@Composable
private fun ComposerStatusChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .clickable(onClick = onClear)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
    }
}
