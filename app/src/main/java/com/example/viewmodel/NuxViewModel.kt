package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.core.AppLanguage
import com.example.core.BubbleShape
import com.example.core.BubbleTone
import com.example.core.MediaUtils
import com.example.core.NuxPreferencesRepository
import com.example.core.NuxUserPrefs
import com.example.core.PinCrypto
import com.example.core.ThemeMode
import com.example.data.firebase.NuxAiRepository
import com.example.data.firebase.NuxRepository
import com.example.data.model.AspectRatioOption
import com.example.data.model.AttachmentItem
import com.example.data.model.ChatMessage
import com.example.data.model.ComposerMode
import com.example.data.model.Conversation
import com.example.data.model.GeneratedImageRecord
import com.example.data.model.ImageStyleOption
import com.example.data.model.MemoryItem
import com.example.data.model.MentionTool
import com.example.data.model.NativeAppSpec
import com.example.data.model.NuxCatalog
import com.example.data.model.Project
import com.example.data.model.RemoteStepItem
import com.example.data.model.SecurityPinDoc
import com.example.data.model.SiteBuild
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

private const val TAG = "NuxViewModel"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class NuxToast(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val isError: Boolean = false
)

class NuxViewModel(
    private val appContext: Application,
    private val repo: NuxRepository,
    private val aiRepo: NuxAiRepository,
    val prefsRepo: NuxPreferencesRepository,
    val currentUserId: String
) : ViewModel(), TextToSpeech.OnInitListener {

    val isGuestMode: Boolean = currentUserId == GUEST_USER_ID

    // Guest-mode in-memory stores (never touch Firestore when unauthenticated)
    private val _guestProfile = MutableStateFlow(
        UserProfile(
            id = GUEST_USER_ID,
            userId = GUEST_USER_ID,
            displayName = "زائر Nux",
            email = "",
            preferredName = "",
            customInstructions = "",
            liveVoice = "Aoede",
            profileCompleted = true
        )
    )
    private val _guestConversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _guestMessagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val _guestProjects = MutableStateFlow<List<Project>>(emptyList())
    private val _guestMemory = MutableStateFlow<List<MemoryItem>>(emptyList())
    private val _guestImages = MutableStateFlow<List<GeneratedImageRecord>>(emptyList())
    private val _guestSites = MutableStateFlow<List<SiteBuild>>(emptyList())
    private val _guestNativeApps = MutableStateFlow<List<NativeAppSpec>>(emptyList())

    val prefs: StateFlow<NuxUserPrefs> = prefsRepo.prefsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = NuxUserPrefs()
    )

    val profileState: StateFlow<UiState<UserProfile?>> =
        (if (isGuestMode) _guestProfile else repo.observeProfile(currentUserId))
            .asUiState("تعذّر تحميل الملف الشخصي")

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        (if (isGuestMode) _guestConversations else repo.observeConversations(currentUserId))
            .asUiState("تعذّر تحميل المحادثات")

    val projectsState: StateFlow<UiState<List<Project>>> =
        (if (isGuestMode) _guestProjects else repo.observeProjects(currentUserId))
            .asUiState("تعذّر تحميل المشاريع")

    val memoryState: StateFlow<UiState<List<MemoryItem>>> =
        (if (isGuestMode) _guestMemory else repo.observeMemory(currentUserId))
            .asUiState("تعذّر تحميل الذاكرة")

    val generatedImagesState: StateFlow<UiState<List<GeneratedImageRecord>>> =
        (if (isGuestMode) _guestImages else repo.observeGeneratedImages(currentUserId))
            .asUiState("تعذّر تحميل الصور")

    val siteBuildsState: StateFlow<UiState<List<SiteBuild>>> =
        (if (isGuestMode) _guestSites else repo.observeSiteBuilds(currentUserId))
            .asUiState("تعذّر تحميل المواقع")

    val nativeAppsState: StateFlow<UiState<List<NativeAppSpec>>> =
        (if (isGuestMode) _guestNativeApps else repo.observeNativeApps(currentUserId))
            .asUiState("تعذّر تحميل مشاريع التطبيقات")

    val securityPinsState: StateFlow<UiState<List<SecurityPinDoc>>> =
        (if (isGuestMode) flowOf(emptyList()) else repo.observeSecurityPins(currentUserId))
            .asUiState("تعذّر تحميل إعدادات الأمان")

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(emptyList())
            } else if (isGuestMode) {
                _guestMessagesMap.map { it[convId].orEmpty() }
            } else {
                repo.observeMessages(currentUserId, convId)
            }
        }
        .asUiState("تعذّر تحميل الرسائل")

    // Ephemeral / Temp chat
    private val _isTempChat = MutableStateFlow(false)
    val isTempChat: StateFlow<Boolean> = _isTempChat.asStateFlow()

    private val _tempMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val tempMessages: StateFlow<List<ChatMessage>> = _tempMessages.asStateFlow()

    // Composer state
    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _composerMode = MutableStateFlow(ComposerMode.CHAT)
    val composerMode: StateFlow<ComposerMode> = _composerMode.asStateFlow()

    private val _imageStyle = MutableStateFlow(ImageStyleOption.REALISTIC)
    val imageStyle: StateFlow<ImageStyleOption> = _imageStyle.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioOption.SQUARE)
    val aspectRatio: StateFlow<AspectRatioOption> = _aspectRatio.asStateFlow()

    private val _attachments = MutableStateFlow<List<AttachmentItem>>(emptyList())
    val attachments: StateFlow<List<AttachmentItem>> = _attachments.asStateFlow()

    private val _activeMentionTool = MutableStateFlow<MentionTool?>(null)
    val activeMentionTool: StateFlow<MentionTool?> = _activeMentionTool.asStateFlow()

    private val _webSearchActive = MutableStateFlow(false)
    val webSearchActive: StateFlow<Boolean> = _webSearchActive.asStateFlow()

    private val _deepThinkingActive = MutableStateFlow(false)
    val deepThinkingActive: StateFlow<Boolean> = _deepThinkingActive.asStateFlow()

    private val _replyImageAttachment = MutableStateFlow<AttachmentItem?>(null)
    val replyImageAttachment: StateFlow<AttachmentItem?> = _replyImageAttachment.asStateFlow()

    // Streaming & Generation state
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingThinking = MutableStateFlow("")
    val streamingThinking: StateFlow<String> = _streamingThinking.asStateFlow()

    private val _streamingAnswer = MutableStateFlow("")
    val streamingAnswer: StateFlow<String> = _streamingAnswer.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _lastFailedImagePrompt = MutableStateFlow<String?>(null)
    val lastFailedImagePrompt: StateFlow<String?> = _lastFailedImagePrompt.asStateFlow()

    private var streamJob: Job? = null
    private var draftSaveJob: Job? = null

    // Security unlocked sets (session-only)
    private val _unlockedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedChatIds: StateFlow<Set<String>> = _unlockedChatIds.asStateFlow()

    private val _loginPinVerified = MutableStateFlow(false)
    val loginPinVerified: StateFlow<Boolean> = _loginPinVerified.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    // Toast
    private val _toast = MutableStateFlow<NuxToast?>(null)
    val toast: StateFlow<NuxToast?> = _toast.asStateFlow()

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    // Studio & Engine states
    private val _isBuildingSite = MutableStateFlow(false)
    val isBuildingSite: StateFlow<Boolean> = _isBuildingSite.asStateFlow()

    private val _isPlanningApk = MutableStateFlow(false)
    val isPlanningApk: StateFlow<Boolean> = _isPlanningApk.asStateFlow()

    private val _remoteSteps = MutableStateFlow<List<RemoteStepItem>>(emptyList())
    val remoteSteps: StateFlow<List<RemoteStepItem>> = _remoteSteps.asStateFlow()

    private val _isPlanningRemote = MutableStateFlow(false)
    val isPlanningRemote: StateFlow<Boolean> = _isPlanningRemote.asStateFlow()

    private val _engineProbeResult = MutableStateFlow<String?>(null)
    val engineProbeResult: StateFlow<String?> = _engineProbeResult.asStateFlow()

    init {
        viewModelScope.launch {
            _deviceId.value = prefsRepo.ensureDeviceId()
        }
        viewModelScope.launch {
            val initialPrefs = prefs.value
            _webSearchActive.value = initialPrefs.webSearchDefault
            _deepThinkingActive.value = initialPrefs.deepThinkingDefault
        }
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        ttsReady = (status == TextToSpeech.SUCCESS)
        if (ttsReady) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    _speakingMessageId.value = null
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _speakingMessageId.value = null
                }
            })
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    fun showToast(message: String, isError: Boolean = false) {
        val t = NuxToast(message = message, isError = isError)
        _toast.value = t
        viewModelScope.launch {
            delay(3200L)
            if (_toast.value?.id == t.id) {
                _toast.value = null
            }
        }
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun ensureProfileExists(displayName: String, email: String, photoUrl: String?) {
        if (isGuestMode) return
        viewModelScope.launch {
            repo.ensureUserProfile(currentUserId, displayName, email, photoUrl)
        }
    }

    fun exitGuestMode() {
        viewModelScope.launch {
            prefsRepo.setGuestMode(false)
        }
    }

    fun completeOnboarding(gender: String, age: Long) {
        val voice = when (gender) {
            "male" -> "Charon"
            "female" -> "Zephyr"
            else -> "Aoede"
        }
        if (isGuestMode) {
            _guestProfile.value = _guestProfile.value.copy(
                gender = gender,
                age = age.coerceIn(16L, 100L),
                liveVoice = voice,
                profileCompleted = true
            )
            viewModelScope.launch { prefsRepo.setLiveVoice(voice) }
            showToast("تم حفظ بياناتك بنجاح")
            return
        }
        viewModelScope.launch {
            repo.completeOnboarding(gender, age, voice)
                .onSuccess {
                    prefsRepo.setLiveVoice(voice)
                    showToast("تم حفظ بياناتك بنجاح")
                }
                .onFailure { showToast(it.localizedMessage ?: "تعذّر الحفظ", true) }
        }
    }

    // --- Navigation & Chat Selection ---

    fun startNewChat(projectId: String? = null, initialQuery: String? = null) {
        stopGeneration()
        _activeConversationId.value = null
        _activeProjectId.value = projectId
        _isTempChat.value = false
        _tempMessages.value = emptyList()
        _chatError.value = null
        _replyImageAttachment.value = null
        if (!initialQuery.isNullOrBlank()) {
            _composerText.value = initialQuery.take(4000)
        }
    }

    fun openConversation(conversationId: String) {
        stopGeneration()
        _isTempChat.value = false
        _activeConversationId.value = conversationId
        _chatError.value = null
    }

    fun toggleTempChat() {
        stopGeneration()
        val next = !_isTempChat.value
        _isTempChat.value = next
        _activeConversationId.value = null
        _tempMessages.value = emptyList()
        _chatError.value = null
        showToast(if (next) "تم تفعيل المحادثة المؤقتة" else "تم إنهاء المحادثة المؤقتة")
    }

    // --- Composer Updates ---

    fun updateComposerText(text: String) {
        val clipped = text.take(4000)
        _composerText.value = clipped
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(350L)
            prefsRepo.saveDraft(_composerMode.value.key, clipped)
        }
    }

    fun setComposerMode(mode: ComposerMode) {
        _composerMode.value = mode
    }

    fun setImageStyle(style: ImageStyleOption) {
        _imageStyle.value = style
    }

    fun setAspectRatio(ratio: AspectRatioOption) {
        _aspectRatio.value = ratio
    }

    fun toggleWebSearch() {
        _webSearchActive.value = !_webSearchActive.value
    }

    fun toggleDeepThinking() {
        _deepThinkingActive.value = !_deepThinkingActive.value
    }

    fun selectMentionTool(tool: MentionTool?) {
        _activeMentionTool.value = tool
        if (tool != null) {
            if (tool.forcesImageMode) _composerMode.value = ComposerMode.IMAGE
            if (tool.imageStyle != null) _imageStyle.value = tool.imageStyle
            if (tool.forcesWebSearch) _webSearchActive.value = true
            if (tool.forcesDeepThinking) _deepThinkingActive.value = true
        }
    }

    fun setReplyToImage(attachment: AttachmentItem?) {
        _replyImageAttachment.value = attachment
        if (attachment != null) {
            _composerMode.value = ComposerMode.IMAGE
        }
    }

    fun addAttachmentsFromUris(uris: List<Uri>, asImages: Boolean) {
        viewModelScope.launch {
            val current = _attachments.value.toMutableList()
            for (uri in uris) {
                if (current.size >= 10) {
                    showToast("الحد الأقصى 10 مرفقات في الرسالة", true)
                    break
                }
                val (name, mime, size) = withContext(Dispatchers.IO) {
                    MediaUtils.getFileMeta(appContext, uri)
                }
                val isImg = asImages || mime.startsWith("image/")
                val isAudio = mime.startsWith("audio/")
                if (isImg && size > MediaUtils.MAX_IMAGE_BYTES) {
                    showToast("الحد الأقصى لحجم الصورة 6 ميجابايت", true)
                    continue
                }
                if (!isImg && size > MediaUtils.MAX_FILE_BYTES) {
                    showToast("الحد الأقصى لحجم الملف 8 ميجابايت", true)
                    continue
                }
                val bmp = if (isImg) {
                    withContext(Dispatchers.IO) { MediaUtils.decodeScaledBitmap(appContext, uri) }
                } else null
                val localPath = if (bmp != null) {
                    withContext(Dispatchers.IO) { MediaUtils.saveBitmapLocally(appContext, bmp, "att") }
                } else null
                val snippet = if (!isImg && !isAudio) {
                    withContext(Dispatchers.IO) { MediaUtils.readTextFileSnippet(appContext, uri) }
                } else null

                current.add(
                    AttachmentItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        mimeType = mime,
                        sizeBytes = size,
                        isImage = isImg,
                        isAudio = isAudio,
                        bitmap = bmp,
                        localPath = localPath,
                        textSnippet = snippet,
                        uri = uri
                    )
                )
            }
            _attachments.value = current
        }
    }

    fun addCapturedCameraImage(filePath: String, uri: Uri) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) { MediaUtils.decodeScaledBitmap(appContext, uri) }
            if (bmp == null) {
                showToast("تعذّر قراءة الصورة الملتقطة", true)
                return@launch
            }
            val saved = withContext(Dispatchers.IO) { MediaUtils.saveBitmapLocally(appContext, bmp, "cam") }
            val current = _attachments.value.toMutableList()
            if (current.size < 10) {
                current.add(
                    AttachmentItem(
                        id = UUID.randomUUID().toString(),
                        name = "صورة الكاميرا",
                        mimeType = "image/jpeg",
                        sizeBytes = 0L,
                        isImage = true,
                        bitmap = bmp,
                        localPath = saved,
                        uri = uri
                    )
                )
                _attachments.value = current
            }
        }
    }

    fun removeAttachment(id: String) {
        _attachments.value = _attachments.value.filterNot { it.id == id }
    }

    // --- Sending & Streaming ---

    fun sendCurrentMessage(overridePrompt: String? = null) {
        if (_isStreaming.value) return
        val promptText = (overridePrompt ?: _composerText.value).trim()
        val currentAtts = _attachments.value
        val replyImg = _replyImageAttachment.value

        if (promptText.isBlank() && currentAtts.isEmpty() && replyImg == null) return

        // Clear composer immediately
        if (overridePrompt == null) {
            _composerText.value = ""
            viewModelScope.launch { prefsRepo.saveDraft(_composerMode.value.key, "") }
        }
        _attachments.value = emptyList()
        _replyImageAttachment.value = null
        _chatError.value = null
        _lastFailedImagePrompt.value = null

        val isArabicAutoImage = promptText.startsWith("أنشئ صورة") ||
            promptText.startsWith("ارسم") ||
            promptText.startsWith("صمم صورة") ||
            promptText.lowercase().startsWith("generate image")

        val effectiveMode = if (isArabicAutoImage && _composerMode.value == ComposerMode.CHAT) {
            ComposerMode.IMAGE
        } else {
            _composerMode.value
        }

        // Auto-detect aspect ratio from Arabic/English prompt hints
        val effectiveRatio = when {
            promptText.contains("ستوري") || promptText.contains("عمودي") -> AspectRatioOption.PORTRAIT
            promptText.contains("عريض") || promptText.contains("أفقي") -> AspectRatioOption.LANDSCAPE
            promptText.contains("مربع") -> AspectRatioOption.SQUARE
            else -> _aspectRatio.value
        }

        streamJob = viewModelScope.launch {
            val isTemp = _isTempChat.value
            val currentPrefs = prefs.value
            val modelId = currentPrefs.selectedModelId

            var convId = _activeConversationId.value
            if (!isTemp && convId.isNullOrBlank()) {
                val title = promptText.ifBlank { "محادثة جديدة" }.take(45)
                convId = createConversationInternal(
                    title = title,
                    projectId = _activeProjectId.value,
                    modelName = modelId
                ).getOrElse { err ->
                    _chatError.value = err.localizedMessage ?: "تعذّر إنشاء المحادثة"
                    return@launch
                }
                _activeConversationId.value = convId
            }

            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                conversationId = convId ?: "temp",
                role = "user",
                content = promptText,
                attachmentNames = currentAtts.map { it.name },
                imageLocalPaths = currentAtts.mapNotNull { it.localPath },
                webSearch = _webSearchActive.value
            )

            if (isTemp) {
                _tempMessages.value = _tempMessages.value + userMsg
            } else {
                addMessageInternal(convId!!, userMsg)
            }

            when (effectiveMode) {
                ComposerMode.IMAGE -> {
                    _isStreaming.value = true
                    _streamingThinking.value = ""
                    _streamingAnswer.value = if (currentPrefs.appLanguage.isRtl) "جارٍ رسم الصورة..." else "Generating image..."
                    try {
                        val refBmp = replyImg?.bitmap ?: currentAtts.firstOrNull { it.isImage }?.bitmap
                        val generatedBitmap = aiRepo.generateOrEditImage(
                            prompt = promptText.ifBlank { "صورة إبداعية" },
                            style = _imageStyle.value,
                            aspectRatio = effectiveRatio,
                            referenceBitmap = refBmp
                        )
                        val savedPath = withContext(Dispatchers.IO) {
                            MediaUtils.saveBitmapLocally(appContext, generatedBitmap, "nux_gen")
                        }
                        if (!isTemp && convId != null) {
                            addGeneratedImageInternal(
                                GeneratedImageRecord(
                                    prompt = promptText.ifBlank { "صورة إبداعية" },
                                    aspectRatio = effectiveRatio.label,
                                    style = _imageStyle.value.arLabel,
                                    model = "gemini-nano-banana-2.1",
                                    localFileName = savedPath,
                                    conversationId = convId
                                )
                            )
                        }
                        val assistantMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            userId = currentUserId,
                            conversationId = convId ?: "temp",
                            role = "assistant",
                            content = if (currentPrefs.appLanguage.isRtl) {
                                "تم إنشاء الصورة بنجاح: **$promptText**"
                            } else {
                                "Image generated: **$promptText**"
                            },
                            model = "gemini-nano-banana-2.1",
                            imageLocalPaths = listOf(savedPath)
                        )
                        if (isTemp) {
                            _tempMessages.value = _tempMessages.value + assistantMsg
                        } else {
                            addMessageInternal(convId!!, assistantMsg)
                        }
                        _composerMode.value = ComposerMode.CHAT
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val msg = e.localizedMessage ?: "خدمة الصور مشغولة الآن."
                        _chatError.value = msg
                        _lastFailedImagePrompt.value = promptText
                    } finally {
                        _isStreaming.value = false
                        _streamingAnswer.value = ""
                        _streamingThinking.value = ""
                    }
                }

                ComposerMode.AUDIO -> {
                    // Generate response text and immediately speak it via TTS
                    streamTextReply(
                        convId = convId,
                        isTemp = isTemp,
                        promptText = promptText,
                        attachments = currentAtts,
                        speakAfter = true
                    )
                    _composerMode.value = ComposerMode.CHAT
                }

                ComposerMode.CHAT -> {
                    streamTextReply(
                        convId = convId,
                        isTemp = isTemp,
                        promptText = promptText,
                        attachments = currentAtts,
                        speakAfter = false
                    )
                }
            }
        }
    }

    private suspend fun streamTextReply(
        convId: String?,
        isTemp: Boolean,
        promptText: String,
        attachments: List<AttachmentItem>,
        speakAfter: Boolean
    ) {
        val currentPrefs = prefs.value
        val profile = (profileState.value as? UiState.Success)?.data
        val projects = (projectsState.value as? UiState.Success)?.data.orEmpty()
        val activeProject = projects.firstOrNull { it.id == _activeProjectId.value }
        val memories = if (isTemp && !currentPrefs.tempChatCustomized) {
            emptyList()
        } else if (currentPrefs.useMemory) {
            (memoryState.value as? UiState.Success)?.data.orEmpty()
        } else {
            emptyList()
        }

        val history = if (isTemp) {
            _tempMessages.value.dropLast(1)
        } else {
            ((activeMessagesState.value as? UiState.Success)?.data.orEmpty()).dropLast(1)
        }

        val customInstr = if (isTemp && !currentPrefs.tempChatCustomized) "" else (profile?.customInstructions ?: "")
        val prefName = if (isTemp && !currentPrefs.tempChatCustomized) "" else (profile?.preferredName ?: "")

        _isStreaming.value = true
        _streamingThinking.value = ""
        _streamingAnswer.value = ""

        var finalThinking = ""
        var finalAnswer = ""

        try {
            aiRepo.streamChatReply(
                modelId = currentPrefs.selectedModelId,
                history = history,
                userPrompt = promptText,
                attachments = attachments,
                preferredName = prefName,
                customInstructions = customInstr,
                projectInstructions = activeProject?.instructions ?: "",
                memories = memories,
                replyLanguageCode = currentPrefs.replyLanguage,
                deepThinking = _deepThinkingActive.value,
                webSearchMode = _webSearchActive.value,
                toolDirective = _activeMentionTool.value?.directive ?: "",
                uiIsArabic = currentPrefs.appLanguage.isRtl
            ).collect { update ->
                finalThinking = update.thinkingText
                finalAnswer = update.answerText
                _streamingThinking.value = update.thinkingText
                _streamingAnswer.value = update.answerText
            }

            val followUps = if (finalAnswer.isNotBlank()) {
                aiRepo.generateFollowUps(promptText, finalAnswer, currentPrefs.appLanguage.isRtl)
            } else emptyList()

            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                conversationId = convId ?: "temp",
                role = "assistant",
                content = finalAnswer,
                thinking = finalThinking.takeIf { it.isNotBlank() },
                model = currentPrefs.selectedModelId,
                followUpSuggestions = followUps,
                webSearch = _webSearchActive.value
            )

            if (isTemp) {
                _tempMessages.value = _tempMessages.value + assistantMsg
            } else if (convId != null) {
                val res = addMessageInternal(convId, assistantMsg)
                if (speakAfter && res.isSuccess) {
                    speakText(res.getOrThrow(), finalAnswer)
                }
            }
            if (speakAfter && isTemp) {
                speakText(assistantMsg.id, finalAnswer)
            }
        } catch (e: CancellationException) {
            val partial = _streamingAnswer.value
            if (partial.isNotBlank()) {
                val stoppedNote = if (currentPrefs.appLanguage.isRtl) {
                    "$partial\n\n*أوقفتُ الرد بناءً على طلبك.*"
                } else {
                    "$partial\n\n*Response stopped at your request.*"
                }
                val stoppedMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    conversationId = convId ?: "temp",
                    role = "assistant",
                    content = stoppedNote,
                    model = currentPrefs.selectedModelId
                )
                if (isTemp) {
                    _tempMessages.value = _tempMessages.value + stoppedMsg
                } else if (convId != null) {
                    addMessageInternal(convId, stoppedMsg)
                }
            }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "streamTextReply failed", e)
            _chatError.value = e.localizedMessage ?: "تعذّر الحصول على رد من النموذج."
        } finally {
            _isStreaming.value = false
            _streamingThinking.value = ""
            _streamingAnswer.value = ""
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
        _isStreaming.value = false
    }

    fun regenerateLastReply() {
        if (_isStreaming.value) return
        val convId = _activeConversationId.value
        val msgs = if (_isTempChat.value) _tempMessages.value else (activeMessagesState.value as? UiState.Success)?.data.orEmpty()
        val lastAssistant = msgs.lastOrNull { it.role == "assistant" }
        val lastUser = msgs.lastOrNull { it.role == "user" } ?: return

        viewModelScope.launch {
            if (_isTempChat.value) {
                if (lastAssistant != null) {
                    _tempMessages.value = _tempMessages.value.dropLast(1)
                }
                streamTextReply(null, true, lastUser.content, emptyList(), false)
            } else if (convId != null) {
                if (lastAssistant != null && lastAssistant.id.isNotBlank()) {
                    deleteMessageInternal(convId, lastAssistant.id)
                }
                streamTextReply(convId, false, lastUser.content, emptyList(), false)
            }
        }
    }

    fun editUserMessage(message: ChatMessage, newContent: String) {
        val convId = _activeConversationId.value ?: return
        val clipped = newContent.take(8000)
        if (isGuestMode) {
            updateGuestMessage(convId, message.id) { it.copy(content = clipped) }
            showToast("تم تعديل الرسالة")
            return
        }
        viewModelScope.launch {
            repo.updateMessage(convId, message.id, mapOf("content" to clipped))
                .onSuccess { showToast("تم تعديل الرسالة") }
                .onFailure { showToast("تعذّر التعديل", true) }
        }
    }

    fun deleteMessage(message: ChatMessage) {
        if (_isTempChat.value) {
            _tempMessages.value = _tempMessages.value.filterNot { it.id == message.id }
            return
        }
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            deleteMessageInternal(convId, message.id)
        }
    }

    fun rateMessage(message: ChatMessage, rating: String, note: String = "") {
        val convId = _activeConversationId.value ?: return
        if (isGuestMode) {
            updateGuestMessage(convId, message.id) { it.copy(feedback = rating, feedbackNote = note.take(500)) }
            showToast("شكرًا لتقييمك!")
            return
        }
        viewModelScope.launch {
            repo.submitFeedback(convId, message.id, rating, note, message.model ?: "gemini-3.8-flash")
                .onSuccess { showToast("شكرًا لتقييمك!") }
                .onFailure { showToast("تعذّر حفظ التقييم", true) }
        }
    }

    // --- Conversation Actions ---

    fun renameConversation(conversationId: String, newTitle: String) {
        val clipped = newTitle.trim().take(160)
        if (isGuestMode) {
            updateGuestConversation(conversationId) { it.copy(title = clipped) }
            showToast("تمت إعادة التسمية")
            return
        }
        viewModelScope.launch {
            repo.updateConversation(conversationId, mapOf("title" to clipped))
                .onSuccess { showToast("تمت إعادة التسمية") }
        }
    }

    fun togglePinConversation(conv: Conversation) {
        if (isGuestMode) {
            updateGuestConversation(conv.id) { it.copy(pinned = !conv.pinned) }
            showToast(if (!conv.pinned) "تم تثبيت المحادثة" else "تم إلغاء التثبيت")
            return
        }
        viewModelScope.launch {
            repo.updateConversation(conv.id, mapOf("pinned" to !conv.pinned))
                .onSuccess { showToast(if (!conv.pinned) "تم تثبيت المحادثة" else "تم إلغاء التثبيت") }
        }
    }

    fun toggleArchiveConversation(conv: Conversation) {
        if (isGuestMode) {
            updateGuestConversation(conv.id) { it.copy(archived = !conv.archived) }
            if (!conv.archived && _activeConversationId.value == conv.id) {
                _activeConversationId.value = null
            }
            showToast(if (!conv.archived) "تمت الأرشفة" else "تم إلغاء الأرشفة")
            return
        }
        viewModelScope.launch {
            repo.updateConversation(conv.id, mapOf("archived" to !conv.archived))
                .onSuccess {
                    if (!conv.archived && _activeConversationId.value == conv.id) {
                        _activeConversationId.value = null
                    }
                    showToast(if (!conv.archived) "تمت الأرشفة" else "تم إلغاء الأرشفة")
                }
        }
    }

    fun toggleLockConversation(conv: Conversation) {
        if (isGuestMode) {
            updateGuestConversation(conv.id) { it.copy(locked = !conv.locked) }
            showToast(if (!conv.locked) "تم قفل الدردشة" else "تم إلغاء قفل الدردشة")
            return
        }
        viewModelScope.launch {
            repo.updateConversation(conv.id, mapOf("locked" to !conv.locked))
                .onSuccess {
                    showToast(if (!conv.locked) "تم قفل الدردشة" else "تم إلغاء قفل الدردشة")
                }
        }
    }

    fun moveConversationToProject(conversationId: String, projectId: String?) {
        if (isGuestMode) {
            updateGuestConversation(conversationId) { it.copy(projectId = projectId) }
            showToast("تم نقل المحادثة")
            return
        }
        viewModelScope.launch {
            val updates = if (projectId != null) mapOf("projectId" to projectId) else mapOf("projectId" to "")
            repo.updateConversation(conversationId, updates)
                .onSuccess { showToast("تم نقل المحادثة") }
        }
    }

    fun deleteConversation(conversationId: String) {
        if (isGuestMode) {
            _guestConversations.value = _guestConversations.value.filterNot { it.id == conversationId }
            _guestMessagesMap.value = _guestMessagesMap.value - conversationId
            if (_activeConversationId.value == conversationId) _activeConversationId.value = null
            showToast("تم حذف المحادثة")
            return
        }
        viewModelScope.launch {
            repo.deleteConversation(conversationId)
                .onSuccess {
                    if (_activeConversationId.value == conversationId) {
                        _activeConversationId.value = null
                    }
                    showToast("تم حذف المحادثة")
                }
        }
    }

    fun deleteAllConversations() {
        if (isGuestMode) {
            _guestConversations.value = emptyList()
            _guestMessagesMap.value = emptyMap()
            _activeConversationId.value = null
            showToast("تم حذف جميع المحادثات")
            return
        }
        viewModelScope.launch {
            repo.deleteAllConversations()
                .onSuccess {
                    _activeConversationId.value = null
                    showToast("تم حذف جميع المحادثات")
                }
                .onFailure { showToast("تعذّر حذف المحادثات", true) }
        }
    }

    // --- Projects, Memory, Customization, Images ---

    fun saveProject(project: Project) {
        if (isGuestMode) {
            val id = project.id.ifBlank { UUID.randomUUID().toString() }
            val next = project.copy(id = id, userId = currentUserId, updatedAt = Timestamp.now())
            val rest = _guestProjects.value.filterNot { it.id == id }
            _guestProjects.value = listOf(next) + rest
            showToast("تم حفظ المشروع")
            return
        }
        viewModelScope.launch {
            repo.saveProject(project)
                .onSuccess { showToast("تم حفظ المشروع") }
                .onFailure { showToast("تعذّر حفظ المشروع", true) }
        }
    }

    fun deleteProject(projectId: String) {
        if (isGuestMode) {
            _guestProjects.value = _guestProjects.value.filterNot { it.id == projectId }
            showToast("تم حذف المشروع")
            return
        }
        viewModelScope.launch {
            repo.deleteProject(projectId)
                .onSuccess { showToast("تم حذف المشروع") }
        }
    }

    fun addMemory(content: String, category: String) {
        if (isGuestMode) {
            val item = MemoryItem(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                content = content.trim().take(240),
                category = category,
                enabled = true,
                createdAt = Timestamp.now()
            )
            _guestMemory.value = listOf(item) + _guestMemory.value
            showToast("تمت إضافة المعلومة إلى الذاكرة")
            return
        }
        viewModelScope.launch {
            repo.addMemory(content, category)
                .onSuccess { showToast("تمت إضافة المعلومة إلى الذاكرة") }
                .onFailure { showToast("تعذّر حفظ المعلومة", true) }
        }
    }

    fun toggleMemoryItem(item: MemoryItem) {
        if (isGuestMode) {
            _guestMemory.value = _guestMemory.value.map {
                if (it.id == item.id) it.copy(enabled = !item.enabled) else it
            }
            return
        }
        viewModelScope.launch {
            repo.updateMemory(item.id, mapOf("enabled" to !item.enabled))
        }
    }

    fun updateMemoryContent(item: MemoryItem, newContent: String, newCategory: String) {
        if (isGuestMode) {
            _guestMemory.value = _guestMemory.value.map {
                if (it.id == item.id) it.copy(content = newContent.take(240), category = newCategory) else it
            }
            showToast("تم تحديث المعلومة")
            return
        }
        viewModelScope.launch {
            repo.updateMemory(item.id, mapOf("content" to newContent.take(240), "category" to newCategory))
                .onSuccess { showToast("تم تحديث المعلومة") }
        }
    }

    fun deleteMemoryItem(id: String) {
        if (isGuestMode) {
            _guestMemory.value = _guestMemory.value.filterNot { it.id == id }
            return
        }
        viewModelScope.launch {
            repo.deleteMemory(id)
        }
    }

    fun clearAllMemory() {
        if (isGuestMode) {
            _guestMemory.value = emptyList()
            showToast("تم مسح الذاكرة")
            return
        }
        viewModelScope.launch {
            repo.clearAllMemory().onSuccess { showToast("تم مسح الذاكرة") }
        }
    }

    fun saveCustomization(preferredName: String, customInstructions: String) {
        if (isGuestMode) {
            _guestProfile.value = _guestProfile.value.copy(
                preferredName = preferredName.take(60),
                customInstructions = customInstructions.take(2000)
            )
            showToast("تم حفظ التخصيص")
            return
        }
        viewModelScope.launch {
            repo.updateCustomization(preferredName, customInstructions)
                .onSuccess { showToast("تم حفظ التخصيص") }
                .onFailure { showToast("تعذّر الحفظ", true) }
        }
    }

    fun updateDisplayName(name: String) {
        if (isGuestMode) {
            _guestProfile.value = _guestProfile.value.copy(
                displayName = name.trim().ifBlank { "زائر Nux" }.take(120)
            )
            showToast("تم تحديث الاسم")
            return
        }
        viewModelScope.launch {
            repo.updateDisplayName(name)
                .onSuccess { showToast("تم تحديث الاسم") }
                .onFailure { showToast("تعذّر التحديث", true) }
        }
    }

    fun generateStudioImage(prompt: String, style: ImageStyleOption, ratio: AspectRatioOption) {
        viewModelScope.launch {
            _isStreaming.value = true
            try {
                val bmp = aiRepo.generateOrEditImage(prompt, style, ratio)
                val path = withContext(Dispatchers.IO) {
                    MediaUtils.saveBitmapLocally(appContext, bmp, "studio")
                }
                addGeneratedImageInternal(
                    GeneratedImageRecord(
                        prompt = prompt,
                        aspectRatio = ratio.label,
                        style = style.arLabel,
                        model = "gemini-nano-banana-2.1",
                        localFileName = path
                    )
                )
                showToast("تم إنشاء الصورة وحفظها في الاستوديو")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "تعذّر إنشاء الصورة", true)
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun deleteStudioImage(imageId: String) {
        if (isGuestMode) {
            _guestImages.value = _guestImages.value.filterNot { it.id == imageId }
            showToast("تم حذف الصورة")
            return
        }
        viewModelScope.launch {
            repo.deleteGeneratedImage(imageId)
                .onSuccess { showToast("تم حذف الصورة") }
        }
    }

    // --- Security PINs ---

    fun setPin(kind: String, rawPin: String) {
        if (isGuestMode) {
            showToast("يرجى تسجيل الدخول بحساب Google لتفعيل رمز الأمان السحابي", true)
            return
        }
        viewModelScope.launch {
            val salt = PinCrypto.generateSalt()
            val hash = PinCrypto.hashPin(rawPin, salt)
            val dev = _deviceId.value.ifBlank { prefsRepo.ensureDeviceId() }
            repo.upsertSecurityPin(kind, hash, salt, enabled = true, trustedDevices = listOf(dev))
                .onSuccess {
                    if (kind == "login") _loginPinVerified.value = true
                    showToast("تم تفعيل رمز PIN بنجاح")
                }
                .onFailure { showToast("تعذّر حفظ الرمز", true) }
        }
    }

    fun disablePin(kind: String) {
        if (isGuestMode) return
        viewModelScope.launch {
            val pins = (securityPinsState.value as? UiState.Success)?.data.orEmpty()
            val current = pins.firstOrNull { it.kind == kind } ?: return@launch
            repo.upsertSecurityPin(kind, current.pinHash, current.salt, enabled = false, trustedDevices = emptyList())
                .onSuccess { showToast("تم إيقاف رمز PIN") }
        }
    }

    fun trustOnlyThisDevice() {
        if (isGuestMode) return
        viewModelScope.launch {
            val pins = (securityPinsState.value as? UiState.Success)?.data.orEmpty()
            val loginPin = pins.firstOrNull { it.kind == "login" } ?: return@launch
            val dev = _deviceId.value.ifBlank { prefsRepo.ensureDeviceId() }
            repo.upsertSecurityPin("login", loginPin.pinHash, loginPin.salt, loginPin.enabled, listOf(dev))
                .onSuccess { showToast("تم إبقاء هذا الجهاز موثوقًا فقط") }
        }
    }

    fun verifyPin(kind: String, enteredPin: String): Boolean {
        val pins = (securityPinsState.value as? UiState.Success)?.data.orEmpty()
        val doc = pins.firstOrNull { it.kind == kind && it.enabled } ?: return true
        return PinCrypto.verifyPin(enteredPin, doc.salt, doc.pinHash)
    }

    fun markChatUnlocked(conversationId: String) {
        _unlockedChatIds.value = _unlockedChatIds.value + conversationId
    }

    fun markLoginPinVerified() {
        _loginPinVerified.value = true
    }

    // --- Studios (Site Builder, APK Studio, Remote, Engine) ---

    fun generateOrUpdateSite(prompt: String, existingSite: SiteBuild? = null) {
        viewModelScope.launch {
            _isBuildingSite.value = true
            try {
                val html = aiRepo.generateWebsiteHtml(prompt, existingSite?.htmlCode)
                val site = if (existingSite != null) {
                    existingSite.copy(
                        prompt = prompt.take(1500),
                        htmlCode = html,
                        versionCount = existingSite.versionCount + 1L
                    )
                } else {
                    SiteBuild(
                        title = prompt.take(60).ifBlank { "موقع Nux" },
                        description = prompt.take(160),
                        prompt = prompt.take(1500),
                        htmlCode = html,
                        published = false,
                        versionCount = 1L
                    )
                }
                if (isGuestMode) {
                    val id = site.id.ifBlank { UUID.randomUUID().toString() }
                    val saved = site.copy(id = id, userId = currentUserId, updatedAt = Timestamp.now())
                    _guestSites.value = listOf(saved) + _guestSites.value.filterNot { it.id == id }
                    showToast("تم بناء الموقع بنجاح")
                } else {
                    repo.saveSiteBuild(site)
                        .onSuccess { showToast("تم بناء الموقع بنجاح") }
                        .onFailure { showToast("تعذّر حفظ الموقع", true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "تعذّر توليد الموقع", true)
            } finally {
                _isBuildingSite.value = false
            }
        }
    }

    fun toggleSitePublished(site: SiteBuild) {
        if (isGuestMode) {
            _guestSites.value = _guestSites.value.map {
                if (it.id == site.id) it.copy(published = !site.published) else it
            }
            showToast(if (!site.published) "تم نشر الموقع" else "تم إيقاف النشر")
            return
        }
        viewModelScope.launch {
            repo.saveSiteBuild(site.copy(published = !site.published))
                .onSuccess {
                    showToast(if (!site.published) "تم نشر الموقع" else "تم إيقاف النشر")
                }
        }
    }

    fun deleteSiteBuild(siteId: String) {
        if (isGuestMode) {
            _guestSites.value = _guestSites.value.filterNot { it.id == siteId }
            showToast("تم حذف الموقع")
            return
        }
        viewModelScope.launch {
            repo.deleteSiteBuild(siteId).onSuccess { showToast("تم حذف الموقع") }
        }
    }

    fun planAndroidAppSpec(idea: String) {
        viewModelScope.launch {
            _isPlanningApk.value = true
            try {
                val planned = aiRepo.planAndroidApp(idea)
                val spec = NativeAppSpec(
                    id = if (isGuestMode) UUID.randomUUID().toString() else "",
                    userId = currentUserId,
                    name = planned.name,
                    idea = idea.take(1500),
                    packageName = planned.packageName,
                    status = "جاهز",
                    specSummary = planned.specSummary,
                    permissions = planned.permissions,
                    features = planned.features,
                    secretKeys = planned.secretKeys,
                    versionName = "1.0.0",
                    versionCode = 1L,
                    updatedAt = Timestamp.now()
                )
                if (isGuestMode) {
                    _guestNativeApps.value = listOf(spec) + _guestNativeApps.value
                    showToast("تم إنشاء مواصفة التطبيق")
                } else {
                    repo.saveNativeApp(spec).onSuccess { showToast("تم إنشاء مواصفة التطبيق") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "تعذّر تخطيط التطبيق", true)
            } finally {
                _isPlanningApk.value = false
            }
        }
    }

    fun deleteNativeApp(appId: String) {
        if (isGuestMode) {
            _guestNativeApps.value = _guestNativeApps.value.filterNot { it.id == appId }
            showToast("تم حذف المشروع")
            return
        }
        viewModelScope.launch {
            repo.deleteNativeApp(appId).onSuccess { showToast("تم حذف المشروع") }
        }
    }

    fun planRemoteCommand(instruction: String) {
        viewModelScope.launch {
            _isPlanningRemote.value = true
            try {
                _remoteSteps.value = aiRepo.planRemoteTask(instruction)
                showToast("تم تخطيط خطوات المهمة")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.localizedMessage ?: "تعذّر تخطيط المهمة", true)
            } finally {
                _isPlanningRemote.value = false
            }
        }
    }

    fun approveRemoteStep(index: Int, approved: Boolean) {
        val current = _remoteSteps.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(
                status = if (approved) "approved" else "rejected"
            )
            _remoteSteps.value = current
        }
    }

    fun runEngineProbe() {
        viewModelScope.launch {
            _engineProbeResult.value = "جارٍ الاختبار..."
            aiRepo.probeEngine()
                .onSuccess { ms -> _engineProbeResult.value = "متصل بنجاح (${ms}ms)" }
                .onFailure { e -> _engineProbeResult.value = "فشل الاتصال: ${e.localizedMessage}" }
        }
    }

    // --- TTS & Voice ---

    fun speakText(messageId: String, text: String) {
        if (!ttsReady || text.isBlank()) return
        val voiceOption = NuxCatalog.liveVoices.firstOrNull { it.id == prefs.value.liveVoice }
        val lang = if (text.any { it in '\u0600'..'\u06FF' }) Locale("ar") else Locale.US
        tts?.language = lang
        tts?.setSpeechRate(prefs.value.speechRate * (voiceOption?.speed ?: 1.0f))
        tts?.setPitch(voiceOption?.pitch ?: 1.0f)
        _speakingMessageId.value = messageId
        tts?.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _speakingMessageId.value = null
    }

    suspend fun runLiveVoiceTurn(spokenText: String): String {
        val currentPrefs = prefs.value
        val profile = (profileState.value as? UiState.Success)?.data
        val memories = (memoryState.value as? UiState.Success)?.data.orEmpty()

        var convId = _activeConversationId.value
        if (convId.isNullOrBlank()) {
            convId = createConversationInternal(
                title = "محادثة صوتية",
                projectId = _activeProjectId.value,
                modelName = currentPrefs.selectedModelId
            ).getOrNull()
            if (convId != null) _activeConversationId.value = convId
        }

        if (convId != null) {
            addMessageInternal(
                convId,
                ChatMessage(
                    userId = currentUserId,
                    conversationId = convId,
                    role = "user",
                    content = spokenText,
                    audioTranscript = spokenText
                )
            )
        }

        val buffer = StringBuilder()
        aiRepo.streamChatReply(
            modelId = "gemini-3.8-flash",
            history = emptyList(),
            userPrompt = spokenText,
            attachments = emptyList(),
            preferredName = profile?.preferredName ?: "",
            customInstructions = (profile?.customInstructions ?: "") + "\nKeep voice responses concise, natural, and conversational.",
            projectInstructions = "",
            memories = memories,
            replyLanguageCode = currentPrefs.replyLanguage,
            deepThinking = false,
            webSearchMode = false,
            toolDirective = "",
            uiIsArabic = currentPrefs.appLanguage.isRtl
        ).collect { update ->
            buffer.clear()
            buffer.append(update.answerText)
        }

        val reply = buffer.toString().ifBlank { "لم أفهم ذلك جيدًا، هل يمكنك الإعادة؟" }
        if (convId != null) {
            addMessageInternal(
                convId,
                ChatMessage(
                    userId = currentUserId,
                    conversationId = convId,
                    role = "assistant",
                    content = reply,
                    model = "gemini-3.8-flash"
                )
            )
        }
        return reply
    }

    // --- Export Data ---

    fun exportConversations(onReady: (String) -> Unit) {
        if (isGuestMode) {
            val arr = JSONArray()
            _guestConversations.value.forEach { conv ->
                val msgsArr = JSONArray()
                _guestMessagesMap.value[conv.id].orEmpty().forEach { m ->
                    msgsArr.put(
                        JSONObject().apply {
                            put("role", m.role)
                            put("content", m.content)
                            put("model", m.model ?: "")
                        }
                    )
                }
                arr.put(
                    JSONObject().apply {
                        put("id", conv.id)
                        put("title", conv.title)
                        put("messages", msgsArr)
                    }
                )
            }
            onReady(
                JSONObject().apply {
                    put("app", "Nux")
                    put("mode", "guest")
                    put("conversations", arr)
                }.toString(2)
            )
            return
        }
        viewModelScope.launch {
            repo.exportConversationsJson()
                .onSuccess { json -> onReady(json) }
                .onFailure { showToast("تعذّر تصدير المحادثات", true) }
        }
    }

    // --- Preference Mutators ---

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setAppLanguage(lang: AppLanguage) = viewModelScope.launch { prefsRepo.setAppLanguage(lang) }
    fun setReplyLanguage(code: String) = viewModelScope.launch { prefsRepo.setReplyLanguage(code) }
    fun setSelectedModel(modelId: String) = viewModelScope.launch { prefsRepo.setSelectedModelId(modelId) }
    fun setShowThinking(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowThinkingSteps(enabled) }
    fun setWatermark(enabled: Boolean) = viewModelScope.launch { prefsRepo.setWatermarkEnabled(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { prefsRepo.setNotificationsEnabled(enabled) }
    fun setUseMemory(enabled: Boolean) = viewModelScope.launch { prefsRepo.setUseMemory(enabled) }
    fun setAutoSaveMemory(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAutoSaveMemory(enabled) }
    fun setTempChatCustomized(enabled: Boolean) = viewModelScope.launch { prefsRepo.setTempChatCustomized(enabled) }
    fun setWebSearchDefault(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setWebSearchDefault(enabled)
        _webSearchActive.value = enabled
    }
    fun setSpeechRate(rate: Float) = viewModelScope.launch { prefsRepo.setSpeechRate(rate) }
    fun setLiveVoice(voice: String) = viewModelScope.launch {
        prefsRepo.setLiveVoice(voice)
        if (isGuestMode) {
            _guestProfile.value = _guestProfile.value.copy(liveVoice = voice)
        } else {
            repo.updateLiveVoice(voice)
        }
    }
    fun setBubbleTone(tone: BubbleTone) = viewModelScope.launch { prefsRepo.setBubbleTone(tone) }
    fun setBubbleShape(shape: BubbleShape) = viewModelScope.launch { prefsRepo.setBubbleShape(shape) }
    fun setSidebarOpen(open: Boolean) = viewModelScope.launch { prefsRepo.setSidebarOpen(open) }

    // --- Internal Guest / Cloud Persistence Helpers ---

    private suspend fun createConversationInternal(
        title: String,
        projectId: String?,
        modelName: String
    ): Result<String> {
        if (!isGuestMode) return repo.createConversation(title, projectId, modelName)
        val id = UUID.randomUUID().toString()
        val conv = Conversation(
            id = id,
            userId = currentUserId,
            title = title.trim().ifBlank { "محادثة جديدة" }.take(160),
            projectId = projectId,
            modelName = modelName,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _guestConversations.value = listOf(conv) + _guestConversations.value
        return Result.success(id)
    }

    private suspend fun addMessageInternal(convId: String, message: ChatMessage): Result<String> {
        if (!isGuestMode) return repo.addMessage(convId, message)
        val msgId = message.id.ifBlank { UUID.randomUUID().toString() }
        val saved = message.copy(id = msgId, userId = currentUserId, conversationId = convId, createdAt = Timestamp.now())
        val map = _guestMessagesMap.value.toMutableMap()
        map[convId] = map[convId].orEmpty() + saved
        _guestMessagesMap.value = map
        updateGuestConversation(convId) { it.copy(updatedAt = Timestamp.now()) }
        return Result.success(msgId)
    }

    private suspend fun deleteMessageInternal(convId: String, messageId: String) {
        if (!isGuestMode) {
            repo.deleteMessage(convId, messageId)
            return
        }
        val map = _guestMessagesMap.value.toMutableMap()
        map[convId] = map[convId].orEmpty().filterNot { it.id == messageId }
        _guestMessagesMap.value = map
    }

    private suspend fun addGeneratedImageInternal(record: GeneratedImageRecord) {
        if (!isGuestMode) {
            repo.addGeneratedImage(record)
            return
        }
        val id = UUID.randomUUID().toString()
        val saved = record.copy(id = id, userId = currentUserId, createdAt = Timestamp.now())
        _guestImages.value = listOf(saved) + _guestImages.value
    }

    private fun updateGuestConversation(convId: String, transform: (Conversation) -> Conversation) {
        _guestConversations.value = _guestConversations.value.map {
            if (it.id == convId) transform(it).copy(updatedAt = Timestamp.now()) else it
        }
    }

    private fun updateGuestMessage(convId: String, msgId: String, transform: (ChatMessage) -> ChatMessage) {
        val map = _guestMessagesMap.value.toMutableMap()
        map[convId] = map[convId].orEmpty().map { if (it.id == msgId) transform(it) else it }
        _guestMessagesMap.value = map
    }

    private fun <T> Flow<T>.asUiState(fallbackMsg: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream observation error", rootError)
                emit(UiState.Error(rootError.message ?: fallbackMsg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    companion object {
        const val GUEST_USER_ID = "__nux_guest_local__"

        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NuxViewModel(
                    appContext = app,
                    repo = NuxRepository(db),
                    aiRepo = NuxAiRepository(),
                    prefsRepo = NuxPreferencesRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
