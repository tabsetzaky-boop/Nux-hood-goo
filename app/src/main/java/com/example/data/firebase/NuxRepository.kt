package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.GeneratedImageRecord
import com.example.data.model.MemoryItem
import com.example.data.model.NativeAppSpec
import com.example.data.model.Project
import com.example.data.model.SecurityPinDoc
import com.example.data.model.SiteBuild
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class NuxRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولًا.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- User Profile ---

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            userDoc(userId).snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                }
        )
    }

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<UserProfile> = try {
        val docRef = userDoc(uid)
        val existing = docRef.get().await()
            .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        if (existing != null) {
            Result.success(existing)
        } else {
            val profile = UserProfile(
                userId = uid,
                displayName = displayName.ifBlank { "مستخدم Nux" },
                email = email,
                photoUrl = photoUrl,
                preferredName = displayName.substringBefore(" ").take(60),
                customInstructions = "",
                gender = "unspecified",
                age = 24L,
                liveVoice = "Aoede",
                profileCompleted = false
            )
            docRef.set(profile.toCreateMap()).await()
            Result.success(profile.copy(id = uid))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid")
        Result.failure(e)
    }

    suspend fun completeOnboarding(gender: String, age: Long, defaultVoice: String): Result<Unit> =
        updateProfileFields(
            mapOf(
                "gender" to gender,
                "age" to age.coerceIn(16L, 100L),
                "liveVoice" to defaultVoice,
                "profileCompleted" to true
            )
        )

    suspend fun updateCustomization(preferredName: String, customInstructions: String): Result<Unit> =
        updateProfileFields(
            mapOf(
                "preferredName" to preferredName.take(60),
                "customInstructions" to customInstructions.take(2000)
            )
        )

    suspend fun updateDisplayName(displayName: String): Result<Unit> =
        updateProfileFields(mapOf("displayName" to displayName.trim().ifBlank { "مستخدم Nux" }.take(120)))

    suspend fun updateLiveVoice(voice: String): Result<Unit> =
        updateProfileFields(mapOf("liveVoice" to voice.take(40)))

    private suspend fun updateProfileFields(fields: Map<String, Any>): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            userDoc(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Conversations ---

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/conversations"
        emitAll(
            userDoc(userId).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun createConversation(
        title: String,
        projectId: String? = null,
        modelName: String = "gemini-3.8-flash"
    ): Result<String> {
        var path = "users/unknown/conversations"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("conversations").document()
            path = ref.path
            val conv = Conversation(
                userId = uid,
                title = title.trim().ifBlank { "محادثة جديدة" }.take(160),
                projectId = projectId,
                modelName = modelName
            )
            ref.set(conv.toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateConversation(conversationId: String, updates: Map<String, Any>): Result<Unit> {
        var path = "users/unknown/conversations/$conversationId"
        return try {
            val uid = requireUserId()
            require(conversationId.isNotBlank())
            val ref = userDoc(uid).collection("conversations").document(conversationId)
            path = ref.path
            val payload = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        var path = "users/unknown/conversations/$conversationId"
        return try {
            val uid = requireUserId()
            val convRef = userDoc(uid).collection("conversations").document(conversationId)
            path = convRef.path
            val msgs = convRef.collection("messages").get().await()
            for (doc in msgs.documents) {
                doc.reference.delete().await()
            }
            convRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllConversations(): Result<Unit> {
        var path = "users/unknown/conversations"
        return try {
            val uid = requireUserId()
            path = "users/$uid/conversations"
            val convs = userDoc(uid).collection("conversations").get().await()
            for (conv in convs.documents) {
                val msgs = conv.reference.collection("messages").get().await()
                for (msg in msgs.documents) {
                    msg.reference.delete().await()
                }
                conv.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Messages ---

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        if (userId.isBlank() || conversationId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/conversations/$conversationId/messages"
        emitAll(
            userDoc(userId).collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                }
        )
    }

    suspend fun addMessage(conversationId: String, message: ChatMessage): Result<String> {
        var path = "users/unknown/conversations/$conversationId/messages"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document()
            path = ref.path
            val payload = message.copy(userId = uid, conversationId = conversationId).toCreateMap()
            ref.set(payload).await()
            userDoc(uid).collection("conversations").document(conversationId)
                .update(mapOf("updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMessage(
        conversationId: String,
        messageId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        var path = "users/unknown/conversations/$conversationId/messages/$messageId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
            path = ref.path
            val payload = updates.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        var path = "users/unknown/conversations/$conversationId/messages/$messageId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun submitFeedback(
        conversationId: String,
        messageId: String,
        rating: String,
        note: String,
        model: String
    ): Result<Unit> {
        var path = "users/unknown/feedback"
        return try {
            val uid = requireUserId()
            val msgResult = updateMessage(
                conversationId,
                messageId,
                mapOf("feedback" to rating, "feedbackNote" to note.take(500))
            )
            if (msgResult.isFailure) return msgResult
            val fbRef = userDoc(uid).collection("feedback").document()
            path = fbRef.path
            fbRef.set(
                mapOf(
                    "userId" to uid,
                    "messageId" to messageId,
                    "conversationId" to conversationId,
                    "rating" to rating,
                    "note" to note.take(500),
                    "model" to model.take(80),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Projects ---

    fun observeProjects(userId: String): Flow<List<Project>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/projects"
        emitAll(
            userDoc(userId).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(Project::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    suspend fun saveProject(project: Project): Result<String> {
        var path = "users/unknown/projects"
        return try {
            val uid = requireUserId()
            val col = userDoc(uid).collection("projects")
            if (project.id.isBlank()) {
                val ref = col.document()
                path = ref.path
                ref.set(project.copy(userId = uid).toCreateMap()).await()
                Result.success(ref.id)
            } else {
                val ref = col.document(project.id)
                path = ref.path
                ref.update(
                    mapOf(
                        "name" to project.name.take(100),
                        "iconEmoji" to project.iconEmoji.ifBlank { "📁" }.take(16),
                        "description" to project.description.take(500),
                        "instructions" to project.instructions.take(2000),
                        "pinned" to project.pinned,
                        "buildKind" to project.buildKind.take(32),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(project.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        var path = "users/unknown/projects/$projectId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("projects").document(projectId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Memory ---

    fun observeMemory(userId: String): Flow<List<MemoryItem>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/memory"
        emitAll(
            userDoc(userId).collection("memory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(MemoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    suspend fun addMemory(content: String, category: String, source: String = "manual"): Result<Unit> {
        var path = "users/unknown/memory"
        return try {
            val uid = requireUserId()
            val clean = content.trim().take(240)
            require(clean.isNotEmpty())
            val ref = userDoc(uid).collection("memory").document()
            path = ref.path
            ref.set(
                MemoryItem(
                    userId = uid,
                    content = clean,
                    category = if (category in listOf("شخصي", "تفضيلات", "عمل", "أخرى")) category else "أخرى",
                    enabled = true,
                    source = source
                ).toCreateMap()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemory(memoryId: String, updates: Map<String, Any>): Result<Unit> {
        var path = "users/unknown/memory/$memoryId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("memory").document(memoryId)
            path = ref.path
            val payload = updates.toMutableMap().apply { put("updatedAt", FieldValue.serverTimestamp()) }
            ref.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(memoryId: String): Result<Unit> {
        var path = "users/unknown/memory/$memoryId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("memory").document(memoryId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAllMemory(): Result<Unit> {
        var path = "users/unknown/memory"
        return try {
            val uid = requireUserId()
            path = "users/$uid/memory"
            val docs = userDoc(uid).collection("memory").get().await()
            for (d in docs.documents) d.reference.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Generated Images ---

    fun observeGeneratedImages(userId: String): Flow<List<GeneratedImageRecord>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/generated_images"
        emitAll(
            userDoc(userId).collection("generated_images")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(GeneratedImageRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    suspend fun addGeneratedImage(record: GeneratedImageRecord): Result<String> {
        var path = "users/unknown/generated_images"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("generated_images").document()
            path = ref.path
            ref.set(record.copy(userId = uid).toCreateMap()).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGeneratedImage(imageId: String): Result<Unit> {
        var path = "users/unknown/generated_images/$imageId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("generated_images").document(imageId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Site Builds ---

    fun observeSiteBuilds(userId: String): Flow<List<SiteBuild>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/site_builds"
        emitAll(
            userDoc(userId).collection("site_builds")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(SiteBuild::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    suspend fun saveSiteBuild(site: SiteBuild): Result<String> {
        var path = "users/unknown/site_builds"
        return try {
            val uid = requireUserId()
            val col = userDoc(uid).collection("site_builds")
            if (site.id.isBlank()) {
                val ref = col.document()
                path = ref.path
                ref.set(site.copy(userId = uid).toCreateMap()).await()
                Result.success(ref.id)
            } else {
                val ref = col.document(site.id)
                path = ref.path
                ref.update(
                    mapOf(
                        "title" to site.title.take(120),
                        "description" to site.description.take(500),
                        "prompt" to site.prompt.take(1500),
                        "htmlCode" to site.htmlCode.take(300000),
                        "published" to site.published,
                        "versionCount" to site.versionCount.coerceIn(1L, 1000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(site.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSiteBuild(siteId: String): Result<Unit> {
        var path = "users/unknown/site_builds/$siteId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("site_builds").document(siteId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Native Apps (APK Studio) ---

    fun observeNativeApps(userId: String): Flow<List<NativeAppSpec>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/native_apps"
        emitAll(
            userDoc(userId).collection("native_apps")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(NativeAppSpec::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    suspend fun saveNativeApp(app: NativeAppSpec): Result<String> {
        var path = "users/unknown/native_apps"
        return try {
            val uid = requireUserId()
            val col = userDoc(uid).collection("native_apps")
            if (app.id.isBlank()) {
                val ref = col.document()
                path = ref.path
                ref.set(app.copy(userId = uid).toCreateMap()).await()
                Result.success(ref.id)
            } else {
                val ref = col.document(app.id)
                path = ref.path
                ref.update(
                    mapOf(
                        "name" to app.name.take(100),
                        "idea" to app.idea.take(1500),
                        "packageName" to app.packageName.take(120),
                        "status" to app.status.take(32),
                        "specSummary" to app.specSummary.take(30000),
                        "permissions" to app.permissions.take(20),
                        "features" to app.features.take(30),
                        "secretKeys" to app.secretKeys.take(20),
                        "versionName" to app.versionName.take(32),
                        "versionCode" to app.versionCode.coerceIn(1L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(app.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNativeApp(appId: String): Result<Unit> {
        var path = "users/unknown/native_apps/$appId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("native_apps").document(appId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Security PINs ---

    fun observeSecurityPins(userId: String): Flow<List<SecurityPinDoc>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/security"
        emitAll(
            userDoc(userId).collection("security")
                .snapshots()
                .map { snap -> snap.toObjects(SecurityPinDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleFlowError(error, OperationType.LIST, path, userId) }
        )
    }

    suspend fun upsertSecurityPin(
        kind: String,
        pinHash: String,
        salt: String,
        enabled: Boolean,
        trustedDevices: List<String>
    ): Result<Unit> {
        val docId = "pin_$kind"
        var path = "users/unknown/security/$docId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("security").document(docId)
            path = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "pinHash" to pinHash,
                        "salt" to salt,
                        "enabled" to enabled,
                        "trustedDevices" to trustedDevices.take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    SecurityPinDoc(
                        userId = uid,
                        kind = kind,
                        pinHash = pinHash,
                        salt = salt,
                        enabled = enabled,
                        trustedDevices = trustedDevices
                    ).toCreateMap()
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Export JSON ---

    suspend fun exportConversationsJson(): Result<String> {
        var path = "users/unknown/conversations"
        return try {
            val uid = requireUserId()
            path = "users/$uid/conversations"
            val convs = userDoc(uid).collection("conversations").get().await()
            val arr = JSONArray()
            for (convDoc in convs.documents) {
                val conv = convDoc.toObject(Conversation::class.java) ?: continue
                val msgsSnap = convDoc.reference.collection("messages")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .await()
                val msgsArr = JSONArray()
                for (mDoc in msgsSnap.documents) {
                    val m = mDoc.toObject(ChatMessage::class.java) ?: continue
                    msgsArr.put(
                        JSONObject().apply {
                            put("role", m.role)
                            put("content", m.content)
                            put("model", m.model ?: "")
                            put("createdAt", m.createdAt?.toDate()?.toString() ?: "")
                        }
                    )
                }
                arr.put(
                    JSONObject().apply {
                        put("id", convDoc.id)
                        put("title", conv.title)
                        put("pinned", conv.pinned)
                        put("archived", conv.archived)
                        put("locked", conv.locked)
                        put("messages", msgsArr)
                    }
                )
            }
            Result.success(
                JSONObject().apply {
                    put("app", "Nux")
                    put("exportedAt", System.currentTimeMillis())
                    put("conversations", arr)
                }.toString(2)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
