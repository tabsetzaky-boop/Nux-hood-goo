package com.example.data.model

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val preferredName: String = "",
    val customInstructions: String = "",
    val gender: String = "unspecified",
    val age: Long = 24L,
    val liveVoice: String = "Aoede",
    val profileCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.ifBlank { "مستخدم Nux" }.take(120),
        "email" to email.take(200),
        "photoUrl" to photoUrl?.take(1000),
        "preferredName" to preferredName.take(60),
        "customInstructions" to customInstructions.take(2000),
        "gender" to gender,
        "age" to age.coerceIn(16L, 100L),
        "liveVoice" to liveVoice.take(40),
        "profileCompleted" to profileCompleted,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "محادثة جديدة",
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val locked: Boolean = false,
    val projectId: String? = null,
    val modelName: String = "gemini-3.8-flash",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.ifBlank { "محادثة جديدة" }.take(160),
        "pinned" to pinned,
        "archived" to archived,
        "locked" to locked,
        "projectId" to projectId?.take(128),
        "modelName" to modelName.take(80),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "user",
    val content: String = "",
    val thinking: String? = null,
    val sources: List<String> = emptyList(),
    val model: String? = null,
    val attachmentNames: List<String> = emptyList(),
    val imageLocalPaths: List<String> = emptyList(),
    val audioTranscript: String? = null,
    val feedback: String? = null,
    val feedbackNote: String? = null,
    val followUpSuggestions: List<String> = emptyList(),
    val webSearch: Boolean = false,
    val errorFlag: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "conversationId" to conversationId,
        "role" to role,
        "content" to content.take(40000),
        "thinking" to thinking?.take(20000),
        "sources" to sources.take(20),
        "model" to model?.take(80),
        "attachmentNames" to attachmentNames.take(10),
        "imageLocalPaths" to imageLocalPaths.take(10),
        "audioTranscript" to audioTranscript?.take(8000),
        "feedback" to feedback,
        "feedbackNote" to feedbackNote?.take(500),
        "followUpSuggestions" to followUpSuggestions.take(6),
        "webSearch" to webSearch,
        "errorFlag" to errorFlag,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconEmoji: String = "📁",
    val description: String = "",
    val instructions: String = "",
    val pinned: Boolean = false,
    val buildKind: String = "general",
    val appName: String? = null,
    val packageName: String? = null,
    val permissions: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.take(100),
        "iconEmoji" to iconEmoji.ifBlank { "📁" }.take(16),
        "description" to description.take(500),
        "instructions" to instructions.take(2000),
        "pinned" to pinned,
        "buildKind" to buildKind.take(32),
        "appName" to appName?.take(80),
        "packageName" to packageName?.take(120),
        "permissions" to permissions.take(20),
        "features" to features.take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class MemoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val content: String = "",
    val category: String = "شخصي",
    val enabled: Boolean = true,
    val source: String = "manual",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "content" to content.take(240),
        "category" to category,
        "enabled" to enabled,
        "source" to source.take(40),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class GeneratedImageRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val aspectRatio: String = "1:1",
    val style: String = "واقعي",
    val model: String = "gemini-nano-banana-2.1",
    val localFileName: String = "",
    val conversationId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "prompt" to prompt.take(2000),
        "aspectRatio" to aspectRatio.take(16),
        "style" to style.take(40),
        "model" to model.take(80),
        "localFileName" to localFileName.take(200),
        "conversationId" to conversationId?.take(128),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class SiteBuild(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val prompt: String = "",
    val htmlCode: String = "",
    val published: Boolean = false,
    val versionCount: Long = 1L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.ifBlank { "موقع Nux" }.take(120),
        "description" to description.take(500),
        "prompt" to prompt.take(1500),
        "htmlCode" to htmlCode.take(300000),
        "published" to published,
        "versionCount" to versionCount.coerceIn(1L, 1000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class NativeAppSpec(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val idea: String = "",
    val packageName: String = "com.nux.app",
    val status: String = "جاهز",
    val specSummary: String = "",
    val permissions: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val secretKeys: List<String> = emptyList(),
    val versionName: String = "1.0.0",
    val versionCode: Long = 1L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.ifBlank { "تطبيق جديد" }.take(100),
        "idea" to idea.take(1500),
        "packageName" to packageName.take(120),
        "status" to status.take(32),
        "specSummary" to specSummary.take(30000),
        "permissions" to permissions.take(20),
        "features" to features.take(30),
        "secretKeys" to secretKeys.take(20),
        "versionName" to versionName.take(32),
        "versionCode" to versionCode.coerceIn(1L, 10000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class SecurityPinDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = "login",
    val pinHash: String = "",
    val salt: String = "",
    val enabled: Boolean = false,
    val trustedDevices: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "kind" to kind,
        "pinHash" to pinHash,
        "salt" to salt,
        "enabled" to enabled,
        "trustedDevices" to trustedDevices.take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

// UI & AI Domain Helpers

enum class ComposerMode(val key: String, val arLabel: String, val enLabel: String) {
    CHAT("chat", "محادثة", "Chat"),
    IMAGE("image", "إنشاء صورة", "Create image"),
    AUDIO("audio", "إنشاء ملف صوتي", "Create audio")
}

enum class ImageStyleOption(val arLabel: String, val enLabel: String, val promptSuffix: String) {
    REALISTIC("واقعي", "Realistic", "photorealistic, ultra detailed, natural lighting"),
    ANIME("أنمي", "Anime", "vibrant modern anime art style, crisp linework"),
    CARTOON("كرتون", "Cartoon", "stylized 3D Pixar-inspired cartoon render, expressive"),
    THREE_D("3D", "3D", "3D octane render, soft studio lighting, sleek materials"),
    SKETCH("رسم يدوي", "Sketch", "hand-drawn artistic ink and watercolor sketch"),
    CINEMATIC("سينمائي", "Cinematic", "cinematic film still, dramatic lighting, anamorphic lens")
}

enum class AspectRatioOption(val label: String) {
    SQUARE("1:1"),
    PORTRAIT("3:4"),
    LANDSCAPE("16:9")
}

data class AttachmentItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isImage: Boolean,
    val isAudio: Boolean = false,
    val bitmap: Bitmap? = null,
    val localPath: String? = null,
    val textSnippet: String? = null,
    val uri: Uri? = null,
)

data class MentionTool(
    val id: String,
    val arName: String,
    val enName: String,
    val arDescription: String,
    val enDescription: String,
    val directive: String = "",
    val isComingSoon: Boolean = false,
    val opensRoute: String? = null,
    val forcesImageMode: Boolean = false,
    val imageStyle: ImageStyleOption? = null,
    val forcesWebSearch: Boolean = false,
    val forcesDeepThinking: Boolean = false,
)

data class NuxModelOption(
    val id: String,
    val title: String,
    val arSubtitle: String,
    val enSubtitle: String,
)

data class LiveVoiceOption(
    val id: String,
    val arName: String,
    val enName: String,
    val pitch: Float,
    val speed: Float,
)

data class ExploreIdea(
    val id: String,
    val categoryAr: String,
    val categoryEn: String,
    val titleAr: String,
    val titleEn: String,
    val promptAr: String,
    val promptEn: String,
    val accentIndex: Int,
)

data class RemoteStepItem(
    val title: String,
    val commandHint: String,
    val requiresApproval: Boolean,
    val status: String = "pending"
)

object NuxCatalog {
    val models = listOf(
        NuxModelOption("gemini-3.8-flash", "Flash 3.8", "الأسرع والأكثر توازنًا افتراضيًا", "Fastest all-round default"),
        NuxModelOption("gemini-3.5-flash-lite", "Flash Lite", "خفيف وفوري للمهام السريعة", "Ultra-fast for quick tasks"),
        NuxModelOption("gemini-4-argon", "Argon 4", "استدلال عميق وبرمجة متقدمة", "Complex reasoning & coding")
    )

    val liveVoices = listOf(
        LiveVoiceOption("Puck", "بَك (Puck)", "Puck", 1.05f, 1.02f),
        LiveVoiceOption("Charon", "كارون (Charon)", "Charon", 0.88f, 0.96f),
        LiveVoiceOption("Kore", "كوري (Kore)", "Kore", 1.12f, 1.0f),
        LiveVoiceOption("Fenrir", "فنرير (Fenrir)", "Fenrir", 0.82f, 0.95f),
        LiveVoiceOption("Aoede", "أويدي (Aoede)", "Aoede", 1.0f, 1.0f),
        LiveVoiceOption("Leda", "ليدا (Leda)", "Leda", 1.08f, 0.98f),
        LiveVoiceOption("Orus", "أوروس (Orus)", "Orus", 0.92f, 1.03f),
        LiveVoiceOption("Zephyr", "زيفير (Zephyr)", "Zephyr", 1.1f, 1.04f),
    )

    val mentionTools = listOf(
        MentionTool("images", "صور", "Images", "إنشاء صورة بالذكاء الاصطناعي", "Generate an AI image", forcesImageMode = true),
        MentionTool("stickers", "ملصقات", "Stickers", "ملصق نظيف معزول", "Clean isolated sticker", "Create a clean die-cut sticker design with bold outlines", forcesImageMode = true),
        MentionTool("cartoon", "كرتون", "Cartoon", "صورة كرتونية 3D", "3D cartoon artwork", forcesImageMode = true, imageStyle = ImageStyleOption.CARTOON),
        MentionTool("anime", "أنمي", "Anime", "رسم بأسلوب الأنمي", "Anime illustration", forcesImageMode = true, imageStyle = ImageStyleOption.ANIME),
        MentionTool("design", "تصميم", "Design", "شعار أو بوستر احترافي", "Logo or poster design", "Professional graphic design, balanced typography and composition", forcesImageMode = true),
        MentionTool("video", "فيديو", "Video", "إنشاء فيديو قصير", "Generate short video", isComingSoon = true),
        MentionTool("search", "بحث", "Search", "بحث معمّق مع ذكر المصادر", "Search with sources", forcesWebSearch = true),
        MentionTool("writing", "كتابة", "Writing", "مساعد كتابة احترافي", "Pro writing assistant", "Act as an elite editor and creative writer."),
        MentionTool("coding", "برمجة · Codex", "Coding · Codex", "مهندس برمجيات وكود كامل", "Software engineer & full code", "Act as a senior software engineer. Output clean, complete, production code inside fenced code blocks."),
        MentionTool("files", "ملفات", "Files", "تحليل المستندات المرفقة", "Analyze attached files", "Analyze every attached file thoroughly and extract key insights."),
        MentionTool("analysis", "تحليل", "Analysis", "تحليل صور أو بيانات", "Analyze image or data", "Provide structured, analytical breakdown with clear bullet points."),
        MentionTool("thinking", "تفكير", "Thinking", "تفكير عميق خطوة بخطوة", "Step-by-step deep reasoning", forcesDeepThinking = true),
        MentionTool("site", "موقع", "Website", "افتح استوديو بناء المواقع", "Open Site Builder", opensRoute = NuxRoutes.DEPLOY),
        MentionTool("app", "تطبيق", "Android App", "افتح استوديو تطبيقات أندرويد", "Open APK Studio", opensRoute = NuxRoutes.APK),
        MentionTool("remote", "تحكم عن بعد", "Remote", "افتح التحكم عن بُعد", "Open Remote Control", opensRoute = NuxRoutes.REMOTE),
        MentionTool("translate", "ترجمة", "Translate", "ترجمة دقيقة بلا شرح زائد", "Accurate direct translation", "Translate accurately and naturally without unnecessary commentary.")
    )

    val exploreIdeas = listOf(
        ExploreIdea("1", "إبداع بصري", "Visual Art", "مدينة عربية مستقبلية", "Futuristic Arab City", "صمم صورة سينمائية لمدينة عربية مستقبلية وقت الغروب بأسلوب نيون وأبراج زجاجية", "Design a cinematic futuristic Middle Eastern city at sunset with neon glass towers", 0),
        ExploreIdea("2", "إنتاجية", "Productivity", "خطة أسبوعية مركّزة", "Focused Weekly Plan", "ساعدني في بناء خطة أسبوعية متوازنة للعمل والتعلم والرياضة في جدول واضح", "Help me build a balanced weekly schedule for work, learning, and fitness", 1),
        ExploreIdea("3", "برمجة", "Coding", "مراجعة وتحسين كود", "Code Review & Refactor", "راجع كود Kotlin التالي واقترح تحسينات للأداء والنظافة المعمارية", "Review my architecture and suggest performance and clean-code improvements", 2),
        ExploreIdea("4", "كتابة", "Writing", "بريد احترافي مقنع", "Persuasive Email", "اكتب رسالة بريد إلكتروني مهذبة ومقنعة لاقتراح شراكة تقنية جديدة", "Write a polished, persuasive email proposing a new tech partnership", 3),
        ExploreIdea("5", "تعليم", "Learning", "اشرح لي ببساطة", "Explain Simply", "اشرح لي مفهوم الحوسبة الكمومية بأسلوب مبسط مع مثال من الحياة اليومية", "Explain quantum computing simply using an everyday analogy", 4),
        ExploreIdea("6", "تصميم", "Design", "شعار متجر قهوة", "Coffee Brand Logo", "ارسم شعارًا بسيطًا وأنيقًا لمحمصة قهوة مختصة بخطوط ذهبية على خلفية داكنة", "Draw a minimalist luxury logo for a specialty coffee roastery on a dark background", 5),
        ExploreIdea("7", "سفر", "Travel", "برنامج سياحي ٥ أيام", "5-Day Itinerary", "اقترح برنامجًا سياحيًا متكاملًا لمدة 5 أيام يشمل الأكل الشعبي والمعالم الخفية", "Suggest a complete 5-day travel itinerary with local food and hidden gems", 6),
        ExploreIdea("8", "أعمال", "Business", "تحليل فكرة مشروع", "Startup Analysis", "حلل فكرة تطبيق خدمات محلية: الميزة التنافسية، المخاطر، وخطة الإطلاق الأولى", "Analyze a local services app idea: moat, risks, and MVP launch plan", 0),
    )

    val memoryCategories = listOf("شخصي", "تفضيلات", "عمل", "أخرى")
}

object NuxRoutes {
    const val CHAT = "chat"
    const val CHAT_WITH_ID = "chat/{conversationId}"
    const val SEARCH = "search"
    const val IMAGES = "images"
    const val LIBRARY = "library"
    const val FILES = "files"
    const val PROJECTS = "projects"
    const val CUSTOMIZE = "customize"
    const val EXPLORE = "explore"
    const val REMOTE = "remote"
    const val MEMORY = "memory"
    const val SECURITY = "security"
    const val ARCHIVE = "archive"
    const val LOCKED = "locked"
    const val PINNED = "pinned"
    const val SETTINGS = "settings"
    const val GUIDE = "guide"
    const val LEGAL = "legal/{doc}"
    const val PROFILE = "profile"
    const val BUILDER = "builder"
    const val DEPLOY = "deploy"
    const val APK = "apk"
    const val BRAIN = "brain"
    const val ENGINE = "engine"

    fun chatRoute(conversationId: String) = "chat/$conversationId"
    fun legalRoute(doc: String) = "legal/$doc"
}
