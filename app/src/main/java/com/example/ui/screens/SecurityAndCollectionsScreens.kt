package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.MediaUtils
import com.example.core.NuxText
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.signOutUser
import com.example.ui.components.BrandGradientButton
import com.example.ui.components.ChatBackdrop
import com.example.ui.components.GlassCard
import com.example.ui.components.NuxAtomLogo
import com.example.ui.components.PinPadDialog
import com.example.ui.theme.LocalNuxColors
import com.example.ui.theme.NuxBlueDark
import com.example.ui.theme.NuxBlueLight
import com.example.ui.theme.NuxBrandEnd
import com.example.ui.theme.NuxBrandStart
import com.example.viewmodel.NuxViewModel
import com.example.viewmodel.UiState

@Composable
fun SecurityScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val pinsState by viewModel.securityPinsState.collectAsStateWithLifecycle()
    val pins = (pinsState as? UiState.Success)?.data.orEmpty()
    val loginPin = pins.firstOrNull { it.kind == "login" }
    val chatsPin = pins.firstOrNull { it.kind == "chats" }

    var editingPinKind by remember { mutableStateOf<String?>(null) }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.security, strings = strings, onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.isGuestMode) {
                    GlassCard(strong = true) {
                        Text(strings.guestUserLabel, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(strings.guestNotice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        BrandGradientButton(
                            text = strings.signInWithGoogle,
                            onClick = {
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { viewModel.showToast("تم تسجيل الدخول بنجاح") },
                                    onAuthError = { msg -> viewModel.showToast(msg, true) },
                                    scope = scope
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                GlassCard(strong = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) "رمز تسجيل الدخول (2FA PIN)" else "Login 2FA PIN",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (loginPin?.enabled == true) {
                                    if (strings.isArabic) "مفعّل (6 أرقام)" else "Enabled (6 digits)"
                                } else {
                                    if (strings.isArabic) "غير مفعّل" else "Disabled"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandGradientButton(
                            text = if (loginPin?.enabled == true) {
                                if (strings.isArabic) "تغيير الرمز" else "Change PIN"
                            } else {
                                if (strings.isArabic) "تفعيل الرمز" else "Enable PIN"
                            },
                            onClick = { editingPinKind = "login" },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        if (loginPin?.enabled == true) {
                            OutlinedButton(onClick = { viewModel.disablePin("login") }) {
                                Text(if (strings.isArabic) "إيقاف" else "Disable")
                            }
                        }
                    }
                }

                GlassCard(strong = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) "رمز قفل الدردشات" else "Chat Lock PIN",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (chatsPin?.enabled == true) {
                                    if (strings.isArabic) "مفعّل لحماية الدردشات المقفلة" else "Enabled for locked chats"
                                } else {
                                    if (strings.isArabic) "غير مفعّل" else "Disabled"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandGradientButton(
                            text = if (chatsPin?.enabled == true) {
                                if (strings.isArabic) "تغيير الرمز" else "Change PIN"
                            } else {
                                if (strings.isArabic) "تفعيل الرمز" else "Enable PIN"
                            },
                            onClick = { editingPinKind = "chats" },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        if (chatsPin?.enabled == true) {
                            OutlinedButton(onClick = { viewModel.disablePin("chats") }) {
                                Text(if (strings.isArabic) "إيقاف" else "Disable")
                            }
                        }
                    }
                }

                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Devices, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (strings.isArabic) "الأجهزة الموثوقة" else "Trusted devices", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (strings.isArabic) {
                                    "عدد الأجهزة الموثوقة: ${loginPin?.trustedDevices?.size ?: 1}"
                                } else {
                                    "Trusted devices count: ${loginPin?.trustedDevices?.size ?: 1}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (loginPin?.enabled == true) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { viewModel.trustOnlyThisDevice() }) {
                            Text(if (strings.isArabic) "الإبقاء على هذا الجهاز فقط" else "Keep only this device")
                        }
                    }
                }
            }
        }
    }

    editingPinKind?.let { kind ->
        PinPadDialog(
            title = if (kind == "login") {
                if (strings.isArabic) "تعيين رمز تسجيل الدخول" else "Set Login PIN"
            } else {
                if (strings.isArabic) "تعيين رمز قفل الدردشات" else "Set Chat Lock PIN"
            },
            subtitle = if (strings.isArabic) "أدخل 6 أرقام ثم أعد تأكيدها" else "Enter 6 digits and confirm",
            confirmMode = true,
            onDismiss = { editingPinKind = null },
            onPinCompleted = { rawPin ->
                viewModel.setPin(kind, rawPin)
                editingPinKind = null
            }
        )
    }
}

@Composable
fun ArchiveScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val conversations = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val archived = remember(conversations) { conversations.filter { it.archived } }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.archiveTitle, strings = strings, onBack = onBack)

            if (archived.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (strings.isArabic) "لا توجد محادثات مؤرشفة." else "No archived chats.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(archived, key = { it.id }) { conv ->
                        GlassCard(onClick = { onOpenConversation(conv.id) }, contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Archive, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(conv.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { viewModel.toggleArchiveConversation(conv) }) {
                                    Icon(Icons.Rounded.Unarchive, contentDescription = strings.unarchive)
                                }
                                IconButton(onClick = { viewModel.deleteConversation(conv.id) }) {
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

@Composable
fun LockedChatsScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val conversations = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val pins = (viewModel.securityPinsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val chatsPinEnabled = pins.any { it.kind == "chats" && it.enabled }

    var unlockedSection by remember(chatsPinEnabled) { mutableStateOf(!chatsPinEnabled) }
    var showPinPad by remember { mutableStateOf(chatsPinEnabled) }
    var pinErr by remember { mutableStateOf<String?>(null) }

    val locked = remember(conversations) { conversations.filter { it.locked } }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.lockedChatsTitle, strings = strings, onBack = onBack)

            if (!unlockedSection) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BrandGradientButton(
                        text = if (strings.isArabic) "إدخال رمز قفل الدردشات" else "Enter Chat Lock PIN",
                        onClick = { showPinPad = true }
                    )
                }
            } else if (locked.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (strings.isArabic) "لا توجد محادثات مقفلة." else "No locked chats.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(locked, key = { it.id }) { conv ->
                        GlassCard(
                            onClick = {
                                viewModel.markChatUnlocked(conv.id)
                                onOpenConversation(conv.id)
                            },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(conv.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { viewModel.toggleLockConversation(conv) }) {
                                    Icon(Icons.Rounded.LockOpen, contentDescription = strings.unlockChat)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinPad) {
        PinPadDialog(
            title = strings.lockedChatsTitle,
            subtitle = if (strings.isArabic) "أدخل رمز قفل الدردشات" else "Enter Chat Lock PIN",
            confirmMode = false,
            externalError = pinErr,
            onDismiss = { showPinPad = false },
            onPinCompleted = { entered ->
                if (viewModel.verifyPin("chats", entered)) {
                    pinErr = null
                    unlockedSection = true
                    showPinPad = false
                } else {
                    pinErr = if (strings.isArabic) "رمز غير صحيح" else "Incorrect PIN"
                }
            }
        )
    }
}

@Composable
fun PinnedChatsScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    val conversations = (viewModel.conversationsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data.orEmpty()
    val pinned = remember(conversations) { conversations.filter { it.pinned && !it.archived && !it.locked } }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.pinnedChatsTitle, strings = strings, onBack = onBack)

            if (pinned.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (strings.isArabic) "لا توجد محادثات مثبتة." else "No pinned chats.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pinned, key = { it.id }) { conv ->
                        GlassCard(onClick = { onOpenConversation(conv.id) }, contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PushPin, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(conv.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { viewModel.togglePinConversation(conv) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = strings.unpin)
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
fun ProfileScreen(
    viewModel: NuxViewModel,
    strings: NuxText,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val nux = LocalNuxColors.current

    val profile = (viewModel.profileState.collectAsStateWithLifecycle().value as? UiState.Success)?.data
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(profile?.displayName) {
        if (profile != null) displayName = profile.displayName
    }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = strings.profile, strings = strings, onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(strong = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(nux.capsuleGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = profile?.photoUrl,
                                    contentDescription = profile?.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = (profile?.displayName?.firstOrNull() ?: 'N').uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = if (viewModel.isGuestMode) strings.guestUserLabel else profile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (viewModel.isGuestMode) {
                            Text(
                                text = strings.guestNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BrandGradientButton(
                                text = strings.signInWithGoogle,
                                onClick = {
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = { viewModel.showToast("تم تسجيل الدخول بنجاح") },
                                        onAuthError = { msg -> viewModel.showToast(msg, true) },
                                        scope = scope
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_google_sign_in_button")
                            )
                        } else {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it.take(120) },
                                label = { Text(if (strings.isArabic) "الاسم الظاهر" else "Display name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )

                            BrandGradientButton(
                                text = strings.save,
                                onClick = { viewModel.updateDisplayName(displayName) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (viewModel.isGuestMode) {
                    OutlinedButton(
                        onClick = { viewModel.exitGuestMode() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exit_guest_mode_button")
                    ) {
                        Text(strings.exitGuestMode)
                    }
                } else {
                    OutlinedButton(
                        onClick = { signOutUser(credentialManager, scope) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.signOut)
                    }
                }
            }
        }
    }
}

@Composable
fun GuideScreen(
    strings: NuxText,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val paletteColors = remember {
        listOf(
            "Primary Dark" to NuxBlueDark,
            "Primary Light" to NuxBlueLight,
            "Brand Start" to NuxBrandStart,
            "Brand End" to NuxBrandEnd
        )
    }

    val guideText = remember(strings.isArabic) {
        if (strings.isArabic) {
            """
            دليل تطبيق Nux · نوكس:
            1. المحادثات الذكية: اختر بين Flash 3.8 و Flash Lite و Argon 4 مع دعم التفكير العميق والبحث.
            2. الإضافات (@): اكتب @ داخل خانة الإرسال للوصول السريع إلى الصور، البرمجة، الترجمة، والمواقع.
            3. استوديو الصور: أنشئ أو عدّل الصور بأسلوب واقعي أو أنمي أو 3D أو سينمائي مع نسب أبعاد متعددة.
            4. المحادثة الصوتية الحيّة: حاور نوكس صوتيًا مع اختيار 8 شخصيات صوتية.
            5. الأمان والخصوصية: فعّل رمز PIN للدخول (2FA) ورمزًا منفصلًا لقفل الدردشات الحساسة.
            """.trimIndent()
        } else {
            """
            Nux App Guide:
            1. Smart Chat: Switch between Flash 3.8, Flash Lite, and Argon 4 with deep thinking & research mode.
            2. @ Tools: Type @ in the composer for instant access to images, coding, translation, and builders.
            3. Image Studio: Generate or edit images across 6 styles and 3 aspect ratios.
            4. Live Voice: Speak naturally with Nux using 8 distinct voice profiles.
            5. Security: Protect sign-in with 2FA PIN and lock private conversations separately.
            """.trimIndent()
        }
    }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(
                title = strings.appGuideTitle,
                strings = strings,
                onBack = onBack,
                trailing = {
                    IconButton(onClick = { MediaUtils.shareText(context, strings.appGuideTitle, guideText) }) {
                        Icon(Icons.Rounded.Share, contentDescription = strings.share)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(strong = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NuxAtomLogo(size = 42.dp, animated = true, withGlow = true)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Nux · نوكس", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(strings.appTagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(guideText, style = MaterialTheme.typography.bodyMedium)
                }

                GlassCard {
                    Text(if (strings.isArabic) "لوحة ألوان Nux" else "Nux Color Palette", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paletteColors.forEach { (name, col) ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(col)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalScreen(
    doc: String,
    strings: NuxText,
    onBack: () -> Unit
) {
    val title = when (doc) {
        "privacy" -> if (strings.isArabic) "سياسة الخصوصية" else "Privacy Policy"
        "licenses" -> if (strings.isArabic) "التراخيص والمصادر المفتوحة" else "Open Source Licenses"
        else -> if (strings.isArabic) "معلومات قانونية" else "Legal Notice"
    }

    val body = when (doc) {
        "privacy" -> if (strings.isArabic) {
            "تُخزَّن محادثاتك ومشاريعك وذاكرتك حصريًا ضمن نطاق حسابك الشخصي في Firebase Firestore. لا يشارك نوكس بياناتك الخاصة مع أطراف خارجية، ويمكنك تصدير جميع محادثاتك أو حذفها في أي وقت من قسم «بياناتي» في الإعدادات."
        } else {
            "Your conversations, projects, and memory items are scoped strictly to your Firebase account. You can export or delete all conversations anytime under Settings -> My Data."
        }
        else -> if (strings.isArabic) {
            "تطبيق Nux مبني باستخدام Jetpack Compose و Material Design 3 و Firebase AI Logic وخط Cairo المرخّص بموجب SIL Open Font License."
        } else {
            "Nux is built with Jetpack Compose, Material Design 3, Firebase AI Logic, and the Cairo typeface under the SIL Open Font License."
        }
    }

    ChatBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NuxScreenTopBar(title = title, strings = strings, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                GlassCard(strong = true) {
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
