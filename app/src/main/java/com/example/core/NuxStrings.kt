package com.example.core

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val isRtl: Boolean
) {
    AR("ar", "العربية", true),
    EN("en", "English", false),
    FR("fr", "Français", false),
    ES("es", "Español", false),
    TR("tr", "Türkçe", false),
    DE("de", "Deutsch", false),
    UR("ur", "اردو", true);

    val layoutDirection: LayoutDirection
        get() = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: AR
    }
}

object ReplyLanguages {
    val options: List<Pair<String, String>> = listOf(
        "auto" to "تلقائي (لغة رسالتك)",
        "ar" to "العربية",
        "en" to "English",
        "fr" to "Français",
        "es" to "Español",
        "tr" to "Türkçe",
        "de" to "Deutsch",
        "ur" to "اردو",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文"
    )

    fun labelFor(code: String, isArabicUi: Boolean): String {
        if (code == "auto") return if (isArabicUi) "تلقائي (لغة رسالتك)" else "Auto (match your prompt)"
        return options.firstOrNull { it.first == code }?.second ?: code
    }
}

data class NuxText(
    val isArabic: Boolean
) {
    val appName = "Nux"
    val appTagline = if (isArabic) "نوكس · مساعدك الذكي بكل اللغات" else "Nux · Multilingual AI Assistant"
    val newChat = if (isArabic) "دردشة جديدة" else "New chat"
    val search = if (isArabic) "بحث" else "Search"
    val images = if (isArabic) "الصور" else "Images"
    val library = if (isArabic) "المكتبة" else "Library"
    val projects = if (isArabic) "المشاريع" else "Projects"
    val files = if (isArabic) "الملفات" else "Files"
    val customize = if (isArabic) "تخصيص" else "Customize"
    val explore = if (isArabic) "استكشاف" else "Explore"
    val remoteControl = if (isArabic) "التحكم عن بُعد" else "Remote control"
    val chatHistory = if (isArabic) "سجل الدردشة" else "Chat history"
    val pinnedChats = if (isArabic) "المحادثات المثبتة" else "Pinned chats"
    val allChats = if (isArabic) "كل المحادثات" else "All chats"
    val all = if (isArabic) "الكل" else "All"
    val settings = if (isArabic) "الإعدادات" else "Settings"
    val profile = if (isArabic) "الملف الشخصي" else "Profile"
    val signOut = if (isArabic) "تسجيل الخروج" else "Sign out"
    val signInWithGoogle = if (isArabic) "تسجيل الدخول بحساب Google" else "Sign in with Google"
    val homeGreetingPrefix = if (isArabic) "مرحبًا" else "Hello"
    val homeSubtitle = if (isArabic) "اكتب رسالتك أو اطلب صورة، وسأُنشئها لك." else "Write your message or ask for an image, and I'll create it for you."
    val askNux = if (isArabic) "اسأل نوكس" else "Ask Nux"
    val aiDisclaimer = if (isArabic) "نموذج ذكاء اصطناعي قد يخطئ" else "AI model may make mistakes"
    val backToChat = if (isArabic) "رجوع إلى المحادثة" else "Back to chat"
    val backToSettings = if (isArabic) "الإعدادات" else "Settings"
    val tempChatLabel = if (isArabic) "محادثة مؤقتة" else "Temporary chat"
    val thinkingStepsTitle = if (isArabic) "خطوات التفكير" else "Thinking steps"
    val deepThinkingChip = if (isArabic) "تفكير عميق" else "Deep thinking"
    val webSearchChip = if (isArabic) "بحث مفعّل" else "Web search"
    val sourcesTitle = if (isArabic) "المصادر" else "Sources"
    val copy = if (isArabic) "نسخ" else "Copy"
    val copied = if (isArabic) "تم النسخ" else "Copied"
    val helpful = if (isArabic) "رد مفيد" else "Helpful"
    val notHelpful = if (isArabic) "رد غير مفيد" else "Not helpful"
    val regenerate = if (isArabic) "إعادة توليد" else "Regenerate"
    val share = if (isArabic) "مشاركة" else "Share"
    val listen = if (isArabic) "استماع" else "Listen"
    val selectText = if (isArabic) "تحديد نص" else "Select text"
    val pin = if (isArabic) "تثبيت" else "Pin"
    val unpin = if (isArabic) "إلغاء التثبيت" else "Unpin"
    val lockChat = if (isArabic) "قفل الدردشة" else "Lock chat"
    val unlockChat = if (isArabic) "إلغاء القفل" else "Unlock"
    val archive = if (isArabic) "أرشفة" else "Archive"
    val unarchive = if (isArabic) "إلغاء الأرشفة" else "Unarchive"
    val rename = if (isArabic) "إعادة تسمية" else "Rename"
    val delete = if (isArabic) "حذف" else "Delete"
    val moveToProject = if (isArabic) "نقل إلى مشروع" else "Move to project"
    val cancel = if (isArabic) "إلغاء" else "Cancel"
    val save = if (isArabic) "حفظ" else "Save"
    val saved = if (isArabic) "محفوظ" else "Saved"
    val retry = if (isArabic) "إعادة المحاولة" else "Retry"
    val comingSoon = if (isArabic) "قريبًا" else "Soon"
    val doneBadge = if (isArabic) "تم" else "Ready"
    val siteBuilder = if (isArabic) "إنشاء المواقع" else "Site Builder"
    val siteDeploy = if (isArabic) "إنشاء ونشر موقع" else "Build & Deploy Site"
    val apkStudio = if (isArabic) "استوديو تطبيقات أندرويد" else "Android APK Studio"
    val projectBrain = if (isArabic) "عقل المشروع" else "Project Brain"
    val unifiedEngine = if (isArabic) "المحرّك الموحّد" else "Unified Engine"
    val memory = if (isArabic) "الذاكرة" else "Memory"
    val security = if (isArabic) "الأمان وتسجيل الدخول" else "Security & Sign-in"
    val archiveTitle = if (isArabic) "الأرشيف" else "Archive"
    val lockedChatsTitle = if (isArabic) "الدردشات المقفلة" else "Locked Chats"
    val pinnedChatsTitle = if (isArabic) "المحادثات المثبتة" else "Pinned Chats"
    val appGuideTitle = if (isArabic) "دليل التطبيق الشامل" else "App Guide"
    val continueWithoutSignIn = if (isArabic) "المتابعة بدون تسجيل دخول" else "Continue without sign-in"
    val guestUserLabel = if (isArabic) "زائر Nux (بدون حساب)" else "Nux Guest (No sign-in)"
    val guestNotice = if (isArabic) {
        "أنت تستخدم التطبيق بدون تسجيل دخول. ميزات الذكاء الاصطناعي والصور متاحة بالكامل، وتُحفظ محادثاتك محليًا لهذه الجلسة. سجّل الدخول بحساب Google للمزامنة السحابية."
    } else {
        "Using Nux without signing in. AI chat and studios work live; chats are kept locally for this session. Sign in with Google for cloud sync."
    }
    val exitGuestMode = if (isArabic) "العودة لشاشة الدخول" else "Back to sign-in screen"
}

fun normalizeArabic(input: String): String {
    return input
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ة', 'ه')
        .replace('ى', 'ي')
        .replace(Regex("[\\u064B-\\u065F]"), "")
        .lowercase()
        .trim()
}
