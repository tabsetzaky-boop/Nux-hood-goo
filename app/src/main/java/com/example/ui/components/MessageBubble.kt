package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.BubbleShape
import com.example.core.BubbleTone
import com.example.core.MediaUtils
import com.example.core.NuxText
import com.example.data.model.ChatMessage
import com.example.ui.theme.LocalNuxColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleView(
    message: ChatMessage,
    strings: NuxText,
    bubbleTone: BubbleTone,
    bubbleShape: BubbleShape,
    showThinkingSteps: Boolean,
    watermarkEnabled: Boolean,
    isSpeakingThis: Boolean,
    isLastAssistant: Boolean,
    onCopy: (String) -> Unit,
    onRate: (String, String) -> Unit,
    onRegenerate: () -> Unit,
    onEditUserMessage: (String) -> Unit,
    onDeleteMessage: () -> Unit,
    onListenToggle: () -> Unit,
    onFollowUpClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nux = LocalNuxColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var menuOpen by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSelectDialog by remember { mutableStateOf(false) }
    var fullscreenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var copiedJustNow by remember { mutableStateOf(false) }
    var thinkingExpanded by remember { mutableStateOf(false) }

    val isUser = message.role == "user"
    val corner = if (bubbleShape == BubbleShape.PILL) 28.dp else 20.dp

    Column(modifier = modifier.fillMaxWidth()) {
        if (isUser) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    val bubbleBg = when (bubbleTone) {
                        BubbleTone.BLUE -> MaterialTheme.colorScheme.primary
                        BubbleTone.GLASS -> nux.glassSurfaceStrong
                        BubbleTone.MINIMAL -> MaterialTheme.colorScheme.secondary
                    }
                    val bubbleTextColor = when (bubbleTone) {
                        BubbleTone.BLUE -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clip(RoundedCornerShape(corner))
                            .background(bubbleBg)
                            .border(1.dp, nux.glassBorder, RoundedCornerShape(corner))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { menuOpen = true }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (message.imageLocalPaths.isNotEmpty()) {
                            message.imageLocalPaths.forEach { path ->
                                val bmp = remember(path) { MediaUtils.loadLocalBitmap(path) }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Attached image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { fullscreenBitmap = bmp }
                                    )
                                }
                            }
                        }
                        if (message.attachmentNames.isNotEmpty() && message.imageLocalPaths.isEmpty()) {
                            message.attachmentNames.forEach { name ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.AttachFile, null, Modifier.size(14.dp), tint = bubbleTextColor)
                                    Spacer(Modifier.width(4.dp))
                                    Text(name, style = MaterialTheme.typography.labelSmall, color = bubbleTextColor)
                                }
                            }
                        }
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                                color = bubbleTextColor
                            )
                        }
                    }

                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.copy) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)) },
                            onClick = {
                                menuOpen = false
                                clipboard.setText(AnnotatedString(message.content))
                                onCopy(message.content)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.rename) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) },
                            onClick = { menuOpen = false; showEditDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.share) },
                            leadingIcon = { Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)) },
                            onClick = {
                                menuOpen = false
                                MediaUtils.shareText(context, "Nux", message.content)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDeleteMessage() }
                        )
                    }
                }
            }
        } else {
            // Assistant Message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { menuOpen = true }
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Thinking Box
                if (showThinkingSteps && !message.thinking.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                            .border(1.dp, nux.glassBorder, RoundedCornerShape(16.dp))
                            .clickable { thinkingExpanded = !thinkingExpanded }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.thinkingStepsTitle,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (thinkingExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        AnimatedVisibility(visible = thinkingExpanded) {
                            Text(
                                text = message.thinking,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Assistant Header + Body
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
                        NuxAtomLogo(size = 24.dp, animated = false, withGlow = false)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (message.content.isNotBlank()) {
                            NuxMarkdownText(markdown = message.content)
                        }

                        // Generated Images
                        if (message.imageLocalPaths.isNotEmpty()) {
                            message.imageLocalPaths.forEach { path ->
                                val bmp = remember(path) { MediaUtils.loadLocalBitmap(path) }
                                if (bmp != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .border(1.dp, nux.glassBorder, RoundedCornerShape(18.dp))
                                            .clickable { fullscreenBitmap = bmp }
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Generated image",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (watermarkEnabled) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(Color.Black.copy(alpha = 0.55f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                NuxAtomLogo(size = 14.dp, animated = false, withGlow = false)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Nux", color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Sources
                        if (message.sources.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Public, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "${strings.sourcesTitle}: ${message.sources.joinToString(" · ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // AI Disclaimer
                        Text(
                            text = strings.aiDisclaimer,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )

                        // Action Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(message.content))
                                    copiedJustNow = true
                                    onCopy(message.content)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedJustNow) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = strings.copy,
                                    tint = if (copiedJustNow) nux.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onRate("up", "") },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ThumbUp,
                                    contentDescription = strings.helpful,
                                    tint = if (message.feedback == "up") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { showFeedbackDialog = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ThumbDown,
                                    contentDescription = strings.notHelpful,
                                    tint = if (message.feedback == "down") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = onListenToggle,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThis) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                    contentDescription = strings.listen,
                                    tint = if (isSpeakingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isLastAssistant) {
                                IconButton(
                                    onClick = onRegenerate,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = strings.regenerate,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { MediaUtils.shareText(context, "Nux", message.content) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = strings.share,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Smart Follow-ups (on last assistant message)
                        if (isLastAssistant && message.followUpSuggestions.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                message.followUpSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(nux.glassSurface)
                                            .border(1.dp, nux.glassBorder, RoundedCornerShape(999.dp))
                                            .clickable { onFollowUpClick(suggestion) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(strings.copy) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuOpen = false
                            clipboard.setText(AnnotatedString(message.content))
                            onCopy(message.content)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.selectText) },
                        onClick = { menuOpen = false; showSelectDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.share) },
                        leadingIcon = { Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuOpen = false
                            MediaUtils.shareText(context, "Nux", message.content)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDeleteMessage() }
                    )
                }
            }
        }
    }

    // Dislike Feedback Note Dialog
    if (showFeedbackDialog) {
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text(strings.notHelpful) },
            text = {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text(if (strings.isArabic) "أخبرنا ما الذي يمكن تحسينه (اختياري)" else "What could be improved? (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRate("down", note)
                    showFeedbackDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Edit User Message Dialog
    if (showEditDialog) {
        var edited by remember(message.content) { mutableStateOf(message.content) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(strings.rename) },
            text = {
                OutlinedTextField(
                    value = edited,
                    onValueChange = { edited = it.take(8000) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (edited.isNotBlank()) onEditUserMessage(edited)
                    showEditDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Select Text Dialog
    if (showSelectDialog) {
        AlertDialog(
            onDismissRequest = { showSelectDialog = false },
            title = { Text(strings.selectText) },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(message.content, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(message.content))
                    showSelectDialog = false
                }) { Text(strings.copy) }
            },
            dismissButton = {
                TextButton(onClick = { showSelectDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Fullscreen Image Viewer
    fullscreenBitmap?.let { bmp ->
        Dialog(
            onDismissRequest = { fullscreenBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(16.dp)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Fullscreen preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val ok = MediaUtils.saveImageToGallery(context, bmp, "Nux")
                            onCopy(if (ok) "تم حفظ الصورة في المعرض" else "تعذّر حفظ الصورة")
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Save", tint = Color.White)
                    }
                    IconButton(
                        onClick = { fullscreenBitmap = null },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}
