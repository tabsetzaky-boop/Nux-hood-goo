package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.MediaUtils
import com.example.core.NuxText
import com.example.core.normalizeArabic
import com.example.data.model.AspectRatioOption
import com.example.data.model.ImageStyleOption
import com.example.data.model.NuxRoutes
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.ChatBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalNuxColors
import com.example.viewmodel.NuxViewModel
import com.example.viewmodel.UiState

@Composable
fun NuxScreenTopBar(
    title: String,
    strings: NuxText,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val nux = LocalNuxColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(nux.glassSurface)
                    .border(1.dp, nux.glassBorder, CircleShape)
                    .testTag("screen_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.backToChat,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SearchScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val nux = LocalNuxColors.current
    var query by remember { mutableStateOf("") }
    val conversations = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val projects = (viewModel.projectsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val images = (viewModel.generatedImagesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()

    val normalizedQ = remember(query) { normalizeArabic(query) }
    val appSections = remember(strings) {
        listOf(
            NuxRoutes.IMAGES to strings.images,
            NuxRoutes.LIBRARY to strings.library,
            NuxRoutes.PROJECTS to strings.projects,
            NuxRoutes.FILES to strings.files,
            NuxRoutes.CUSTOMIZE to strings.customize,
            NuxRoutes.EXPLORE to strings.explore,
            NuxRoutes.MEMORY to strings.memory,
            NuxRoutes.SECURITY to strings.security,
            NuxRoutes.SETTINGS to strings.settings,
            NuxRoutes.BUILDER to strings.siteBuilder,
            NuxRoutes.APK to strings.apkStudio,
            NuxRoutes.REMOTE to strings.remoteControl,
            NuxRoutes.GUIDE to strings.appGuideTitle
        )
    }

    val matchedConvs = remember(normalizedQ, conversations) {
        if (normalizedQ.isBlank()) conversations.filter { !it.archived && !it.locked }.take(8)
        else conversations.filter { !it.locked && normalizeArabic(it.title).contains(normalizedQ) }
    }
    val matchedProjects = remember(normalizedQ, projects) {
        if (normalizedQ.isBlank()) emptyList()
        else projects.filter { normalizeArabic("${it.name} ${it.description}").contains(normalizedQ) }
    }
    val matchedImages = remember(normalizedQ, images) {
        if (normalizedQ.isBlank()) emptyList()
        else images.filter { normalizeArabic("${it.prompt} ${it.style}").contains(normalizedQ) }
    }
    val matchedSections = remember(normalizedQ, appSections) {
        if (normalizedQ.isBlank()) appSections.take(6)
        else appSections.filter { normalizeArabic(it.second).contains(normalizedQ) }
    }

    val quickChips = remember(strings.isArabic) {
        if (strings.isArabic) listOf("صور", "برمجة", "مواقع", "ذاكرة", "أمان", "تطبيق")
        else listOf("images", "code", "sites", "memory", "security", "apk")
    }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.search, strings = strings, onBack = onBack)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(if (strings.isArabic) "ابحث في المحادثات، الصور، المشاريع والأقسام..." else "Search chats, images, projects, sections...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(999.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickChips.forEach { chip ->
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(nux.glassSurface)
                            .border(1.dp, nux.glassBorder, RoundedCornerShape(999.dp))
                            .clickable { query = chip }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (matchedSections.isNotEmpty()) {
                    item {
                        Text(
                            text = if (strings.isArabic) "الأقسام" else "Sections",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(matchedSections, key = { "sec_${it.first}" }) { (route, label) ->
                        GlassCard(
                            onClick = { onNavigateRoute(route) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }

                if (matchedConvs.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.chatHistory,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(matchedConvs, key = { "conv_${it.id}" }) { conv ->
                        GlassCard(
                            onClick = { onOpenConversation(conv.id) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ChatBubbleOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(conv.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                if (matchedProjects.isNotEmpty()) {
                    item {
                        Text(strings.projects, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    items(matchedProjects, key = { "proj_${it.id}" }) { proj ->
                        GlassCard(
                            onClick = { onNavigateRoute(NuxRoutes.PROJECTS) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text("${proj.iconEmoji} ${proj.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (matchedImages.isNotEmpty()) {
                    item {
                        Text(strings.images, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    items(matchedImages, key = { "img_${it.id}" }) { img ->
                        GlassCard(
                            onClick = { onNavigateRoute(NuxRoutes.IMAGES) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(img.prompt, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImagesScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val context = LocalContext.current
    val nux = LocalNuxColors.current
    val imagesState by viewModel.generatedImagesState.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()

    var prompt by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(ImageStyleOption.REALISTIC) }
    var ratio by remember { mutableStateOf(AspectRatioOption.SQUARE) }
    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }

    val records = (imagesState as? UiState.Success)?.data.orEmpty()

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.images, strings = strings, onBack = onBack)

            GlassCard(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                strong = true
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it.take(1500) },
                    label = { Text(if (strings.isArabic) "صف الصورة التي تريد توليدها" else "Describe the image to generate") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_studio_prompt")
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ImageStyleOption.entries.forEach { st ->
                        val active = st == style
                        Text(
                            text = if (strings.isArabic) st.arLabel else st.enLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary else nux.glassSurface)
                                .clickable { style = st }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    AspectRatioOption.entries.forEach { ar ->
                        val active = ar == ratio
                        Text(
                            text = ar.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary else nux.glassSurface)
                                .clickable { ratio = ar }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                BrandGradientButton(
                    text = if (isStreaming) {
                        if (strings.isArabic) "جارٍ الإنشاء..." else "Generating..."
                    } else {
                        if (strings.isArabic) "إنشاء الصورة" else "Generate image"
                    },
                    enabled = prompt.isNotBlank() && !isStreaming,
                    onClick = {
                        viewModel.generateStudioImage(prompt, style, ratio)
                        prompt = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_studio_generate_button"),
                    leadingIcon = {
                        if (isStreaming) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                )
            }

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (strings.isArabic) "لم يتم إنشاء صور بعد — اكتب وصفًا بالأعلى!" else "No generated images yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(165.dp),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(records, key = { it.id }) { rec ->
                        val bmp = remember(rec.localFileName) { MediaUtils.loadLocalBitmap(rec.localFileName) }
                        GlassCard(contentPadding = PaddingValues(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .clickable { if (bmp != null) previewBmp = bmp },
                                contentAlignment = Alignment.Center
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = rec.prompt,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(rec.prompt, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${rec.style} · ${rec.aspectRatio}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (!rec.conversationId.isNullOrBlank()) {
                                    IconButton(onClick = { onOpenConversation(rec.conversationId) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Open chat", modifier = Modifier.size(17.dp))
                                    }
                                }
                                if (bmp != null) {
                                    IconButton(
                                        onClick = {
                                            val ok = MediaUtils.saveImageToGallery(context, bmp, rec.prompt)
                                            viewModel.showToast(if (ok) "تم حفظ الصورة في المعرض" else "تعذّر الحفظ", !ok)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = "Save to gallery", modifier = Modifier.size(17.dp))
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteStudioImage(rec.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewBmp?.let { bmp ->
        Dialog(onDismissRequest = { previewBmp = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(16.dp)
            ) {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = "Preview", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                IconButton(
                    onClick = { previewBmp = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val conversations = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val images = (viewModel.generatedImagesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val projects = (viewModel.projectsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val sites = (viewModel.siteBuildsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val apps = (viewModel.nativeAppsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.library, strings = strings, onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(strings.allChats, "${conversations.size}", Modifier.weight(1f)) { onNavigateRoute(NuxRoutes.SEARCH) }
                        StatTile(strings.images, "${images.size}", Modifier.weight(1f)) { onNavigateRoute(NuxRoutes.IMAGES) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(strings.projects, "${projects.size}", Modifier.weight(1f)) { onNavigateRoute(NuxRoutes.PROJECTS) }
                        StatTile(strings.siteBuilder, "${sites.size + apps.size}", Modifier.weight(1f)) { onNavigateRoute(NuxRoutes.BUILDER) }
                    }
                }

                item {
                    Text(
                        text = if (strings.isArabic) "أحدث المحادثات" else "Recent conversations",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                items(conversations.filter { !it.locked && !it.archived }.take(20), key = { it.id }) { conv ->
                    GlassCard(onClick = { onOpenConversation(conv.id) }, contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(conv.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(conv.modelName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (conv.pinned) {
                                Text("📌", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, count: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Text(count, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FilesScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val nux = LocalNuxColors.current
    val images = (viewModel.generatedImagesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val sites = (viewModel.siteBuildsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val apps = (viewModel.nativeAppsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()

    var filter by remember { mutableStateOf(0) } // 0=All, 1=Images, 2=Documents/Builds

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.files, strings = strings, onBack = onBack)

            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = if (strings.isArabic) listOf("الكل", "الصور", "المستندات والمشاريع") else listOf("All", "Images", "Docs & Builds")
                tabs.forEachIndexed { idx, label ->
                    val active = filter == idx
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary else nux.glassSurface)
                            .clickable { filter = idx }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filter == 0 || filter == 1) {
                    items(images, key = { "f_img_${it.id}" }) { img ->
                        GlassCard(
                            onClick = { if (!img.conversationId.isNullOrBlank()) onOpenConversation(img.conversationId) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(img.prompt, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Text("${img.style} · ${img.aspectRatio}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                if (filter == 0 || filter == 2) {
                    items(sites, key = { "f_site_${it.id}" }) { site ->
                        GlassCard(contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(site.title, style = MaterialTheme.typography.bodyMedium)
                                    Text("HTML5 · ${site.htmlCode.length} chars", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    items(apps, key = { "f_app_${it.id}" }) { app ->
                        GlassCard(contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
