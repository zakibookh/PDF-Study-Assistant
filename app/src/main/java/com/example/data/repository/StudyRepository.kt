package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.SessionPreferences
import com.example.data.model.AuthResponse
import com.example.data.model.CategoryDto
import com.example.data.model.CategoryRequest
import com.example.data.model.ChatAskRequest
import com.example.data.model.ChatAskResponse
import com.example.data.model.ChatHistoryItem
import com.example.data.model.DocumentDetailResponse
import com.example.data.model.DocumentDto
import com.example.data.model.DocumentPageResponse
import com.example.data.model.DocumentUploadResponse
import com.example.data.model.HealthResponse
import com.example.data.model.LoginRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.SearchRouteRequest
import com.example.data.model.UpdateDocumentRequest
import com.example.data.model.UserDto
import com.example.data.remote.ApiClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class StudyRepository(
    private val context: Context,
    private val preferences: SessionPreferences,
    private val apiFactory: ApiClientFactory,
) {
    suspend fun initialize(): Pair<String, UserDto?> {
        val baseUrl = preferences.getApiBaseUrl()
        val token = preferences.getAuthToken()
        apiFactory.authToken = token
        apiFactory.getService(baseUrl)
        val cachedUser = preferences.getCachedUser()
        return baseUrl to cachedUser
    }

    private suspend fun service() = apiFactory.getService(preferences.getApiBaseUrl())

    private inline fun <T> safeCall(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (t: Throwable) {
            Result.failure(Exception(apiFactory.parseError(t), t))
        }
    }

    suspend fun checkHealth(customUrl: String? = null): Result<HealthResponse> = withContext(Dispatchers.IO) {
        safeCall {
            val targetService = if (customUrl != null) {
                apiFactory.getService(customUrl)
            } else {
                service()
            }
            targetService.getHealth()
        }
    }

    suspend fun saveBaseUrl(url: String): String {
        val normalized = preferences.saveApiBaseUrl(url)
        apiFactory.getService(normalized)
        return normalized
    }

    suspend fun resetBaseUrl(): String {
        val normalized = preferences.resetApiBaseUrl()
        apiFactory.getService(normalized)
        return normalized
    }

    suspend fun getPreferredMode(): String = withContext(Dispatchers.IO) {
        preferences.savePreferredMode("smart_search").let { "smart_search" }
    }

    suspend fun savePreferredMode(mode: String) {
        preferences.savePreferredMode(mode)
    }

    fun preferredModeFlow() = preferences.preferredModeFlow
    fun baseUrlFlow() = preferences.apiBaseUrlFlow

    // --- Auth ---
    suspend fun login(email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        safeCall {
            val res = service().login(LoginRequest(email.trim(), password))
            apiFactory.authToken = res.token
            preferences.saveSession(res.token, res.user)
            res
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        adminInviteCode: String?,
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        safeCall {
            val code = adminInviteCode?.trim()?.takeIf { it.isNotEmpty() }
            val res = service().register(RegisterRequest(name.trim(), email.trim(), password, code))
            apiFactory.authToken = res.token
            preferences.saveSession(res.token, res.user)
            res
        }
    }

    suspend fun fetchMe(): Result<UserDto> = withContext(Dispatchers.IO) {
        safeCall {
            val res = service().me()
            preferences.saveCachedUser(res.user)
            res.user
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { service().logout() }
        apiFactory.authToken = null
        preferences.clearSession()
        Result.success(Unit)
    }

    suspend fun clearLocalSession() {
        apiFactory.authToken = null
        preferences.clearSession()
    }

    // --- Categories ---
    suspend fun listCategories(): Result<List<CategoryDto>> = withContext(Dispatchers.IO) {
        safeCall { service().listCategories().categories }
    }

    suspend fun createCategory(name: String, description: String): Result<CategoryDto> = withContext(Dispatchers.IO) {
        safeCall { service().createCategory(CategoryRequest(name.trim(), description.trim())).category }
    }

    suspend fun updateCategory(id: String, name: String, description: String): Result<CategoryDto> =
        withContext(Dispatchers.IO) {
            safeCall { service().updateCategory(id, CategoryRequest(name.trim(), description.trim())).category }
        }

    suspend fun deleteCategory(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            service().deleteCategory(id)
            Unit
        }
    }

    // --- Student Documents & Reader ---
    suspend fun listDocuments(query: String? = null, categoryId: String? = null): Result<List<DocumentDto>> =
        withContext(Dispatchers.IO) {
            safeCall {
                service().listStudentDocuments(
                    query = query?.trim()?.takeIf { it.isNotEmpty() },
                    categoryId = categoryId?.takeIf { it.isNotEmpty() },
                ).documents
            }
        }

    suspend fun getDocumentDetail(id: String): Result<DocumentDetailResponse> = withContext(Dispatchers.IO) {
        safeCall { service().getStudentDocument(id) }
    }

    suspend fun getDocumentPage(id: String, page: Int): Result<DocumentPageResponse> = withContext(Dispatchers.IO) {
        safeCall { service().getDocumentPage(id, page) }
    }

    suspend fun downloadDocumentPdf(id: String, suggestedFileName: String): Result<File> = withContext(Dispatchers.IO) {
        safeCall {
            val cacheDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeName = suggestedFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "$id.pdf" }
            val targetFile = File(cacheDir, "${id}_$safeName")
            val body = service().downloadDocumentPdf(id)
            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        }
    }

    // --- Chat & Search ---
    suspend fun askQuestion(
        question: String,
        documentId: String?,
        answerMode: String,
        limit: Int = 5,
    ): Result<ChatAskResponse> = withContext(Dispatchers.IO) {
        safeCall {
            service().askChat(
                ChatAskRequest(
                    question = question.trim(),
                    document_id = documentId,
                    answer_mode = answerMode,
                    limit = limit,
                ),
            )
        }
    }

    suspend fun quickSearch(
        query: String,
        documentId: String?,
        answerMode: String,
        limit: Int = 8,
    ): Result<ChatAskResponse> = withContext(Dispatchers.IO) {
        safeCall {
            val req = SearchRouteRequest(query = query.trim(), document_id = documentId, limit = limit)
            if (answerMode == "exact_answer") {
                service().exactSearch(req)
            } else {
                service().smartSearch(req)
            }
        }
    }

    suspend fun getChatHistory(documentId: String? = null): Result<List<ChatHistoryItem>> =
        withContext(Dispatchers.IO) {
            safeCall { service().getChatHistory(documentId?.takeIf { it.isNotBlank() }).history }
        }

    suspend fun clearChatHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            service().deleteChatHistory()
            Unit
        }
    }

    // --- Admin Documents ---
    suspend fun listAdminDocuments(
        query: String? = null,
        categoryId: String? = null,
        status: String? = null,
    ): Result<List<DocumentDto>> = withContext(Dispatchers.IO) {
        safeCall {
            service().listAdminDocuments(
                query = query?.trim()?.takeIf { it.isNotEmpty() },
                categoryId = categoryId?.takeIf { it.isNotEmpty() },
                status = status?.takeIf { it.isNotEmpty() },
            ).documents
        }
    }

    suspend fun uploadPdfFromUri(
        uri: Uri,
        title: String,
        description: String,
        categoryId: String?,
    ): Result<DocumentUploadResponse> = withContext(Dispatchers.IO) {
        safeCall {
            val resolver = context.contentResolver
            val fileName = resolveFileName(uri) ?: "study_document.pdf"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Unable to read selected PDF file.")

            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = fileName,
                body = bytes.toRequestBody("application/pdf".toMediaType()),
            )
            val textType = "text/plain".toMediaType()
            val titleBody = title.trim().toRequestBody(textType)
            val descBody = description.trim().toRequestBody(textType)
            val catBody = categoryId?.takeIf { it.isNotBlank() }?.toRequestBody(textType)

            service().uploadAdminDocument(filePart, titleBody, descBody, catBody)
        }
    }

    suspend fun updateAdminDocument(
        id: String,
        title: String,
        description: String,
        categoryId: String?,
    ): Result<DocumentUploadResponse> = withContext(Dispatchers.IO) {
        safeCall {
            service().updateAdminDocument(
                id,
                UpdateDocumentRequest(title.trim(), description.trim(), categoryId?.takeIf { it.isNotBlank() }),
            )
        }
    }

    suspend fun reprocessAdminDocument(id: String): Result<DocumentUploadResponse> = withContext(Dispatchers.IO) {
        safeCall { service().processAdminDocument(id) }
    }

    suspend fun deleteAdminDocument(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            service().deleteAdminDocument(id)
            Unit
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
