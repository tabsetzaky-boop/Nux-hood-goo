package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.MediaUtils
import com.example.core.NuxText
import com.example.data.model.NuxCatalog
import com.example.data.model.NuxRoutes
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.ChatBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveVoiceDialog
import com.example.ui.components.MessageBubbleView
import com.example.ui.components.NuxAtomLogo
import com.example.ui.components.NuxComposer
import com.example.ui.components.NuxMarkdownText
import com.example.ui.components.PinPadDialog
import com.example.ui.components.TypingDotsIndicator
import com.example.ui.theme.LocalNuxColors
import com.example.viewmodel.NuxViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    isWideScreen: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val nux = LocalNuxColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val activeProjectId by viewModel.activeProjectId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val isTempChat by viewModel.isTempChat.collectAsStateWithLifecycle()
    val tempMessages by viewModel.tempMessages.collectAsStateWithLifecycle()
    val unlockedChatIds by viewModel.unlockedChatIds.collectAsStateWithLifecycle()

    val composerText by viewModel.composerText.collectAsStateWithLifecycle()
    val composerMode by viewModel.composerMode.collectAsStateWithLifecycle()
    val imageStyle by viewModel.imageStyle.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.aspectRatio.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val activeMentionTool by viewModel.activeMentionTool.collectAsStateWithLifecycle()
    val webSearchActive by viewModel.webSearchActive.collectAsStateWithLifecycle()
    val deepThinkingActive by viewModel.deepThinkingActive.collectAsStateWithLifecycle()
    val replyImage by viewModel.replyImageAttachment.collectAsStateWithLifecycle()

    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingThinking by viewModel.streamingThinking.collectAsStateWithLifecycle()
    val streamingAnswer by viewModel.streamingAnswer.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val lastFailedImagePrompt by viewModel.lastFailedImagePrompt.collectAsStateWithLifecycle()
    val speakingMessageId by viewModel.speakingMessageId.collectAsStateWithLifecycle()

    var showModelMenu by remember { mutableStateOf(false) }
    var showChatMenu by remember { mutableStateOf(false) }
    var showLiveVoice by remember { mutableStateOf(false) }
    var showChatPinPad by remember { mutableStateOf(false) }
    var chatPinError by remember { mutableStateOf<String?>(null) }

    val profile = (profileState as? UiState.Success)?.data
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val activeConv = conversations.firstOrNull { it.id == activeConvId }
    val activeProject = projects.firstOrNull { it.id == (activeConv?.projectId ?: activeProjectId) }

    val messages = if (isTempChat) {
        tempMessages
    } else {
        (activeMessagesState as? UiState.Success)?.data.orEmpty()
    }

    val isLockedAndHidden = activeConv != null && activeConv.locked && activeConv.id !in unlockedChatIds
    val listState = rememberLazyListState()
    val showScrollDown by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 3 && lastVisible < total - 2
        }
    }

    LaunchedEffect(messages.size, streamingAnswer) {
        if (messages.isNotEmpty() || streamingAnswer.isNotBlank()) {
            val lastIndex = messages.size + (if (isStreaming) 1 else 0) - 1
            if (lastIndex >= 0) {
                listState.animateScrollToItem(lastIndex.coerceAtLeast(0))
            }
        }
    }

    val selectedModel = NuxCatalog.models.firstOrNull { it.id == prefs.selectedModelId } ?: NuxCatalog.models.first()
    val preferredName = profile?.preferredName?.takeIf { it.isNotBlank() }
        ?: profile?.displayName?.substringBefore(" ")
        ?: ""

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Floating Top Bar (52.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isWideScreen) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(nux.glassSurface)
                                .border(1.dp, nux.glassBorder, CircleShape)
                                .testTag("chat_drawer_button")
                        ) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Menu", modifier = Modifier.size(19.dp))
                        }
                    }

                    // Model Picker Pill
                    Box {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(nux.glassSurface)
                                .border(1.dp, nux.glassBorder, RoundedCornerShape(999.dp))
                                .clickable { showModelMenu = true }
                                .testTag("model_picker_button")
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedModel.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Select model",
                                modifier = Modifier.size(17.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            NuxCatalog.models.forEach { m ->
                                val active = m.id == selectedModel.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = m.title,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (strings.isArabic) m.arSubtitle else m.enSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSelectedModel(m.id)
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Temporary Chat Toggle
                    IconButton(
                        onClick = { viewModel.toggleTempChat() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTempChat) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else nux.glassSurface
                            )
                            .border(
                                1.dp,
                                if (isTempChat) MaterialTheme.colorScheme.primary else nux.glassBorder,
                                CircleShape
                            )
                            .testTag("temp_chat_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = strings.tempChatLabel,
                            tint = if (isTempChat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Right Chat Capsule (Avatar -> Profile + Menu)
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(nux.glassSurface)
                        .border(1.dp, nux.glassBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(nux.capsuleGradient)
                            .clickable { onNavigateRoute(NuxRoutes.PROFILE) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.photoUrl,
                                contentDescription = strings.profile,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = (preferredName.firstOrNull() ?: 'N').uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showChatMenu = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "Chat options",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showChatMenu,
                            onDismissRequest = { showChatMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.newChat) },
                                leadingIcon = { Icon(Icons.Rounded.AddComment, null, Modifier.size(18.dp)) },
                                onClick = {
                                    showChatMenu = false
                                    viewModel.startNewChat()
                                }
                            )
                            if (activeConv != null) {
                                DropdownMenuItem(
                                    text = { Text(if (activeConv.pinned) strings.unpin else strings.pin) },
                                    leadingIcon = { Icon(Icons.Rounded.PushPin, null, Modifier.size(18.dp)) },
                                    onClick = {
                                        showChatMenu = false
                                        viewModel.togglePinConversation(activeConv)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (activeConv.locked) strings.unlockChat else strings.lockChat) },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp)) },
                                    onClick = {
                                        showChatMenu = false
                                        viewModel.toggleLockConversation(activeConv)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (activeConv.archived) strings.unarchive else strings.archive) },
                                    leadingIcon = { Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp)) },
                                    onClick = {
                                        showChatMenu = false
                                        viewModel.toggleArchiveConversation(activeConv)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.share) },
                                    leadingIcon = { Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)) },
                                    onClick = {
                                        showChatMenu = false
                                        val body = messages.joinToString("\n\n") { "${it.role}: ${it.content}" }
                                        MediaUtils.shareText(context, activeConv.title, body.ifBlank { activeConv.title })
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showChatMenu = false
                                        viewModel.deleteConversation(activeConv.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Main Body Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLockedAndHidden -> {
                        GlassCard(
                            modifier = Modifier
                                .padding(24.dp)
                                .widthIn(max = 420.dp),
                            cornerRadius = 24.dp,
                            strong = true
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (strings.isArabic) "هذه الدردشة مقفلة" else "This chat is locked",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (strings.isArabic) {
                                        "أدخل رمز قفل الدردشات المكون من 6 أرقام لعرض الرسائل."
                                    } else {
                                        "Enter your 6-digit Chat Lock PIN to view this conversation."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                BrandGradientButton(
                                    text = strings.unlockChat,
                                    onClick = { showChatPinPad = true }
                                )
                            }
                        }
                    }

                    messages.isEmpty() && !isStreaming -> {
                        // Home Empty State
                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isTempChat) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Timer, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.tempChatLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            NuxAtomLogo(size = 76.dp, animated = true, withGlow = true)

                            Text(
                                text = if (preferredName.isNotBlank()) "${strings.homeGreetingPrefix} $preferredName" else "${strings.homeGreetingPrefix} بك في Nux",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.homeSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(messages, key = { idx, m -> m.id.ifBlank { "msg_$idx" } }) { idx, msg ->
                                val isLastAsst = msg.role == "assistant" &&
                                    idx == messages.indexOfLast { it.role == "assistant" } && !isStreaming

                                MessageBubbleView(
                                    message = msg,
                                    strings = strings,
                                    bubbleTone = prefs.bubbleTone,
                                    bubbleShape = prefs.bubbleShape,
                                    showThinkingSteps = prefs.showThinkingSteps,
                                    watermarkEnabled = prefs.watermarkEnabled,
                                    isSpeakingThis = speakingMessageId == msg.id,
                                    isLastAssistant = isLastAsst,
                                    onCopy = { viewModel.showToast(strings.copied) },
                                    onRate = { rating, note -> viewModel.rateMessage(msg, rating, note) },
                                    onRegenerate = { viewModel.regenerateLastReply() },
                                    onEditUserMessage = { edited -> viewModel.editUserMessage(msg, edited) },
                                    onDeleteMessage = { viewModel.deleteMessage(msg) },
                                    onListenToggle = {
                                        if (speakingMessageId == msg.id) viewModel.stopSpeaking()
                                        else viewModel.speakText(msg.id, msg.content)
                                    },
                                    onFollowUpClick = { prompt -> viewModel.sendCurrentMessage(prompt) }
                                )
                            }

                            if (isStreaming) {
                                item(key = "streaming_bubble") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(30.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            NuxAtomLogo(size = 24.dp, animated = true, withGlow = true)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (streamingThinking.isNotBlank() && prefs.showThinkingSteps) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f))
                                                        .border(1.dp, nux.glassBorder, RoundedCornerShape(14.dp))
                                                        .padding(10.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = strings.thinkingStepsTitle,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = streamingThinking,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (streamingAnswer.isNotBlank()) {
                                                NuxMarkdownText(markdown = streamingAnswer)
                                            }
                                            TypingDotsIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Scroll to Bottom FAB
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollDown,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                val target = (messages.size - 1).coerceAtLeast(0)
                                listState.animateScrollToItem(target)
                            }
                        },
                        shape = CircleShape,
                        containerColor = nux.glassSurfaceStrong
                    ) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Scroll down")
                    }
                }
            }

            // Error Banner above Composer
            if (!chatError.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = chatError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!lastFailedImagePrompt.isNullOrBlank()) {
                        TextButton(onClick = { viewModel.sendCurrentMessage(lastFailedImagePrompt) }) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.retry)
                        }
                    }
                }
            }

            // Anchored Morphing Composer
            if (!isLockedAndHidden) {
                NuxComposer(
                    strings = strings,
                    text = composerText,
                    mode = composerMode,
                    imageStyle = imageStyle,
                    aspectRatio = aspectRatio,
                    attachments = attachments,
                    replyImage = replyImage,
                    activeTool = activeMentionTool,
                    webSearchActive = webSearchActive,
                    deepThinkingActive = deepThinkingActive,
                    isStreaming = isStreaming,
                    activeProjectName = activeProject?.name,
                    onTextChange = { viewModel.updateComposerText(it) },
                    onModeChange = { viewModel.setComposerMode(it) },
                    onImageStyleChange = { viewModel.setImageStyle(it) },
                    onAspectRatioChange = { viewModel.setAspectRatio(it) },
                    onToggleWebSearch = { viewModel.toggleWebSearch() },
                    onToggleDeepThinking = { viewModel.toggleDeepThinking() },
                    onSelectTool = { viewModel.selectMentionTool(it) },
                    onNavigateRoute = onNavigateRoute,
                    onAddUris = { uris, isImg -> viewModel.addAttachmentsFromUris(uris, isImg) },
                    onCapturedCamera = { path, uri -> viewModel.addCapturedCameraImage(path, uri) },
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    onEditImageAttachment = { viewModel.setReplyToImage(it) },
                    onClearReplyImage = { viewModel.setReplyToImage(null) },
                    onSend = { viewModel.sendCurrentMessage() },
                    onStop = { viewModel.stopGeneration() },
                    onOpenLiveVoice = { showLiveVoice = true },
                    onShowMessage = { viewModel.showToast(it, isError = true) },
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (showLiveVoice) {
        LiveVoiceDialog(
            strings = strings,
            selectedVoice = prefs.liveVoice,
            isSpeaking = speakingMessageId != null,
            onSelectVoice = { viewModel.setLiveVoice(it) },
            onSpeakReply = { reply -> viewModel.speakText("live_${System.currentTimeMillis()}", reply) },
            onStopSpeaking = { viewModel.stopSpeaking() },
            onProcessVoiceTurn = { spoken -> viewModel.runLiveVoiceTurn(spoken) },
            onDismiss = { showLiveVoice = false }
        )
    }

    if (showChatPinPad && activeConv != null) {
        PinPadDialog(
            title = if (strings.isArabic) "فتح الدردشة المقفلة" else "Unlock Locked Chat",
            subtitle = activeConv.title,
            confirmMode = false,
            externalError = chatPinError,
            onDismiss = { showChatPinPad = false },
            onPinCompleted = { pin ->
                if (viewModel.verifyPin("chats", pin)) {
                    chatPinError = null
                    showChatPinPad = false
                    viewModel.markChatUnlocked(activeConv.id)
                } else {
                    chatPinError = if (strings.isArabic) "الرمز غير صحيح" else "Incorrect PIN"
                }
            }
        )
    }
}
