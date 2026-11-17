package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DocumentDto
import com.example.data.model.SearchMatchItem
import com.example.ui.components.AnswerModeBadge
import com.example.ui.components.formatScorePercent
import com.example.ui.viewmodel.StudyMessageItem
import com.example.ui.viewmodel.StudyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    studyState: StudyUiState,
    availableDocuments: List<DocumentDto>,
    onQuestionChange: (String) -> Unit,
    onSelectDocument: (DocumentDto?) -> Unit,
    onSelectAnswerMode: (String) -> Unit,
    onToggleSaveToHistory: (Boolean) -> Unit,
    onSubmitQuestion: () -> Unit,
    onClearThread: () -> Unit,
    onOpenCitationPage: (documentId: String, title: String, page: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDocPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(studyState.messages.size) {
        if (studyState.messages.isNotEmpty()) {
            listState.animateScrollToItem(studyState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Top Study Controls Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { showDocPicker = true },
                        label = {
                            Text(
                                text = studyState.selectedDocument?.title ?: "All Study PDFs (${availableDocuments.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter by PDF",
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = if (studyState.selectedDocument != null) {
                            {
                                IconButton(
                                    onClick = { onSelectDocument(null) },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear PDF filter",
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_doc_scope_chip"),
                    )

                    FilterChip(
                        selected = !studyState.saveToHistory,
                        onClick = { onToggleSaveToHistory(!studyState.saveToHistory) },
                        label = {
                            Text(if (studyState.saveToHistory) "Ask + Save" else "Quick Search")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (studyState.saveToHistory) Icons.Default.BookmarkAdded else Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.testTag("study_history_mode_chip"),
                    )

                    if (studyState.messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearThread,
                            modifier = Modifier.testTag("study_clear_thread_button"),
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear conversation",
                            )
                        }
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = studyState.answerMode == "smart_search",
                        onClick = { onSelectAnswerMode("smart_search") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("mode_smart_search"),
                    ) {
                        Text("Smart Search", maxLines = 1)
                    }
                    SegmentedButton(
                        selected = studyState.answerMode == "exact_answer",
                        onClick = { onSelectAnswerMode("exact_answer") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("mode_exact_answer"),
                    ) {
                        Text("Exact Answer", maxLines = 1)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        // Main Messages or Empty Welcome
        if (studyState.messages.isEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.img_study_hero),
                                contentDescription = stringResource(R.string.cd_app_hero),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(155.dp),
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Weydii PDF-yadaada Su'aal",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Deterministic bilingual English & Somali search. Every answer cites the exact PDF document and page number with zero AI hallucination.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AnswerModeBadge("smart_search")
                                    AnswerModeBadge("exact_answer")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Sida loo adeegsado (How it works)",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "• Smart Search: Finds the highest-ranked passage using BM25 + TF-IDF across English and Somali text.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "• Exact Answer: Returns the verbatim original sentence directly from your textbook page.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "• Tap any citation badge to jump straight to that PDF page in the built-in Reader.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(studyState.messages, key = { it.id }) { msg ->
                    StudyMessageCard(
                        item = msg,
                        onOpenCitationPage = onOpenCitationPage,
                    )
                }
            }
        }

        // Bottom Question Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = studyState.questionInput,
                    onValueChange = onQuestionChange,
                    placeholder = {
                        Text("Qor su'aashaada (English or Af-Soomaali)...")
                    },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmitQuestion() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("study_question_input"),
                )
                Surface(
                    onClick = onSubmitQuestion,
                    enabled = studyState.questionInput.trim().length >= 2 && !studyState.isSubmitting,
                    shape = MaterialTheme.shapes.medium,
                    color = if (studyState.questionInput.trim().length >= 2 && !studyState.isSubmitting) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("ask_send_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (studyState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send_question),
                                tint = if (studyState.questionInput.trim().length >= 2) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDocPicker) {
        ModalBottomSheet(onDismissRequest = { showDocPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Select PDF Scope",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Surface(
                    onClick = {
                        onSelectDocument(null)
                        showDocPicker = false
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = if (studyState.selectedDocument == null) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "All Completed PDFs (${availableDocuments.size})",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (studyState.selectedDocument == null) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availableDocuments, key = { it.id }) { doc ->
                        val selected = studyState.selectedDocument?.id == doc.id
                        Surface(
                            onClick = {
                                onSelectDocument(doc)
                                showDocPicker = false
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${doc.category_name ?: "General"} • ${doc.page_count} pages",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyMessageCard(
    item: StudyMessageItem,
    onOpenCitationPage: (documentId: String, title: String, page: Int) -> Unit,
) {
    var showAllMatches by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // User Question Bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth(0.86f),
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        // Answer Card
        OutlinedCard(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AnswerModeBadge(item.answerMode)
                        if (item.isQuickSearch) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item.response?.let { res ->
                        if (res.found) {
                            Text(
                                text = "Score ${formatScorePercent(res.relevance_score)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }

                when {
                    item.isLoading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Baaraya bogagga PDF-yada...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    item.error != null -> Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = item.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }

                    item.response != null -> {
                        val res = item.response
                        if (!res.found) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = res.answer.ifBlank { stringResource(R.string.no_answer_fallback) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        } else {
                            Text(
                                text = res.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            // Primary Citation Button
                            val docId = res.document_id
                            val pageNum = res.page_number
                            val docTitle = res.document_title ?: "PDF Document"
                            if (docId != null && pageNum != null) {
                                Surface(
                                    onClick = { onOpenCitationPage(docId, docTitle, pageNum) },
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Column {
                                                Text(
                                                    text = docTitle,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = "Page $pageNum • Tap to open reader",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Matched terms chips from top match
                            val topTerms = res.results.firstOrNull()?.matched_terms.orEmpty()
                            if (topTerms.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    topTerms.take(8).forEach { term ->
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                        ) {
                                            Text(
                                                text = "#$term",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // Secondary results expander
                            if (res.results.size > 1) {
                                TextButton(
                                    onClick = { showAllMatches = !showAllMatches },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = if (showAllMatches) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (showAllMatches) {
                                            "Hide other matches"
                                        } else {
                                            "Show ${res.results.size - 1} more passage(s)"
                                        },
                                    )
                                }

                                AnimatedVisibility(visible = showAllMatches) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        res.results.drop(1).forEach { match ->
                                            SecondaryMatchRow(match, onOpenCitationPage)
                                        }
                                    }
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
private fun SecondaryMatchRow(
    match: SearchMatchItem,
    onOpenCitationPage: (documentId: String, title: String, page: Int) -> Unit,
) {
    Surface(
        onClick = { onOpenCitationPage(match.document_id, match.document_title, match.page_number) },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${match.document_title} • p.${match.page_number}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatScorePercent(match.relevance_score),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = match.matched_text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
