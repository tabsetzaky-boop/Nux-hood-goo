package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.NuxText
import com.example.data.model.MemoryItem
import com.example.data.model.NuxCatalog
import com.example.data.model.NuxRoutes
import com.example.data.model.Project
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.ChatBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalNuxColors
import com.example.viewmodel.NuxViewModel
import com.example.viewmodel.UiState

@Composable
fun ProjectsScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onStartProjectChat: (String) -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()

    var editingProject by remember { mutableStateOf<Project?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(
                title = strings.projects,
                strings = strings,
                onBack = onBack,
                trailing = {
                    BrandGradientButton(
                        text = if (strings.isArabic) "مشروع جديد" else "New project",
                        onClick = {
                            editingProject = Project()
                            showDialog = true
                        },
                        height = 36.dp,
                        leadingIcon = { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("new_project_button")
                    )
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3 Studio Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioShortcutCard(
                            icon = Icons.Rounded.Language,
                            title = strings.siteBuilder,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateRoute(NuxRoutes.BUILDER) }
                        )
                        StudioShortcutCard(
                            icon = Icons.Rounded.Android,
                            title = strings.apkStudio,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateRoute(NuxRoutes.APK) }
                        )
                        StudioShortcutCard(
                            icon = Icons.Rounded.Psychology,
                            title = strings.projectBrain,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateRoute(NuxRoutes.BRAIN) }
                        )
                    }
                }

                if (projects.isEmpty()) {
                    item {
                        GlassCard {
                            Text(
                                text = if (strings.isArabic) "لا توجد مساحات عمل بعد. أنشئ مشروعك الأول لتنظيم المحادثات والتعليمات." else "No workspaces yet. Create your first project.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(project.iconEmoji, fontSize = 24.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = project.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (project.description.isNotBlank()) {
                                            Text(
                                                text = project.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        viewModel.saveProject(project.copy(pinned = !project.pinned))
                                    }) {
                                        Icon(
                                            Icons.Rounded.PushPin,
                                            contentDescription = strings.pin,
                                            tint = if (project.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingProject = project
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = strings.rename)
                                    }
                                    IconButton(onClick = { viewModel.deleteProject(project.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (project.instructions.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = project.instructions,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            BrandGradientButton(
                                text = if (strings.isArabic) "محادثة جديدة في المشروع" else "New chat in project",
                                onClick = { onStartProjectChat(project.id) },
                                height = 38.dp,
                                leadingIcon = { Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog && editingProject != null) {
        val initial = editingProject!!
        var emoji by remember(initial.id) { mutableStateOf(initial.iconEmoji.ifBlank { "📁" }) }
        var name by remember(initial.id) { mutableStateOf(initial.name) }
        var desc by remember(initial.id) { mutableStateOf(initial.description) }
        var instr by remember(initial.id) { mutableStateOf(initial.instructions) }
        var pinned by remember(initial.id) { mutableStateOf(initial.pinned) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (initial.id.isBlank()) (if (strings.isArabic) "مشروع جديد" else "New Project") else strings.rename) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { emoji = it.take(4) },
                            label = { Text("أيقونة") },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(100) },
                            label = { Text(if (strings.isArabic) "اسم المشروع" else "Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it.take(500) },
                        label = { Text(if (strings.isArabic) "الوصف" else "Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instr,
                        onValueChange = { instr = it.take(2000) },
                        label = { Text(if (strings.isArabic) "تعليمات المشروع للنموذج" else "Project instructions") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                        Text(strings.pin)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.saveProject(
                            initial.copy(
                                iconEmoji = emoji.ifBlank { "📁" },
                                name = name.trim(),
                                description = desc.trim(),
                                instructions = instr.trim(),
                                pinned = pinned
                            )
                        )
                        showDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun StudioShortcutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizeScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onStartNewChat: () -> Unit
) {
    val nux = LocalNuxColors.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data

    var preferredName by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    LaunchedEffect(profile?.preferredName, profile?.customInstructions) {
        if (profile != null) {
            preferredName = profile.preferredName
            instructions = profile.customInstructions
        }
    }

    val quickChips = remember(strings.isArabic) {
        if (strings.isArabic) {
            listOf(
                "ردود مختصرة ومركّزة",
                "كود برمجي كامل مع الشرح",
                "لهجة دافئة وودودة",
                "نبرة مهنية رسمية"
            )
        } else {
            listOf("Concise & direct", "Full code with notes", "Warm & friendly", "Formal & executive")
        }
    }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.customize, strings = strings, onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(strong = true) {
                    OutlinedTextField(
                        value = preferredName,
                        onValueChange = { preferredName = it.take(60) },
                        label = { Text(if (strings.isArabic) "بماذا تحب أن يناديك نوكس؟" else "What should Nux call you?") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customize_name_input")
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it.take(2000) },
                        label = { Text(if (strings.isArabic) "تعليمات مخصصة (الأسلوب، التنسيق، الخبرات)" else "Custom instructions") },
                        supportingText = { Text("${instructions.length} / 2000") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customize_instructions_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickChips.forEach { chip ->
                            Text(
                                text = "+ $chip",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(nux.glassSurface)
                                    .border(1.dp, nux.glassBorder, RoundedCornerShape(999.dp))
                                    .clickable {
                                        instructions = if (instructions.isBlank()) chip else "$instructions\n- $chip".take(2000)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandGradientButton(
                            text = strings.save,
                            onClick = { viewModel.saveCustomization(preferredName, instructions) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customize_save_button")
                        )
                        TextButton(onClick = {
                            preferredName = ""
                            instructions = ""
                            viewModel.saveCustomization("", "")
                        }) {
                            Text(if (strings.isArabic) "مسح" else "Clear")
                        }
                    }
                }

                GlassCard(onClick = { onNavigateRoute(NuxRoutes.MEMORY) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Memory, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.memory, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = if (strings.isArabic) "أضف الحقائق والتفضيلات التي يتذكرها نوكس دائمًا" else "Manage persistent memory facts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                GlassCard(onClick = onStartNewChat) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.newChat, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val nux = LocalNuxColors.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val memoryState by viewModel.memoryState.collectAsStateWithLifecycle()
    val items = (memoryState as? UiState.Success)?.data.orEmpty()

    var newContent by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("شخصي") }
    var editItem by remember { mutableStateOf<MemoryItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(
                title = strings.memory,
                strings = strings,
                onBack = onBack,
                trailing = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) {
                            Text(if (strings.isArabic) "مسح الكل" else "Clear all", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (strings.isArabic) "استخدام الذاكرة في الردود" else "Use memory in replies", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = prefs.useMemory, onCheckedChange = { viewModel.setUseMemory(it) })
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (strings.isArabic) "حفظ التفضيلات تلقائيًا" else "Auto-save preferences", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = prefs.autoSaveMemory, onCheckedChange = { viewModel.setAutoSaveMemory(it) })
                        }
                    }
                }

                item {
                    GlassCard(strong = true) {
                        OutlinedTextField(
                            value = newContent,
                            onValueChange = { newContent = it.take(240) },
                            label = { Text(if (strings.isArabic) "أضف معلومة جديدة ليتذكرها نوكس" else "Add a fact to remember") },
                            supportingText = { Text("${newContent.length} / 240") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_add_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NuxCatalog.memoryCategories.forEach { cat ->
                                val active = newCategory == cat
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primary else nux.glassSurface)
                                        .clickable { newCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        BrandGradientButton(
                            text = if (strings.isArabic) "إضافة للذاكرة" else "Add memory",
                            enabled = newContent.isNotBlank(),
                            onClick = {
                                viewModel.addMemory(newContent, newCategory)
                                newContent = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_add_button")
                        )
                    }
                }

                items(items, key = { it.id }) { mem ->
                    GlassCard(contentPadding = PaddingValues(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mem.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(mem.content, style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = mem.enabled, onCheckedChange = { viewModel.toggleMemoryItem(mem) })
                            IconButton(onClick = { editItem = mem }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.rename, modifier = Modifier.size(17.dp))
                            }
                            IconButton(onClick = { viewModel.deleteMemoryItem(mem.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    editItem?.let { item ->
        var text by remember(item.id) { mutableStateOf(item.content) }
        var cat by remember(item.id) { mutableStateOf(item.category) }
        AlertDialog(
            onDismissRequest = { editItem = null },
            title = { Text(strings.rename) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = text, onValueChange = { text = it.take(240) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NuxCatalog.memoryCategories.forEach { c ->
                            Text(
                                text = c,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (cat == c) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (cat == c) MaterialTheme.colorScheme.primary else nux.glassSurface)
                                    .clickable { cat = c }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) viewModel.updateMemoryContent(item, text, cat)
                    editItem = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editItem = null }) { Text(strings.cancel) }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(if (strings.isArabic) "مسح الذاكرة" else "Clear memory") },
            text = { Text(if (strings.isArabic) "هل تريد حذف جميع عناصر الذاكرة المحفوظة؟" else "Delete all saved memory items?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllMemory()
                    confirmClear = false
                }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun ExploreScreen(
    strings: NuxText,
    onBack: () -> Unit,
    onSelectIdea: (String) -> Unit
) {
    val accents = remember {
        listOf(
            listOf(Color(0xFF0A84FF), Color(0xFF2F6BFF)),
            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
            listOf(Color(0xFF10B981), Color(0xFF0EA5E9)),
            listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
            listOf(Color(0xFF6366F1), Color(0xFF06B6D4)),
            listOf(Color(0xFFD946EF), Color(0xFF7C3AED)),
            listOf(Color(0xFF14B8A6), Color(0xFF3B82F6)),
        )
    }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.explore, strings = strings, onBack = onBack)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(NuxCatalog.exploreIdeas, key = { it.id }) { idea ->
                    val palette = accents[idea.accentIndex % accents.size]
                    val prompt = if (strings.isArabic) idea.promptAr else idea.promptEn
                    GlassCard(
                        onClick = { onSelectIdea(prompt) },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.linearGradient(palette))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (strings.isArabic) idea.categoryAr else idea.categoryEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (strings.isArabic) idea.titleAr else idea.titleEn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
