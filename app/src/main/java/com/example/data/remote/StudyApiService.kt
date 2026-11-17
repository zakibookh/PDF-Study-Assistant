package com.example.data.remote

import com.example.data.model.AuthResponse
import com.example.data.model.CategoriesResponse
import com.example.data.model.CategoryMutationResponse
import com.example.data.model.CategoryRequest
import com.example.data.model.ChatAskRequest
import com.example.data.model.ChatAskResponse
import com.example.data.model.ChatHistoryResponse
import com.example.data.model.DeleteResponse
import com.example.data.model.DocumentDetailResponse
import com.example.data.model.DocumentPageResponse
import com.example.data.model.DocumentUploadResponse
import com.example.data.model.DocumentsListResponse
import com.example.data.model.GenericMessageResponse
import com.example.data.model.HealthResponse
import com.example.data.model.LoginRequest
import com.example.data.model.MeResponse
import com.example.data.model.RegisterRequest
import com.example.data.model.SearchRouteRequest
import com.example.data.model.UpdateDocumentRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface StudyApiService {

    @GET("/")
    suspend fun getHealth(): HealthResponse

    // --- Authentication ---
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(): GenericMessageResponse

    @GET("api/v1/auth/me")
    suspend fun me(): MeResponse

    // --- Categories ---
    @GET("api/v1/categories")
    suspend fun listCategories(): CategoriesResponse

    @POST("api/v1/admin/categories")
    suspend fun createCategory(@Body body: CategoryRequest): CategoryMutationResponse

    @PUT("api/v1/admin/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body body: CategoryRequest,
    ): CategoryMutationResponse

    @DELETE("api/v1/admin/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): DeleteResponse

    // --- Student Documents ---
    @GET("api/v1/documents")
    suspend fun listStudentDocuments(
        @Query("q") query: String? = null,
        @Query("category_id") categoryId: String? = null,
    ): DocumentsListResponse

    @GET("api/v1/documents/{id}")
    suspend fun getStudentDocument(@Path("id") id: String): DocumentDetailResponse

    @GET("api/v1/documents/{id}/pages/{page}")
    suspend fun getDocumentPage(
        @Path("id") id: String,
        @Path("page") page: Int,
    ): DocumentPageResponse

    @Streaming
    @GET("api/v1/documents/{id}/download")
    suspend fun downloadDocumentPdf(@Path("id") id: String): ResponseBody

    // --- Chat & Search ---
    @POST("api/v1/chat/ask")
    suspend fun askChat(@Body body: ChatAskRequest): ChatAskResponse

    @POST("api/v1/search/smart")
    suspend fun smartSearch(@Body body: SearchRouteRequest): ChatAskResponse

    @POST("api/v1/search/exact")
    suspend fun exactSearch(@Body body: SearchRouteRequest): ChatAskResponse

    @GET("api/v1/chat/history")
    suspend fun getChatHistory(
        @Query("document_id") documentId: String? = null,
    ): ChatHistoryResponse

    @DELETE("api/v1/chat/history")
    suspend fun deleteChatHistory(): GenericMessageResponse

    // --- Admin Documents ---
    @GET("api/v1/admin/documents")
    suspend fun listAdminDocuments(
        @Query("q") query: String? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("status") status: String? = null,
    ): DocumentsListResponse

    @Multipart
    @POST("api/v1/admin/documents")
    suspend fun uploadAdminDocument(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category_id") categoryId: RequestBody?,
    ): DocumentUploadResponse

    @PUT("api/v1/admin/documents/{id}")
    suspend fun updateAdminDocument(
        @Path("id") id: String,
        @Body body: UpdateDocumentRequest,
    ): DocumentUploadResponse

    @DELETE("api/v1/admin/documents/{id}")
    suspend fun deleteAdminDocument(@Path("id") id: String): DeleteResponse

    @POST("api/v1/admin/documents/{id}/process")
    suspend fun processAdminDocument(
        @Path("id") id: String,
        @Body body: Map<String, String> = emptyMap(),
    ): DocumentUploadResponse
}
