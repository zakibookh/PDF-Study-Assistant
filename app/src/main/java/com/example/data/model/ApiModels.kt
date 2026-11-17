package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val success: Boolean = false,
    val name: String = "",
    val mode: String? = null,
    val status: String = "",
    val endpoints: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val created_at: String,
) {
    val isAdmin: Boolean
        get() = role.equals("admin", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val admin_invite_code: String? = null,
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val success: Boolean,
    val token: String,
    val expires_at: Long,
    val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    val success: Boolean,
    val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class GenericMessageResponse(
    val success: Boolean,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: String,
    val name: String,
    val description: String = "",
    val document_count: Int = 0,
)

@JsonClass(generateAdapter = true)
data class CategoriesResponse(
    val success: Boolean,
    val categories: List<CategoryDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CategoryRequest(
    val name: String,
    val description: String,
)

@JsonClass(generateAdapter = true)
data class CategoryMutationResponse(
    val success: Boolean,
    val category: CategoryDto,
)

@JsonClass(generateAdapter = true)
data class DocumentDto(
    val id: String,
    val title: String,
    val description: String = "",
    val file_name: String = "",
    val r2_object_key: String = "",
    val file_size: Long = 0L,
    val page_count: Int = 0,
    val category_id: String? = null,
    val category_name: String? = null,
    val uploaded_at: String = "",
    val processing_status: String = "Completed",
    val status_message: String = "",
    val uploaded_by: String? = null,
)

@JsonClass(generateAdapter = true)
data class DocumentsListResponse(
    val success: Boolean,
    val documents: List<DocumentDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DocumentDetailResponse(
    val success: Boolean,
    val document: DocumentDto,
    val download_url: String? = null,
    val chunk_count: Int? = null,
)

@JsonClass(generateAdapter = true)
data class DocumentPageResponse(
    val success: Boolean,
    val document_id: String,
    val document_title: String,
    val page_count: Int,
    val page_number: Int,
    val original_text: String,
)

@JsonClass(generateAdapter = true)
data class UploadStatsDto(
    val pageCount: Int = 0,
    val chunkCount: Int = 0,
    val termCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class DocumentUploadResponse(
    val success: Boolean,
    val document: DocumentDto? = null,
    val stats: UploadStatsDto? = null,
    val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdateDocumentRequest(
    val title: String,
    val description: String,
    val category_id: String? = null,
)

@JsonClass(generateAdapter = true)
data class DeleteResponse(
    val success: Boolean,
    val deleted_id: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChatAskRequest(
    val question: String,
    val document_id: String? = null,
    val answer_mode: String = "smart_search",
    val limit: Int = 5,
)

@JsonClass(generateAdapter = true)
data class SearchRouteRequest(
    val query: String,
    val document_id: String? = null,
    val limit: Int = 8,
)

@JsonClass(generateAdapter = true)
data class SearchMatchItem(
    val document_id: String,
    val document_title: String,
    val page_number: Int,
    val relevance_score: Double,
    val matched_text: String,
    val matched_terms: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ChatAskResponse(
    val success: Boolean = true,
    val history_id: String? = null,
    val created_at: String? = null,
    val found: Boolean = false,
    val answer: String = "",
    val answer_mode: String = "smart_search",
    val document_id: String? = null,
    val document_title: String? = null,
    val page_number: Int? = null,
    val relevance_score: Double = 0.0,
    val matched_text: String? = null,
    val results: List<SearchMatchItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ChatHistoryItem(
    val id: String,
    val user_id: String = "",
    val document_id: String? = null,
    val question: String,
    val answer: String,
    val answer_mode: String = "smart_search",
    val page_number: Int? = null,
    val relevance_score: Double = 0.0,
    val document_title: String? = null,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class ChatHistoryResponse(
    val success: Boolean,
    val history: List<ChatHistoryItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val success: Boolean = false,
    val error: String = "",
    val code: String? = null,
)
