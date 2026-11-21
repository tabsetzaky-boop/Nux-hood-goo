package com.example.data.firebase

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AspectRatioOption
import com.example.data.model.AttachmentItem
import com.example.data.model.ChatMessage
import com.example.data.model.ImageStyleOption
import com.example.data.model.MemoryItem
import com.example.data.model.RemoteStepItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "NuxAiRepository"

data class AiStreamUpdate(
    val thinkingText: String,
    val answerText: String,
    val isDone: Boolean = false
)

data class PlannedNativeAppResult(
    val name: String,
    val packageName: String,
    val specSummary: String,
    val permissions: List<String>,
    val features: List<String>,
    val secretKeys: List<String>
)

class NuxAiRepository {

    private val defaultChatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val followUpModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    properties = mapOf(
                        "suggestions" to Schema.array(
                            items = Schema.string(description = "Short actionable next-step prompt in the user's language"),
                            description = "2 to 3 concise follow-up suggestions"
                        )
                    )
                )
            }
        )
    }

    private val apkPlannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Short app name"),
                        "packageName" to Schema.string(description = "Reverse-DNS Android package name"),
                        "specSummary" to Schema.string(description = "Detailed Arabic/English screen & architecture spec"),
                        "permissions" to Schema.array(items = Schema.string(), description = "Android permissions like CAMERA, LOCATION, NOTIFICATIONS"),
                        "features" to Schema.array(items = Schema.string(), description = "Key app features"),
                        "secretKeys" to Schema.array(items = Schema.string(), description = "External API secret names needed if any")
                    )
                )
            }
        )
    }

    private val remotePlannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    properties = mapOf(
                        "steps" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "title" to Schema.string(description = "Human-readable step title"),
                                    "commandHint" to Schema.string(description = "Shell/OS command outline"),
                                    "requiresApproval" to Schema.boolean(description = "True if file modification, deletion, or system action")
                                )
                            )
                        )
                    )
                )
            }
        )
    }

    fun streamChatReply(
        modelId: String,
        history: List<ChatMessage>,
        userPrompt: String,
        attachments: List<AttachmentItem>,
        preferredName: String,
        customInstructions: String,
        projectInstructions: String,
        memories: List<MemoryItem>,
        replyLanguageCode: String,
        deepThinking: Boolean,
        webSearchMode: Boolean,
        toolDirective: String,
        uiIsArabic: Boolean
    ): Flow<AiStreamUpdate> = flow {
        val sysPrompt = buildSystemPrompt(
            preferredName = preferredName,
            customInstructions = customInstructions,
            projectInstructions = projectInstructions,
            memories = memories,
            replyLanguageCode = replyLanguageCode,
            deepThinking = deepThinking,
            webSearchMode = webSearchMode,
            toolDirective = toolDirective,
            uiIsArabic = uiIsArabic
        )

        val effectiveModelId = when {
            deepThinking && modelId == "gemini-3.5-flash-lite" -> "gemini-3.8-flash"
            modelId in listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon") -> modelId
            else -> "gemini-3.8-flash"
        }

        val model = Firebase.ai.generativeModel(
            modelName = effectiveModelId,
            systemInstruction = content { text(sysPrompt) }
        )

        val historyContents = history
            .takeLast(16)
            .filter { it.content.isNotBlank() }
            .map { msg ->
                val role = if (msg.role == "user") "user" else "model"
                content(role) { text(msg.content) }
            }

        val chat = model.startChat(history = historyContents)

        val incomingContent = content("user") {
            val docSnippets = StringBuilder()
            for (att in attachments) {
                if (att.isImage && att.bitmap != null) {
                    image(att.bitmap)
                } else if (!att.textSnippet.isNullOrBlank()) {
                    docSnippets.append("\n[Attached File: ${att.name}]\n${att.textSnippet}\n")
                } else {
                    docSnippets.append("\n[Attached File: ${att.name} (${att.mimeType})]\n")
                }
            }
            val finalPrompt = buildString {
                if (userPrompt.isNotBlank()) append(userPrompt)
                if (docSnippets.isNotEmpty()) append(docSnippets)
            }.ifBlank { "حلل المرفقات." }
            text(finalPrompt)
        }

        val rawBuffer = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(incomingContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        rawBuffer.append(piece)
                        val parsed = parseThinkingAndAnswer(rawBuffer.toString(), deepThinking)
                        emit(parsed.copy(isDone = false))
                    }
                }
            }
            val finalParsed = parseThinkingAndAnswer(rawBuffer.toString(), deepThinking)
            if (finalParsed.answerText.isBlank() && finalParsed.thinkingText.isBlank()) {
                throw IllegalStateException("لم يُرجع النموذج أي نص.")
            }
            emit(finalParsed.copy(isDone = true))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat streaming timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat streaming failed", e)
            throw e
        }
    }

    suspend fun generateFollowUps(userPrompt: String, assistantReply: String, isArabic: Boolean): List<String> {
        return try {
            withTimeout(30_000L) {
                val langHint = if (isArabic) "Arabic" else "matching the user's language"
                val resp = followUpModel.generateContent(
                    "Based on this exchange, suggest 3 short natural next-step questions or actions in $langHint (max 7 words each).\nUser: ${userPrompt.take(300)}\nAssistant: ${assistantReply.take(500)}"
                )
                val jsonStr = resp.text ?: return@withTimeout emptyList()
                val arr = JSONObject(jsonStr).optJSONArray("suggestions") ?: return@withTimeout emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    arr.optString(i).takeIf { it.isNotBlank() }
                }.take(3)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Follow-up suggestion skipped: ${e.message}")
            emptyList()
        }
    }

    suspend fun generateOrEditImage(
        prompt: String,
        style: ImageStyleOption,
        aspectRatio: AspectRatioOption,
        referenceBitmap: Bitmap? = null
    ): Bitmap {
        val ar = when (aspectRatio) {
            AspectRatioOption.SQUARE -> AspectRatio.SQUARE_1x1
            AspectRatioOption.PORTRAIT -> AspectRatio.PORTRAIT_3x4
            AspectRatioOption.LANDSCAPE -> AspectRatio.LANDSCAPE_16x9
        }

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(ar)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }

        val imageModel = Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )

        val enrichedPrompt = "$prompt. Visual style: ${style.promptSuffix}."

        try {
            return withTimeout(60_000L) {
                val response = if (referenceBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(referenceBitmap)
                            text(enrichedPrompt)
                        }
                    )
                } else {
                    imageModel.generateContent(enrichedPrompt)
                }
                val part = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                part?.image ?: throw IllegalStateException("لم يُرجع النموذج صورة.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("انتهت مهلة إنشاء الصورة (60 ثانية). حاول مرة أخرى.")
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image quota exceeded", e)
            throw IllegalStateException("خدمة الصور مشغولة الآن أو تجاوزت الحصة المتاحة.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw e
        }
    }

    suspend fun generateWebsiteHtml(prompt: String, existingHtml: String? = null): String {
        val system = """
            You are Nux Site Builder, an elite front-end architect.
            Generate a COMPLETE, modern, responsive single-file HTML5 document (including inline <style> and <script>).
            Match the language and text direction (dir="rtl" for Arabic, dir="ltr" otherwise) of the user's request.
            Include Cairo font via Google Fonts if Arabic, sleek glass/modern aesthetics, interactive sections, and mobile responsiveness.
            Output ONLY raw HTML starting with <!DOCTYPE html> and nothing else (no markdown fences).
        """.trimIndent()

        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(system) }
        )

        val fullPrompt = if (!existingHtml.isNullOrBlank()) {
            "Update this existing HTML website according to the user's request:\nRequest: $prompt\n\nExisting HTML:\n${existingHtml.take(25000)}"
        } else {
            "Build a complete single-file HTML website for: $prompt"
        }

        return withTimeout(60_000L) {
            val raw = model.generateContent(fullPrompt).text
                ?: throw IllegalStateException("تعذّر توليد كود الموقع.")
            raw.removePrefix("```html").removePrefix("```").removeSuffix("```").trim()
        }
    }

    suspend fun planAndroidApp(idea: String): PlannedNativeAppResult {
        return withTimeout(60_000L) {
            val resp = apkPlannerModel.generateContent(
                "Plan a complete Android Jetpack Compose app specification in Arabic (with English packageName & permission identifiers) for this idea:\n$idea"
            )
            val jsonStr = resp.text ?: throw IllegalStateException("تعذّر تخطيط مواصفة التطبيق.")
            val json = JSONObject(jsonStr)
            val permsArr = json.optJSONArray("permissions")
            val featsArr = json.optJSONArray("features")
            val secretsArr = json.optJSONArray("secretKeys")
            PlannedNativeAppResult(
                name = json.optString("name", "تطبيق Nux").take(100),
                packageName = json.optString("packageName", "com.nux.generated.app").take(120),
                specSummary = json.optString("specSummary", idea).take(30000),
                permissions = permsArr?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } } ?: emptyList(),
                features = featsArr?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } } ?: emptyList(),
                secretKeys = secretsArr?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } } ?: emptyList(),
            )
        }
    }

    suspend fun planRemoteTask(instruction: String): List<RemoteStepItem> {
        return withTimeout(45_000L) {
            val resp = remotePlannerModel.generateContent(
                "Break this desktop/remote computer task into 3 to 5 safe clear execution steps (Arabic titles, command hint, and requiresApproval boolean):\n$instruction"
            )
            val jsonStr = resp.text ?: throw IllegalStateException("تعذّر تخطيط خطوات المهمة.")
            val arr = JSONObject(jsonStr).optJSONArray("steps") ?: return@withTimeout emptyList()
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                RemoteStepItem(
                    title = obj.optString("title"),
                    commandHint = obj.optString("commandHint"),
                    requiresApproval = obj.optBoolean("requiresApproval", true)
                )
            }
        }
    }

    suspend fun probeEngine(): Result<Long> {
        return try {
            val start = System.currentTimeMillis()
            withTimeout(30_000L) {
                val res = defaultChatModel.generateContent("Reply with OK only.")
                if (res.text.isNullOrBlank()) throw IllegalStateException("Empty probe response")
            }
            Result.success(System.currentTimeMillis() - start)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(
        preferredName: String,
        customInstructions: String,
        projectInstructions: String,
        memories: List<MemoryItem>,
        replyLanguageCode: String,
        deepThinking: Boolean,
        webSearchMode: Boolean,
        toolDirective: String,
        uiIsArabic: Boolean
    ): String = buildString {
        appendLine("You are Nux (نوكس), a smart, direct, friendly, honest multilingual AI assistant.")
        appendLine("Personality: Start with the result then details, never fabricate facts or fake URLs, and apologize concisely in one line if something cannot be done.")
        if (preferredName.isNotBlank()) {
            appendLine("Address the user as: $preferredName.")
        }
        if (replyLanguageCode == "auto") {
            appendLine("Always reply in the same language as the user's latest message (defaulting to ${if (uiIsArabic) "Arabic" else "English"} if ambiguous).")
        } else {
            appendLine("Always reply in language code: $replyLanguageCode.")
        }
        if (customInstructions.isNotBlank()) {
            appendLine("User personal instructions: $customInstructions")
        }
        if (projectInstructions.isNotBlank()) {
            appendLine("Active project workspace instructions: $projectInstructions")
        }
        val activeMemories = memories.filter { it.enabled }.take(40)
        if (activeMemories.isNotEmpty()) {
            appendLine("Known facts about the user from Memory:")
            activeMemories.forEach { appendLine("- [${it.category}] ${it.content}") }
        }
        if (toolDirective.isNotBlank()) {
            appendLine("Special active tool instruction: $toolDirective")
        }
        if (webSearchMode) {
            appendLine("Research mode is active: provide thorough, fact-grounded explanations, clearly citing well-known public references or noting when live browsing is unavailable.")
        }
        if (deepThinking) {
            appendLine("Deep Thinking mode is active: FIRST write your step-by-step reasoning inside [THINKING] ... [/THINKING] tags, then write your clear final answer outside the tags.")
        }
    }

    private fun parseThinkingAndAnswer(raw: String, deepThinking: Boolean): AiStreamUpdate {
        if (!deepThinking && !raw.contains("[THINKING]")) {
            return AiStreamUpdate(thinkingText = "", answerText = raw)
        }
        val startIdx = raw.indexOf("[THINKING]")
        if (startIdx == -1) {
            return AiStreamUpdate(thinkingText = "", answerText = raw)
        }
        val afterStart = raw.substring(startIdx + "[THINKING]".length)
        val endIdx = afterStart.indexOf("[/THINKING]")
        return if (endIdx == -1) {
            AiStreamUpdate(thinkingText = afterStart.trimStart(), answerText = "")
        } else {
            val thinking = afterStart.substring(0, endIdx).trim()
            val answer = (raw.substring(0, startIdx) + afterStart.substring(endIdx + "[/THINKING]".length)).trim()
            AiStreamUpdate(thinkingText = thinking, answerText = answer)
        }
    }
}
