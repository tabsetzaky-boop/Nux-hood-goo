package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CompassCalibration
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.MediaUtils
import com.example.core.NuxText
import com.example.data.model.Conversation
import com.example.data.model.NuxRoutes
import com.example.data.model.Project
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalNuxColors

private data class SidebarNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun NuxSidebar(
    strings: NuxText,
    currentRoute: String,
    activeConversationId: String?,
    profile: UserProfile?,
    conversations: List<Conversation>,
    projects: List<Project>,
    isWideScreen: Boolean,
    onCloseDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onTogglePinConversation: (Conversation) -> Unit,
    onToggleArchiveConversation: (Conversation) -> Unit,
    onToggleLockConversation: (Conversation) -> Unit,
    onMoveConversationToProject: (String, String?) -> Unit,
    onDeleteConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nux = LocalNuxColors.current
    val context = LocalContext.current

    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var moveTarget by remember { mutableStateOf<Conversation?>(null) }

    val navItems = remember(strings) {
        listOf(
            SidebarNavItem(NuxRoutes.IMAGES, strings.images, Icons.Outlined.Image),
            SidebarNavItem(NuxRoutes.LIBRARY, strings.library, Icons.Outlined.LibraryBooks),
            SidebarNavItem(NuxRoutes.PROJECTS, strings.projects, Icons.Outlined.Folder),
            SidebarNavItem(NuxRoutes.FILES, strings.files, Icons.Outlined.Description),
            SidebarNavItem(NuxRoutes.CUSTOMIZE, strings.customize, Icons.Outlined.Tune),
            SidebarNavItem(NuxRoutes.EXPLORE, strings.explore, Icons.Rounded.Explore),
            SidebarNavItem(NuxRoutes.REMOTE, strings.remoteControl, Icons.Outlined.Monitor),
        )
    }

    val visibleChats = remember(conversations) {
        conversations.filter { !it.archived && !it.locked }
    }
    val pinnedChats = remember(visibleChats) {
        visibleChats.filter { it.pinned }.take(4)
    }
    val regularChats = remember(visibleChats) {
        visibleChats.filter { !it.pinned }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(nux.sidebarPanel)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Top Row: Nux Logo + Close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onNewChat()
                        if (!isWideScreen) onCloseDrawer()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NuxAtomLogo(size = 28.dp, animated = true, withGlow = true)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Nux",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onCloseDrawer,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("sidebar_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Action Row: New Chat + Search Square
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                    .border(1.dp, nux.glassBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        onNewChat()
                        if (!isWideScreen) onCloseDrawer()
                    }
                    .testTag("sidebar_new_chat_button")
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddComment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.newChat,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                    .border(1.dp, nux.glassBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        onNavigate(NuxRoutes.SEARCH)
                        if (!isWideScreen) onCloseDrawer()
                    }
                    .testTag("sidebar_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = strings.search,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 3. Scrollable Nav + Chat History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(navItems, key = { it.route }) { item ->
                val active = currentRoute == item.route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable {
                            onNavigate(item.route)
                            if (!isWideScreen) onCloseDrawer()
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Divider: Chat History
            item(key = "divider_history") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.chatHistory,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }
            }

            // Pinned Chats
            if (pinnedChats.isNotEmpty()) {
                item(key = "hdr_pinned") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.pinnedChats,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.all,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onNavigate(NuxRoutes.PINNED)
                                if (!isWideScreen) onCloseDrawer()
                            }
                        )
                    }
                }
                items(pinnedChats, key = { "pin_${it.id}" }) { conv ->
                    SidebarChatRow(
                        conv = conv,
                        isSelected = conv.id == activeConversationId,
                        strings = strings,
                        onOpen = {
                            onOpenConversation(conv.id)
                            if (!isWideScreen) onCloseDrawer()
                        },
                        onShare = { MediaUtils.shareText(context, conv.title, conv.title) },
                        onPin = { onTogglePinConversation(conv) },
                        onLock = { onToggleLockConversation(conv) },
                        onArchive = { onToggleArchiveConversation(conv) },
                        onMoveToProject = { moveTarget = conv },
                        onRename = { renameTarget = conv },
                        onDelete = { deleteTarget = conv }
                    )
                }
            }

            // Projects Section
            if (projects.isNotEmpty()) {
                item(key = "hdr_projects") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.projects,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = {
                                onNavigate(NuxRoutes.PROJECTS)
                                if (!isWideScreen) onCloseDrawer()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = strings.projects,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                items(projects.take(5), key = { "proj_${it.id}" }) { proj ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onNavigate(NuxRoutes.PROJECTS)
                                if (!isWideScreen) onCloseDrawer()
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = proj.iconEmoji, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = proj.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (proj.pinned) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // All Chats
            if (regularChats.isNotEmpty()) {
                item(key = "hdr_all") {
                    Text(
                        text = strings.allChats,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 4.dp)
                    )
                }
                items(regularChats, key = { it.id }) { conv ->
                    SidebarChatRow(
                        conv = conv,
                        isSelected = conv.id == activeConversationId,
                        strings = strings,
                        onOpen = {
                            onOpenConversation(conv.id)
                            if (!isWideScreen) onCloseDrawer()
                        },
                        onShare = { MediaUtils.shareText(context, conv.title, conv.title) },
                        onPin = { onTogglePinConversation(conv) },
                        onLock = { onToggleLockConversation(conv) },
                        onArchive = { onToggleArchiveConversation(conv) },
                        onMoveToProject = { moveTarget = conv },
                        onRename = { renameTarget = conv },
                        onDelete = { deleteTarget = conv }
                    )
                }
            } else if (pinnedChats.isEmpty()) {
                item(key = "empty_history") {
                    Text(
                        text = if (strings.isArabic) "ابدأ محادثتك الأولى مع نوكس الآن." else "Start your first chat with Nux now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                    )
                }
            }
        }

        // 4. Bottom Fixed Capsule: Account -> /settings + Circular New Chat
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayName = profile?.preferredName?.takeIf { it.isNotBlank() }
                ?: profile?.displayName?.takeIf { it.isNotBlank() }
                ?: strings.settings

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(nux.capsuleGradient)
                    .clickable {
                        onNavigate(NuxRoutes.SETTINGS)
                        if (!isWideScreen) onCloseDrawer()
                    }
                    .testTag("sidebar_account_capsule")
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.photoUrl,
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "N",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .border(1.dp, nux.glassBorder, CircleShape)
                    .clickable {
                        onNewChat()
                        if (!isWideScreen) onCloseDrawer()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddComment,
                    contentDescription = strings.newChat,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Rename Dialog
    renameTarget?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(strings.rename) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(160) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) onRenameConversation(target.id, text)
                    renameTarget = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(strings.cancel) }
            }
        )
    }

    // Delete Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.delete) },
            text = { Text("هل تريد حذف «${target.title}» نهائيًا؟") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConversation(target.id)
                    deleteTarget = null
                }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            }
        )
    }

    // Move to Project Dialog
    moveTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text(strings.moveToProject) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (projects.isEmpty()) {
                        Text("لا توجد مشاريع بعد. أنشئ مشروعًا من قسم المشاريع.")
                    } else {
                        projects.forEach { proj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onMoveConversationToProject(target.id, proj.id)
                                        moveTarget = null
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(proj.iconEmoji, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(proj.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { moveTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarChatRow(
    conv: Conversation,
    isSelected: Boolean,
    strings: NuxText,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onPin: () -> Unit,
    onLock: () -> Unit,
    onArchive: () -> Unit,
    onMoveToProject: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.secondary
                    else Color.Transparent
                )
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = { menuOpen = true }
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conv.pinned) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = conv.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.share) },
                leadingIcon = { Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onShare() }
            )
            DropdownMenuItem(
                text = { Text(if (conv.pinned) strings.unpin else strings.pin) },
                leadingIcon = { Icon(Icons.Rounded.PushPin, null, Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onPin() }
            )
            DropdownMenuItem(
                text = { Text(strings.rename) },
                leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text(strings.moveToProject) },
                leadingIcon = { Icon(Icons.Rounded.DriveFileMove, null, Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onMoveToProject() }
            )
            DropdownMenuItem(
                text = { Text(strings.lockChat) },
                leadingIcon = { Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onLock() }
            )
            DropdownMenuItem(
                text = { Text(strings.archive) },
                leadingIcon = { Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp)) },
                onClick = { menuOpen = false; onArchive() }
            )
            DropdownMenuItem(
                text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                onClick = { menuOpen = false; onDelete() }
            )
        }
    }
}
