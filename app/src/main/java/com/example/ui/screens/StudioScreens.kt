package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.NuxText
import com.example.data.model.NuxCatalog
import com.example.data.model.SiteBuild
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.ChatBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalNuxColors
import com.example.viewmodel.NuxViewModel
import com.example.viewmodel.UiState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SiteBuilderScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val nux = LocalNuxColors.current
    val sitesState by viewModel.siteBuildsState.collectAsStateWithLifecycle()
    val isBuilding by viewModel.isBuildingSite.collectAsStateWithLifecycle()
    val sites = (sitesState as? UiState.Success)?.data.orEmpty()

    var prompt by remember { mutableStateOf("") }
    var selectedSite by remember { mutableStateOf<SiteBuild?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0=Preview, 1=Code

    val activeSite = selectedSite ?: sites.firstOrNull()

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.siteDeploy, strings = strings, onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(strong = true) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it.take(1500) },
                        label = { Text(if (strings.isArabic) "صف الموقع أو التعديل المطلوب" else "Describe website or edit") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("site_builder_prompt")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = if (strings.isArabic) {
                            listOf("صفحة هبوط لمنتج تقني", "بورتفوليو مصمم إبداعي", "قائمة مطعم عصري")
                        } else {
                            listOf("SaaS product landing page", "Creative portfolio", "Modern restaurant menu")
                        }
                        presets.forEach { p ->
                            Text(
                                text = p,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(nux.glassSurface)
                                    .clickable { prompt = p }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    BrandGradientButton(
                        text = if (isBuilding) {
                            if (strings.isArabic) "جارٍ البناء..." else "Building..."
                        } else {
                            if (strings.isArabic) "بناء / تحديث الموقع" else "Build / Update Site"
                        },
                        enabled = prompt.isNotBlank() && !isBuilding,
                        onClick = {
                            viewModel.generateOrUpdateSite(prompt, selectedSite)
                            prompt = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("site_builder_generate_button"),
                        leadingIcon = {
                            if (isBuilding) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.Language, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                if (activeSite != null) {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(activeSite.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("v${activeSite.versionCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (activeSite.published) (if (strings.isArabic) "منشور" else "Published") else (if (strings.isArabic) "مسودة" else "Draft"), style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.width(6.dp))
                                Switch(checked = activeSite.published, onCheckedChange = { viewModel.toggleSitePublished(activeSite) })
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TabButton(if (strings.isArabic) "معاينة حية" else "Live Preview", activeTab == 0) { activeTab = 0 }
                            TabButton(if (strings.isArabic) "كود HTML" else "HTML Code", activeTab == 1) { activeTab = 1 }
                        }

                        Spacer(Modifier.height(10.dp))

                        if (activeTab == 0) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        webViewClient = WebViewClient()
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(null, activeSite.htmlCode, "text/html", "UTF-8", null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(nux.codeBackground)
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = activeSite.htmlCode,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFFD6E4FF)
                                )
                            }
                        }
                    }
                }

                if (sites.isNotEmpty()) {
                    Text(if (strings.isArabic) "المواقع المحفوظة" else "Saved Sites", fontWeight = FontWeight.Bold)
                    sites.forEach { site ->
                        GlassCard(
                            onClick = { selectedSite = site },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Public, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(site.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text("v${site.versionCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteSiteBuild(site.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApkStudioScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val nux = LocalNuxColors.current
    val appsState by viewModel.nativeAppsState.collectAsStateWithLifecycle()
    val isPlanning by viewModel.isPlanningApk.collectAsStateWithLifecycle()
    val apps = (appsState as? UiState.Success)?.data.orEmpty()

    var idea by remember { mutableStateOf("") }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.apkStudio, strings = strings, onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(strong = true) {
                        OutlinedTextField(
                            value = idea,
                            onValueChange = { idea = it.take(1500) },
                            label = { Text(if (strings.isArabic) "فكرة تطبيق الأندرويد" else "Describe your Android app idea") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apk_studio_idea_input")
                        )
                        Spacer(Modifier.height(10.dp))
                        BrandGradientButton(
                            text = if (isPlanning) {
                                if (strings.isArabic) "جارٍ التخطيط..." else "Planning..."
                            } else {
                                if (strings.isArabic) "توليد مواصفة التطبيق" else "Generate App Spec"
                            },
                            enabled = idea.isNotBlank() && !isPlanning,
                            onClick = {
                                viewModel.planAndroidAppSpec(idea)
                                idea = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apk_studio_plan_button"),
                            leadingIcon = {
                                if (isPlanning) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                else Icon(Icons.Rounded.Android, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }

                items(apps, key = { it.id }) { app ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteNativeApp(app.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(app.specSummary, style = MaterialTheme.typography.bodySmall)
                        if (app.permissions.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                app.permissions.forEach { perm ->
                                    Text(
                                        text = perm,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(nux.glassSurface)
                                            .border(1.dp, nux.glassBorder, RoundedCornerShape(999.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        if (app.features.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            app.features.take(5).forEach { feat ->
                                Text("• $feat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectBrainScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val nux = LocalNuxColors.current
    val projects = (viewModel.projectsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val apps = (viewModel.nativeAppsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = if (strings.isArabic) {
        listOf("الهوية", "الصلاحيات", "المفاتيح", "المتطلبات", "الإصدارات")
    } else {
        listOf("Identity", "Permissions", "Secrets", "Requirements", "Versions")
    }

    val activeApp = apps.firstOrNull()
    val activeProj = projects.firstOrNull()

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.projectBrain, strings = strings, onBack = onBack)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    TabButton(label, selectedTab == idx) { selectedTab = idx }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(strong = true) {
                    when (selectedTab) {
                        0 -> {
                            Text(activeApp?.name ?: activeProj?.name ?: "Nux Workspace", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.height(4.dp))
                            Text(activeApp?.packageName ?: "com.nux.workspace", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(activeApp?.specSummary ?: activeProj?.description ?: if (strings.isArabic) "أنشئ مشروعًا أو تطبيقًا لعرض تفاصيل العقل المعماري." else "Create a project or APK spec to inspect architecture details.")
                        }
                        1 -> {
                            val perms = activeApp?.permissions.orEmpty()
                            if (perms.isEmpty()) Text(if (strings.isArabic) "لا توجد صلاحيات مسجلة بعد." else "No permissions recorded yet.")
                            else perms.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        2 -> {
                            val keys = activeApp?.secretKeys.orEmpty()
                            if (keys.isEmpty()) Text(if (strings.isArabic) "لا تتطلب مفاتيح خارجية حاليًا." else "No external secret keys required.")
                            else keys.forEach { Text("🔑 $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        3 -> {
                            val feats = activeApp?.features.orEmpty()
                            if (feats.isEmpty()) Text(if (strings.isArabic) "لا توجد متطلبات مسجلة بعد." else "No requirements recorded yet.")
                            else feats.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                        else -> {
                            Text("v${activeApp?.versionName ?: "1.0.0"} (Code ${activeApp?.versionCode ?: 1})", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (strings.isArabic) "حالة البناء: ${activeApp?.status ?: "جاهز"}" else "Status: ${activeApp?.status ?: "Ready"}",
                                color = nux.success
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteControlScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val deviceId by viewModel.deviceId.collectAsStateWithLifecycle()
    val steps by viewModel.remoteSteps.collectAsStateWithLifecycle()
    val isPlanning by viewModel.isPlanningRemote.collectAsStateWithLifecycle()

    val pairingCode = remember(deviceId) {
        val raw = deviceId.filter { it.isLetterOrDigit() }.uppercase().takeLast(6).padStart(6, '8')
        raw
    }
    val setupCmd = "npx @nux/remote-agent --pair $pairingCode"
    var instruction by remember { mutableStateOf("") }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.remoteControl, strings = strings, onBack = onBack)

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
                            Column {
                                Text(if (strings.isArabic) "رمز إقران الجهاز" else "Device Pairing Code", style = MaterialTheme.typography.labelMedium)
                                Text(pairingCode, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            OutlinedButton(onClick = {
                                clipboard.setText(AnnotatedString(setupCmd))
                                viewModel.showToast(strings.copied)
                            }) {
                                Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.copy)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(setupCmd, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    GlassCard(strong = true) {
                        OutlinedTextField(
                            value = instruction,
                            onValueChange = { instruction = it.take(1200) },
                            label = { Text(if (strings.isArabic) "اكتب المهمة المراد تخطيط تنفيذها" else "Describe remote task to plan") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_instruction_input")
                        )
                        Spacer(Modifier.height(10.dp))
                        BrandGradientButton(
                            text = if (isPlanning) (if (strings.isArabic) "جارٍ التخطيط..." else "Planning...") else (if (strings.isArabic) "تخطيط الخطوات" else "Plan Steps"),
                            enabled = instruction.isNotBlank() && !isPlanning,
                            onClick = { viewModel.planRemoteCommand(instruction) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Terminal, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                items(steps.size) { idx ->
                    val step = steps[idx]
                    GlassCard {
                        Text(step.title, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(step.commandHint, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (step.status) {
                                    "approved" -> if (strings.isArabic) "تمت الموافقة ✓" else "Approved ✓"
                                    "rejected" -> if (strings.isArabic) "مرفوض ✕" else "Rejected ✕"
                                    else -> if (step.requiresApproval) (if (strings.isArabic) "بانتظار موافقتك" else "Needs approval") else (if (strings.isArabic) "آمن" else "Safe")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                IconButton(onClick = { viewModel.approveRemoteStep(idx, true) }) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Approve", tint = LocalNuxColors.current.success)
                                }
                                IconButton(onClick = { viewModel.approveRemoteStep(idx, false) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EngineScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val probeResult by viewModel.engineProbeResult.collectAsStateWithLifecycle()

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.unifiedEngine, strings = strings, onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(strong = true) {
                    Text(if (strings.isArabic) "النماذج المتصلة" else "Connected Models", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    NuxCatalog.models.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(m.title, fontWeight = FontWeight.Medium)
                            Text(strings.doneBadge, color = LocalNuxColors.current.success, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nano Banana 2.1 (Images)", fontWeight = FontWeight.Medium)
                        Text(strings.doneBadge, color = LocalNuxColors.current.success, style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(Modifier.height(12.dp))
                    BrandGradientButton(
                        text = if (strings.isArabic) "اختبار الاتصال الحي بالمحرّك" else "Run Live Engine Probe",
                        onClick = { viewModel.runEngineProbe() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("engine_probe_button"),
                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    )
                    if (!probeResult.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(probeResult!!, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val nux = LocalNuxColors.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else nux.glassSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
