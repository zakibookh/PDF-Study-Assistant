package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.SessionPreferences
import com.example.data.model.CategoryDto
import com.example.data.model.ChatAskResponse
import com.example.data.model.ChatHistoryItem
import com.example.data.model.DocumentDto
import com.example.data.model.HealthResponse
import com.example.data.model.UserDto
import com.example.data.remote.ApiClientFactory
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface SessionState {
    data object Checking : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val user: UserDto) : SessionState
}

sealed interface HealthStatus {
    data object Idle : HealthStatus
    data object Checking : HealthStatus
    data class Online(
        val response: HealthResponse,
        val isV1Compatible: Boolean,
    ) : HealthStatus
    data class Error(val message: String) : HealthStatus
}

data class AuthUiState(
    val isRegisterMode: Boolean = false,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val adminInviteCode: String = "",
    val showAdminCodeField: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class StudyMessageItem(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val answerMode: String,
    val isQuickSearch: Boolean = false,
    val response: ChatAskResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class StudyUiState(
    val messages: List<StudyMessageItem> = emptyList(),
    val selectedDocument: DocumentDto? = null,
    val answerMode: String = "smart_search",
    val saveToHistory: Boolean = true,
    val questionInput: String = "",
    val isSubmitting: Boolean = false,
)

data class LibraryUiState(
    val categories: List<CategoryDto> = emptyList(),
    val documents: List<DocumentDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class ReaderUiState(
    val isOpen: Boolean = false,
    val documentId: String = "",
    val documentTitle: String = "",
    val document: DocumentDto? = null,
    val currentPage: Int = 1,
    val pageCount: Int = 1,
    val pageText: String = "",
    val isLoadingPage: Boolean = false,
    val isDownloadingPdf: Boolean = false,
    val downloadedPdfFile: File? = null,
    val error: String? = null,
)

data class HistoryUiState(
    val items: List<ChatHistoryItem> = emptyList(),
    val filterDocumentId: String? = null,
    val isLoading: Boolean = false,
    val isClearing: Boolean = false,
    val error: String? = null,
)

data class AdminUiState(
    val documents: List<DocumentDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val statusFilter: String? = null,
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val feedbackMessage: String? = null,
    val error: String? = null,
)

data class SettingsUiState(
    val baseUrlInput: String = BuildConfig.API_BASE_URL,
    val savedBaseUrl: String = BuildConfig.API_BASE_URL,
    val healthStatus: HealthStatus = HealthStatus.Idle,
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val apiFactory = ApiClientFactory()
    private val preferences = SessionPreferences(application.applicationContext, apiFactory.moshi)
    private val repository = StudyRepository(application.applicationContext, preferences, apiFactory)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _studyUiState = MutableStateFlow(StudyUiState())
    val studyUiState: StateFlow<StudyUiState> = _studyUiState.asStateFlow()

    private val _libraryUiState = MutableStateFlow(LibraryUiState())
    val libraryUiState: StateFlow<LibraryUiState> = _libraryUiState.asStateFlow()

    private val _readerUiState = MutableStateFlow(ReaderUiState())
    val readerUiState: StateFlow<ReaderUiState> = _readerUiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(HistoryUiState())
    val historyUiState: StateFlow<HistoryUiState> = _historyUiState.asStateFlow()

    private val _adminUiState = MutableStateFlow(AdminUiState())
    val adminUiState: StateFlow<AdminUiState> = _adminUiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    init {
        bootstrap()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            repository.preferredModeFlow().collectLatest { mode ->
                _studyUiState.update { it.copy(answerMode = mode) }
            }
        }
        viewModelScope.launch {
            repository.baseUrlFlow().collectLatest { url ->
                _settingsUiState.update {
                    it.copy(
                        savedBaseUrl = url,
                        baseUrlInput = if (it.baseUrlInput == BuildConfig.API_BASE_URL || it.baseUrlInput.isBlank()) url else it.baseUrlInput,
                    )
                }
            }
        }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val (baseUrl, cachedUser) = repository.initialize()
            _settingsUiState.update { it.copy(baseUrlInput = baseUrl, savedBaseUrl = baseUrl) }
            checkServerHealth(baseUrl)

            if (apiFactory.authToken.isNullOrBlank()) {
                _sessionState.value = SessionState.Unauthenticated
                return@launch
            }
            if (cachedUser != null) {
                _sessionState.value = SessionState.Authenticated(cachedUser)
                loadInitialAuthenticatedData(cachedUser)
            }
            repository.fetchMe().onSuccess { freshUser ->
                _sessionState.value = SessionState.Authenticated(freshUser)
                if (cachedUser == null) {
                    loadInitialAuthenticatedData(freshUser)
                }
            }.onFailure { err ->
                if (cachedUser == null || err.message?.contains("401") == true) {
                    repository.clearLocalSession()
                    _sessionState.value = SessionState.Unauthenticated
                }
            }
        }
    }

    private fun loadInitialAuthenticatedData(user: UserDto) {
        refreshLibrary()
        refreshHistory()
        if (user.isAdmin) {
            refreshAdminData()
        }
    }

    // --- Health & Settings ---
    fun onBaseUrlInputChange(value: String) {
        _settingsUiState.update { it.copy(baseUrlInput = value) }
    }

    fun saveAndTestServerUrl() {
        val raw = _settingsUiState.value.baseUrlInput
        viewModelScope.launch {
            val normalized = repository.saveBaseUrl(raw)
            _settingsUiState.update { it.copy(baseUrlInput = normalized, savedBaseUrl = normalized) }
            checkServerHealth(normalized)
        }
    }

    fun resetServerUrl() {
        viewModelScope.launch {
            val defaultUrl = repository.resetBaseUrl()
            _settingsUiState.update { it.copy(baseUrlInput = defaultUrl, savedBaseUrl = defaultUrl) }
            checkServerHealth(defaultUrl)
        }
    }

    fun checkServerHealth(customUrl: String? = null) {
        viewModelScope.launch {
            _settingsUiState.update { it.copy(healthStatus = HealthStatus.Checking) }
            repository.checkHealth(customUrl)
                .onSuccess { health ->
                    val isV1 = health.endpoints.any { it.contains("/api/v1/") }
                    _settingsUiState.update {
                        it.copy(healthStatus = HealthStatus.Online(health, isV1))
                    }
                }
                .onFailure { err ->
                    _settingsUiState.update {
                        it.copy(healthStatus = HealthStatus.Error(err.message ?: "Unable to connect"))
                    }
                }
        }
    }

    // --- Authentication ---
    fun toggleAuthMode(isRegister: Boolean) {
        _authUiState.update { it.copy(isRegisterMode = isRegister, error = null) }
    }

    fun onAuthNameChange(value: String) = _authUiState.update { it.copy(name = value, error = null) }
    fun onAuthEmailChange(value: String) = _authUiState.update { it.copy(email = value, error = null) }
    fun onAuthPasswordChange(value: String) = _authUiState.update { it.copy(password = value, error = null) }
    fun onAdminCodeChange(value: String) = _authUiState.update { it.copy(adminInviteCode = value, error = null) }
    fun toggleAdminCodeField() = _authUiState.update { it.copy(showAdminCodeField = !it.showAdminCodeField) }

    fun submitAuth() {
        val state = _authUiState.value
        val email = state.email.trim()
        val password = state.password
        if (state.isRegisterMode && state.name.trim().length < 2) {
            _authUiState.update { it.copy(error = "Magaca waa inuu ahaadaa ugu yaraan 2 xaraf (Name min 2 chars).") }
            return
        }
        if (email.isBlank() || !email.contains("@")) {
            _authUiState.update { it.copy(error = "Fadlan geli email sax ah (Enter a valid email).") }
            return
        }
        if (password.length < (if (state.isRegisterMode) 8 else 1)) {
            _authUiState.update {
                it.copy(
                    error = if (state.isRegisterMode) {
                        "Furaha sirta ah waa inuu ahaadaa ugu yaraan 8 xaraf (Min 8 characters)."
                    } else {
                        "Fadlan geli furaha sirta ah (Password required)."
                    },
                )
            }
            return
        }

        viewModelScope.launch {
            _authUiState.update { it.copy(isLoading = true, error = null) }
            val result = if (state.isRegisterMode) {
                repository.register(state.name, email, password, if (state.showAdminCodeField) state.adminInviteCode else null)
            } else {
                repository.login(email, password)
            }
            result.onSuccess { auth ->
                _authUiState.value = AuthUiState()
                _sessionState.value = SessionState.Authenticated(auth.user)
                loadInitialAuthenticatedData(auth.user)
            }.onFailure { err ->
                _authUiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _studyUiState.value = StudyUiState()
            _libraryUiState.value = LibraryUiState()
            _historyUiState.value = HistoryUiState()
            _adminUiState.value = AdminUiState()
            _readerUiState.value = ReaderUiState()
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    // --- Study & Search ---
    fun onQuestionInputChange(value: String) {
        _studyUiState.update { it.copy(questionInput = value) }
    }

    fun selectStudyDocument(document: DocumentDto?) {
        _studyUiState.update { it.copy(selectedDocument = document) }
    }

    fun setAnswerMode(mode: String) {
        _studyUiState.update { it.copy(answerMode = mode) }
        viewModelScope.launch { repository.savePreferredMode(mode) }
    }

    fun setSaveToHistory(save: Boolean) {
        _studyUiState.update { it.copy(saveToHistory = save) }
    }

    fun submitStudyQuestion(overrideQuestion: String? = null) {
        val current = _studyUiState.value
        val question = (overrideQuestion ?: current.questionInput).trim()
        if (question.length < 2 || current.isSubmitting) return

        val msgId = UUID.randomUUID().toString()
        val pendingItem = StudyMessageItem(
            id = msgId,
            question = question,
            answerMode = current.answerMode,
            isQuickSearch = !current.saveToHistory,
            isLoading = true,
        )
        _studyUiState.update {
            it.copy(
                messages = it.messages + pendingItem,
                questionInput = if (overrideQuestion == null) "" else it.questionInput,
                isSubmitting = true,
            )
        }

        viewModelScope.launch {
            val result = if (current.saveToHistory) {
                repository.askQuestion(
                    question = question,
                    documentId = current.selectedDocument?.id,
                    answerMode = current.answerMode,
                )
            } else {
                repository.quickSearch(
                    query = question,
                    documentId = current.selectedDocument?.id,
                    answerMode = current.answerMode,
                )
            }

            result.onSuccess { response ->
                _studyUiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        messages = state.messages.map { item ->
                            if (item.id == msgId) item.copy(response = response, isLoading = false) else item
                        },
                    )
                }
                if (current.saveToHistory) {
                    refreshHistory()
                }
            }.onFailure { err ->
                _studyUiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        messages = state.messages.map { item ->
                            if (item.id == msgId) item.copy(error = err.message, isLoading = false) else item
                        },
                    )
                }
            }
        }
    }

    fun clearStudyThread() {
        _studyUiState.update { it.copy(messages = emptyList()) }
    }

    // --- Library ---
    fun refreshLibrary() {
        viewModelScope.launch {
            _libraryUiState.update { it.copy(isLoading = true, error = null) }
            val catsResult = repository.listCategories()
            val currentState = _libraryUiState.value
            val docsResult = repository.listDocuments(currentState.searchQuery, currentState.selectedCategoryId)

            val categories = catsResult.getOrElse { currentState.categories }
            docsResult.onSuccess { docs ->
                _libraryUiState.update {
                    it.copy(
                        categories = categories,
                        documents = docs,
                        isLoading = false,
                        error = null,
                    )
                }
            }.onFailure { err ->
                _libraryUiState.update {
                    it.copy(
                        categories = categories,
                        isLoading = false,
                        error = err.message,
                    )
                }
            }
        }
    }

    fun onLibrarySearchQueryChange(query: String) {
        _libraryUiState.update { it.copy(searchQuery = query) }
        refreshLibrary()
    }

    fun selectLibraryCategory(categoryId: String?) {
        _libraryUiState.update {
            it.copy(selectedCategoryId = if (it.selectedCategoryId == categoryId) null else categoryId)
        }
        refreshLibrary()
    }

    // --- Document Reader ---
    fun openDocumentReader(documentId: String, title: String, initialPage: Int = 1) {
        val safePage = initialPage.coerceAtLeast(1)
        _readerUiState.value = ReaderUiState(
            isOpen = true,
            documentId = documentId,
            documentTitle = title,
            currentPage = safePage,
            isLoadingPage = true,
        )
        viewModelScope.launch {
            repository.getDocumentDetail(documentId).onSuccess { detail ->
                _readerUiState.update {
                    it.copy(
                        document = detail.document,
                        documentTitle = detail.document.title,
                        pageCount = detail.document.page_count.coerceAtLeast(1),
                    )
                }
            }
            loadReaderPage(documentId, safePage)
        }
    }

    fun loadReaderPage(documentId: String = _readerUiState.value.documentId, page: Int) {
        if (documentId.isBlank() || page < 1) return
        viewModelScope.launch {
            _readerUiState.update { it.copy(isLoadingPage = true, error = null, currentPage = page) }
            repository.getDocumentPage(documentId, page)
                .onSuccess { res ->
                    _readerUiState.update {
                        it.copy(
                            documentTitle = res.document_title,
                            pageCount = res.page_count.coerceAtLeast(1),
                            currentPage = res.page_number,
                            pageText = res.original_text,
                            isLoadingPage = false,
                            error = null,
                        )
                    }
                }
                .onFailure { err ->
                    _readerUiState.update { it.copy(isLoadingPage = false, error = err.message) }
                }
        }
    }

    fun downloadPdfForViewing() {
        val state = _readerUiState.value
        if (state.documentId.isBlank() || state.isDownloadingPdf) return
        viewModelScope.launch {
            _readerUiState.update { it.copy(isDownloadingPdf = true, error = null) }
            val suggestedName = state.document?.file_name?.ifBlank { "${state.documentTitle}.pdf" } ?: "${state.documentId}.pdf"
            repository.downloadDocumentPdf(state.documentId, suggestedName)
                .onSuccess { file ->
                    _readerUiState.update { it.copy(isDownloadingPdf = false, downloadedPdfFile = file) }
                }
                .onFailure { err ->
                    _readerUiState.update { it.copy(isDownloadingPdf = false, error = err.message) }
                }
        }
    }

    fun closeDocumentReader() {
        _readerUiState.value = ReaderUiState()
    }

    // --- History ---
    fun refreshHistory(documentId: String? = _historyUiState.value.filterDocumentId) {
        viewModelScope.launch {
            _historyUiState.update { it.copy(isLoading = true, error = null, filterDocumentId = documentId) }
            repository.getChatHistory(documentId)
                .onSuccess { list ->
                    _historyUiState.update { it.copy(items = list, isLoading = false, error = null) }
                }
                .onFailure { err ->
                    _historyUiState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            _historyUiState.update { it.copy(isClearing = true) }
            repository.clearChatHistory()
                .onSuccess {
                    _historyUiState.update { it.copy(items = emptyList(), isClearing = false) }
                }
                .onFailure { err ->
                    _historyUiState.update { it.copy(isClearing = false, error = err.message) }
                }
        }
    }

    // --- Admin Studio ---
    fun refreshAdminData() {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isLoading = true, error = null) }
            val cats = repository.listCategories().getOrElse { _adminUiState.value.categories }
            val status = _adminUiState.value.statusFilter
            repository.listAdminDocuments(status = status)
                .onSuccess { docs ->
                    _adminUiState.update {
                        it.copy(documents = docs, categories = cats, isLoading = false, error = null)
                    }
                }
                .onFailure { err ->
                    _adminUiState.update {
                        it.copy(categories = cats, isLoading = false, error = err.message)
                    }
                }
        }
    }

    fun setAdminStatusFilter(status: String?) {
        _adminUiState.update { it.copy(statusFilter = status) }
        refreshAdminData()
    }

    fun uploadAdminPdf(uri: Uri, title: String, description: String, categoryId: String?) {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isMutating = true, error = null, feedbackMessage = null) }
            repository.uploadPdfFromUri(uri, title, description, categoryId)
                .onSuccess { res ->
                    val statsText = res.stats?.let { " (${it.pageCount} pages, ${it.chunkCount} chunks indexed)" } ?: ""
                    _adminUiState.update {
                        it.copy(
                            isMutating = false,
                            feedbackMessage = "Uploaded ${res.document?.title ?: "PDF"}$statsText",
                        )
                    }
                    refreshAdminData()
                    refreshLibrary()
                }
                .onFailure { err ->
                    _adminUiState.update { it.copy(isMutating = false, error = err.message) }
                }
        }
    }

    fun updateAdminDocument(id: String, title: String, description: String, categoryId: String?) {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isMutating = true, error = null, feedbackMessage = null) }
            repository.updateAdminDocument(id, title, description, categoryId)
                .onSuccess {
                    _adminUiState.update { it.copy(isMutating = false, feedbackMessage = "Document updated.") }
                    refreshAdminData()
                    refreshLibrary()
                }
                .onFailure { err ->
                    _adminUiState.update { it.copy(isMutating = false, error = err.message) }
                }
        }
    }

    fun reprocessAdminDocument(id: String) {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isMutating = true, error = null, feedbackMessage = null) }
            repository.reprocessAdminDocument(id)
                .onSuccess { res ->
                    val stats = res.stats
                    _adminUiState.update {
                        it.copy(
                            isMutating = false,
                            feedbackMessage = if (stats != null) {
                                "Re-indexed ${stats.pageCount} pages (${stats.chunkCount} chunks)."
                            } else {
                                "Re-indexing completed."
                            },
                        )
                    }
                    refreshAdminData()
                    refreshLibrary()
                }
                .onFailure { err ->
                    _adminUiState.update { it.copy(isMutating = false, error = err.message) }
                }
        }
    }

    fun deleteAdminDocument(id: String) {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isMutating = true, error = null, feedbackMessage = null) }
            repository.deleteAdminDocument(id)
                .onSuccess {
                    _adminUiState.update { it.copy(isMutating = false, feedbackMessage = "Document deleted.") }
                    refreshAdminData()
                    refreshLibrary()
                }
                .onFailure { err ->
                    _adminUiState.update { it.copy(isMutating = false, error = err.message) }
                }
        }
    }

    fun createCategory(name: String, description: String) {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isMutating = true, error = null, feedbackMessage = null) }
            repository.createCategory(name, description)
                .onSuccess { cat ->
                    _adminUiState.update { it.copy(isMutating = false, feedbackMessage = "Category '${cat.name}' created.") }
                    refreshAdminData()
                    refreshLibrary()
                }
                .onFailure { err ->
                    _adminUiState.update { it.copy(isMutating = false, error = err.message) }
                }
        }
    }

    fun updateCategory(id: String, name: String, description: String) {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isMutating = true, error = null, feedbackMessage = null) }
            repository.updateCategory(id, name, description)
                .onSuccess { cat ->
                    _adminUiState.update { it.copy(isMutating = false, feedbackMessage = "Category '${cat.name}' updated.") }
                    refreshAdminData()
                    refreshLibrary()
                }
                .onFailure { err ->
                    _adminUiState.update { it.copy(isMutating = false, error = err.message) }
                }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            _adminUiState.update { it.copy(isMutating = true, error = null, feedbackMessage = null) }
            repository.deleteCategory(id)
                .onSuccess {
                    _adminUiState.update { it.copy(isMutating = false, feedbackMessage = "Category deleted.") }
                    refreshAdminData()
                    refreshLibrary()
                }
                .onFailure { err ->
                    _adminUiState.update { it.copy(isMutating = false, error = err.message) }
                }
        }
    }

    fun dismissAdminFeedback() {
        _adminUiState.update { it.copy(feedbackMessage = null, error = null) }
    }
}
