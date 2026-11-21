package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.AppLanguage
import com.example.core.BubbleShape
import com.example.core.BubbleTone
import com.example.core.MediaUtils
import com.example.core.NuxText
import com.example.core.ReplyLanguages
import com.example.core.ThemeMode
import com.example.core.normalizeArabic
import com.example.data.model.NuxCatalog
import com.example.data.model.NuxRoutes
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.ChatBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalNuxColors
import com.example.viewmodel.NuxViewModel
import com.example.viewmodel.UiState

@Composable
fun SettingsScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val context = LocalContext.current
    val nux = LocalNuxColors.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val profile = (viewModel.profileState.collectAsStateWithLifecycle().value as? UiState.Success)?.data
    val conversations = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val images = (viewModel.generatedImagesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val memory = (viewModel.memoryState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotifications(granted)
        if (!granted) {
            viewModel.showToast(
                if (strings.isArabic) "يرجى السماح بإذن الإشعارات من إعدادات النظام" else "Notification permission denied",
                isError = true
            )
        }
    }

    val normQ = remember(searchQuery) { normalizeArabic(searchQuery) }
    fun matches(vararg keywords: String): Boolean {
        if (normQ.isBlank()) return true
        return keywords.any { normalizeArabic(it).contains(normQ) }
    }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            NuxScreenTopBar(title = strings.settings, strings = strings, onBack = onBack)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Account
                if (matches("الحساب", "account", "ملف", "profile", "بريد")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "1. الحساب" else "1. Account")
                        GlassCard(onClick = { onNavigateRoute(NuxRoutes.PROFILE) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (viewModel.isGuestMode) strings.guestUserLabel else (profile?.displayName?.ifBlank { "Nux User" } ?: "Nux User"),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (viewModel.isGuestMode) strings.signInWithGoogle else profile?.email.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (viewModel.isGuestMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(strings.profile, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // 2. General
                if (matches("عام", "general", "لغة", "language", "سمة", "theme", "داكن", "فاتح", "إشعارات", "اهتزاز", "تفكير", "علامة مائية")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "2. عام" else "2. General")
                        GlassCard {
                            Text(if (strings.isArabic) "لغة الواجهة" else "App language", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AppLanguage.entries.forEach { lang ->
                                    SettingsChip(
                                        label = lang.nativeName,
                                        selected = prefs.appLanguage == lang,
                                        onClick = { viewModel.setAppLanguage(lang) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(if (strings.isArabic) "المظهر" else "Theme", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val themes = listOf(
                                    ThemeMode.DARK to (if (strings.isArabic) "داكن" else "Dark"),
                                    ThemeMode.LIGHT to (if (strings.isArabic) "فاتح" else "Light"),
                                    ThemeMode.SYSTEM to (if (strings.isArabic) "النظام" else "System")
                                )
                                themes.forEach { (mode, label) ->
                                    SettingsChip(
                                        label = label,
                                        selected = prefs.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            SettingsSwitchRow(
                                title = if (strings.isArabic) "الإشعارات" else "Notifications",
                                checked = prefs.notificationsEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                        if (granted) viewModel.setNotifications(true)
                                        else notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotifications(enable)
                                    }
                                }
                            )
                            SettingsSwitchRow(
                                title = if (strings.isArabic) "الاهتزاز اللمسي (Haptics)" else "Haptic feedback",
                                checked = prefs.hapticsEnabled,
                                onCheckedChange = { viewModel.setHaptics(it) }
                            )
                            SettingsSwitchRow(
                                title = if (strings.isArabic) "إظهار خطوات التفكير" else "Show thinking steps",
                                checked = prefs.showThinkingSteps,
                                onCheckedChange = { viewModel.setShowThinking(it) }
                            )
                            SettingsSwitchRow(
                                title = if (strings.isArabic) "علامة Nux المائية على الصور" else "Nux image watermark",
                                checked = prefs.watermarkEnabled,
                                onCheckedChange = { viewModel.setWatermark(it) }
                            )
                        }
                    }
                }

                // 3. My Message Bubble Style
                if (matches("فقاعة", "bubble", "شكل", "لون", "رسائلي")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "3. مظهر فقاعات رسائلي" else "3. Message Bubble Style")
                        GlassCard {
                            val previewShape = RoundedCornerShape(if (prefs.bubbleShape == BubbleShape.PILL) 28.dp else 18.dp)
                            val previewBg = when (prefs.bubbleTone) {
                                BubbleTone.BLUE -> MaterialTheme.colorScheme.primary
                                BubbleTone.GLASS -> nux.glassSurfaceStrong
                                BubbleTone.MINIMAL -> MaterialTheme.colorScheme.secondary
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(previewShape)
                                    .background(previewBg)
                                    .border(1.dp, nux.glassBorder, previewShape)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (strings.isArabic) "معاينة فقاعة رسالتك ✨" else "Message bubble preview ✨",
                                    color = if (prefs.bubbleTone == BubbleTone.BLUE) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SettingsChip(if (strings.isArabic) "زجاجي" else "Glass", prefs.bubbleTone == BubbleTone.GLASS) { viewModel.setBubbleTone(BubbleTone.GLASS) }
                                SettingsChip(if (strings.isArabic) "أزرق" else "Blue", prefs.bubbleTone == BubbleTone.BLUE) { viewModel.setBubbleTone(BubbleTone.BLUE) }
                                SettingsChip(if (strings.isArabic) "بسيط" else "Minimal", prefs.bubbleTone == BubbleTone.MINIMAL) { viewModel.setBubbleTone(BubbleTone.MINIMAL) }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SettingsChip(if (strings.isArabic) "زوايا ناعمة" else "Rounded", prefs.bubbleShape == BubbleShape.ROUNDED) { viewModel.setBubbleShape(BubbleShape.ROUNDED) }
                                SettingsChip(if (strings.isArabic) "كبسولة" else "Pill", prefs.bubbleShape == BubbleShape.PILL) { viewModel.setBubbleShape(BubbleShape.PILL) }
                            }
                        }
                    }
                }

                // 4. Voice
                if (matches("صوت", "voice", "سرعة", "speech", "tts")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "4. الصوت والنطق" else "4. Voice & Speech")
                        GlassCard {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NuxCatalog.liveVoices.forEach { v ->
                                    SettingsChip(
                                        label = if (strings.isArabic) v.arName else v.enName,
                                        selected = prefs.liveVoice == v.id,
                                        onClick = { viewModel.setLiveVoice(v.id) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(if (strings.isArabic) "سرعة النطق" else "Speech speed", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0.8f to "0.8x", 1.0f to "1.0x", 1.2f to "1.2x", 1.5f to "1.5x").forEach { (rate, lbl) ->
                                    SettingsChip(lbl, prefs.speechRate == rate) { viewModel.setSpeechRate(rate) }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            BrandGradientButton(
                                text = if (strings.isArabic) "اختبار الصوت" else "Test Voice",
                                onClick = {
                                    val sample = if (strings.isArabic) "مرحبًا، أنا نوكس، مساعدك الذكي." else "Hello, I am Nux, your AI assistant."
                                    viewModel.speakText("test_voice", sample)
                                },
                                height = 40.dp,
                                leadingIcon = { Icon(Icons.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                // 5. Reply Language
                if (matches("لغة الرد", "reply", "ردود")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "5. لغة الردود" else "5. Reply Language")
                        GlassCard {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ReplyLanguages.options.forEach { (code, label) ->
                                    SettingsChip(
                                        label = if (code == "auto") ReplyLanguages.labelFor("auto", strings.isArabic) else label,
                                        selected = prefs.replyLanguage == code,
                                        onClick = { viewModel.setReplyLanguage(code) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Personalization & Memory
                if (matches("تخصيص", "ذاكرة", "memory", "personalization")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "6. التخصيص والذاكرة" else "6. Personalization & Memory")
                        GlassCard {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BrandGradientButton(
                                    text = strings.customize,
                                    onClick = { onNavigateRoute(NuxRoutes.CUSTOMIZE) },
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp
                                )
                                BrandGradientButton(
                                    text = "${strings.memory} (${memory.size})",
                                    onClick = { onNavigateRoute(NuxRoutes.MEMORY) },
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp
                                )
                            }
                        }
                    }
                }

                // 7. Security, Archive & Locked Chats
                if (matches("أمان", "قفل", "أرشيف", "archive", "locked", "security", "pin")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "7. الأمان والأرشيف والدردشات المقفلة" else "7. Security, Archive & Locked")
                        GlassCard {
                            SettingsNavRow(Icons.Rounded.Security, strings.security) { onNavigateRoute(NuxRoutes.SECURITY) }
                            SettingsNavRow(Icons.Rounded.Archive, strings.archiveTitle) { onNavigateRoute(NuxRoutes.ARCHIVE) }
                            SettingsNavRow(Icons.Rounded.Lock, strings.lockedChatsTitle) { onNavigateRoute(NuxRoutes.LOCKED) }
                        }
                    }
                }

                // 8 & 9. Temp Chat & Privacy
                if (matches("مؤقتة", "خصوصية", "بحث ويب", "privacy", "temp")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "8 و 9. الخصوصية والمحادثة المؤقتة" else "8 & 9. Temp Chat & Privacy")
                        GlassCard {
                            SettingsSwitchRow(
                                title = if (strings.isArabic) "تضمين التخصيص في المؤقتة" else "Use customization in temp chat",
                                checked = prefs.tempChatCustomized,
                                onCheckedChange = { viewModel.setTempChatCustomized(it) }
                            )
                            SettingsSwitchRow(
                                title = if (strings.isArabic) "تفعيل وضع البحث افتراضيًا" else "Web research mode by default",
                                checked = prefs.webSearchDefault,
                                onCheckedChange = { viewModel.setWebSearchDefault(it) }
                            )
                        }
                    }
                }

                // 10, 11, 12. Storage, Coming Soon, Usage
                if (matches("مساحة", "تخزين", "استخدام", "قريبًا", "فيديو", "محفظة", "usage", "storage")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "10–12. الاستخدام والتخزين والمزايا القادمة" else "10–12. Storage, Usage & Roadmap")
                        GlassCard {
                            Text(
                                text = if (strings.isArabic) {
                                    "المحادثات: ${conversations.size} · الصور: ${images.size} · الذاكرة: ${memory.size}"
                                } else {
                                    "Chats: ${conversations.size} · Images: ${images.size} · Memory items: ${memory.size}"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BadgeTag(if (strings.isArabic) "فيديو AI · قريبًا" else "AI Video · Soon")
                                BadgeTag(if (strings.isArabic) "المحفظة · قريبًا" else "Wallet · Soon")
                            }
                        }
                    }
                }

                // 13. My Data
                if (matches("بياناتي", "تصدير", "حذف", "export", "delete", "data")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "13. بياناتي" else "13. My Data")
                        GlassCard {
                            SettingsNavRow(Icons.Rounded.Download, if (strings.isArabic) "تصدير المحادثات (JSON)" else "Export chats (JSON)") {
                                viewModel.exportConversations { json ->
                                    MediaUtils.shareText(context, "Nux Export", json)
                                }
                            }
                            SettingsNavRow(
                                icon = Icons.Rounded.DeleteForever,
                                label = if (strings.isArabic) "حذف جميع المحادثات" else "Delete all conversations",
                                isDestructive = true
                            ) { confirmDeleteAll = true }
                        }
                    }
                }

                // 14. App Guide & Legal
                if (matches("دليل", "قانون", "خصوصية", "ترخيص", "guide", "legal")) {
                    item {
                        SettingsSectionTitle(if (strings.isArabic) "14. الدليل والمستندات القانونية" else "14. Guide & Legal")
                        GlassCard {
                            SettingsNavRow(Icons.Rounded.MenuBook, strings.appGuideTitle) { onNavigateRoute(NuxRoutes.GUIDE) }
                            SettingsNavRow(Icons.Rounded.Policy, if (strings.isArabic) "سياسة الخصوصية" else "Privacy Policy") {
                                onNavigateRoute(NuxRoutes.legalRoute("privacy"))
                            }
                            SettingsNavRow(Icons.Rounded.HelpOutline, if (strings.isArabic) "التراخيص" else "Licenses") {
                                onNavigateRoute(NuxRoutes.legalRoute("licenses"))
                            }
                        }
                    }
                }
            }

            // Sticky Bottom Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (strings.isArabic) "ابحث داخل الإعدادات..." else "Search settings...") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                shape = RoundedCornerShape(999.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("settings_search_input")
            )
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(if (strings.isArabic) "حذف جميع المحادثات" else "Delete all chats") },
            text = { Text(if (strings.isArabic) "لا يمكن التراجع عن هذه الخطوة. هل أنت متأكد؟" else "This cannot be undone. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllConversations()
                    confirmDeleteAll = false
                }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val nux = LocalNuxColors.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else nux.glassSurface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else nux.glassBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BadgeTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
