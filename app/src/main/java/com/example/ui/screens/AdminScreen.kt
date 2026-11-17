package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryDto
import com.example.data.model.DocumentDto
import com.example.ui.components.formatFileSize
import com.example.ui.viewmodel.AdminUiState

@Composable
fun AdminScreen(
    adminState: AdminUiState,
    onRefresh: () -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onUploadPdf: (uri: Uri, title: String, description: String, categoryId: String?) -> Unit,
    onUpdateDocument: (id: String, title: String, description: String, categoryId: String?) -> Unit,
    onReprocessDocument: (id: String) -> Unit,
    onDeleteDocument: (id: String) -> Unit,
    onCreateCategory: (name: String, description: String) -> Unit,
    onUpdateCategory: (id: String, name: String, description: String) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = PDFs, 1 = Categories
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var editingDoc by remember { mutableStateOf<DocumentDto?>(null) }
    var deletingDoc by remember { mutableStateOf<DocumentDto?>(null) }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryDto?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryDto?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingUploadUri = uri
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("admin_tab_documents"),
                ) {
                    Text("Documents")
                }
                SegmentedButton(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("admin_tab_categories"),
                ) {
                    Text("Categories")
                }
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("admin_refresh_button"),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh))
            }
        }

        if (adminState.isMutating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (!adminState.feedbackMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = adminState.feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissFeedback) { Text("OK") }
                }
            }
        }

        if (!adminState.error.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = adminState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissFeedback) { Text("Dismiss") }
                }
            }
        }

        if (selectedSection == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    enabled = !adminState.isMutating,
                    modifier = Modifier.testTag("admin_upload_pdf_button"),
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = stringResource(R.string.cd_upload_pdf))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Study PDF")
                }
                Text(
                    text = "${adminState.documents.size} total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val statuses = listOf<String?>(null, "Completed", "Pending", "Processing", "Failed")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(statuses) { st ->
                    FilterChip(
                        selected = adminState.statusFilter == st,
                        onClick = { onStatusFilterChange(st) },
                        label = { Text(st ?: "All") },
                    )
                }
            }

            if (adminState.isLoading && adminState.documents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(adminState.documents, key = { it.id }) { doc ->
                        AdminDocumentCard(
                            doc = doc,
                            onEdit = { editingDoc = doc },
                            onReprocess = { onReprocessDocument(doc.id) },
                            onDelete = { deletingDoc = doc },
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { showNewCategoryDialog = true },
                    enabled = !adminState.isMutating,
                    modifier = Modifier.testTag("admin_new_category_button"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New Category")
                }
                Text(
                    text = "${adminState.categories.size} categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(adminState.categories, key = { it.id }) { cat ->
                    OutlinedCard(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                if (cat.description.isNotBlank()) {
                                    Text(
                                        text = cat.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = "${cat.document_count} documents",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Row {
                                IconButton(onClick = { editingCategory = cat }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit category")
                                }
                                IconButton(onClick = { deletingCategory = cat }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete category",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingUploadUri?.let { uri ->
        DocumentFormDialog(
            dialogTitle = "Upload PDF to Library",
            initialTitle = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".pdf") ?: "Study PDF",
            initialDescription = "",
            initialCategoryId = adminState.categories.firstOrNull()?.id,
            categories = adminState.categories,
            confirmLabel = "Upload & Index",
            onConfirm = { title, desc, catId ->
                pendingUploadUri = null
                onUploadPdf(uri, title, desc, catId)
            },
            onDismiss = { pendingUploadUri = null },
        )
    }

    editingDoc?.let { doc ->
        DocumentFormDialog(
            dialogTitle = "Edit PDF Metadata",
            initialTitle = doc.title,
            initialDescription = doc.description,
            initialCategoryId = doc.category_id,
            categories = adminState.categories,
            confirmLabel = "Save",
            onConfirm = { title, desc, catId ->
                editingDoc = null
                onUpdateDocument(doc.id, title, desc, catId)
            },
            onDismiss = { editingDoc = null },
        )
    }

    deletingDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { deletingDoc = null },
            title = { Text("Delete PDF?") },
            text = { Text("Delete '${doc.title}' from Cloudflare R2 and D1 index?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        deletingDoc = null
                        onDeleteDocument(doc.id)
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingDoc = null }) { Text("Cancel") }
            },
        )
    }

    if (showNewCategoryDialog) {
        CategoryFormDialog(
            dialogTitle = "Create Category",
            initialName = "",
            initialDescription = "",
            onConfirm = { name, desc ->
                showNewCategoryDialog = false
                onCreateCategory(name, desc)
            },
            onDismiss = { showNewCategoryDialog = false },
        )
    }

    editingCategory?.let { cat ->
        CategoryFormDialog(
            dialogTitle = "Edit Category",
            initialName = cat.name,
            initialDescription = cat.description,
            onConfirm = { name, desc ->
                editingCategory = null
                onUpdateCategory(cat.id, name, desc)
            },
            onDismiss = { editingCategory = null },
        )
    }

    deletingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete Category?") },
            text = { Text("Delete '${cat.name}'? Documents in this category will become uncategorized.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        deletingCategory = null
                        onDeleteCategory(cat.id)
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AdminDocumentCard(
    doc: DocumentDto,
    onEdit: () -> Unit,
    onReprocess: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusColor = when (doc.processing_status) {
        "Completed" -> MaterialTheme.colorScheme.tertiaryContainer
        "Failed" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val statusTextColor = when (doc.processing_status) {
        "Completed" -> MaterialTheme.colorScheme.onTertiaryContainer
        "Failed" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    OutlinedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = statusColor,
                ) {
                    Text(
                        text = doc.processing_status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Text(
                text = "${doc.category_name ?: "Uncategorized"} • ${doc.page_count} pages • ${formatFileSize(doc.file_size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (doc.status_message.isNotBlank()) {
                Text(
                    text = doc.status_message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                TextButton(onClick = onReprocess) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Re-index")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentFormDialog(
    dialogTitle: String,
    initialTitle: String,
    initialDescription: String,
    initialCategoryId: String?,
    categories: List<CategoryDto>,
    confirmLabel: String,
    onConfirm: (title: String, description: String, categoryId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedCategoryId by remember { mutableStateOf(initialCategoryId) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedCatName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "None"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedCategoryId = null
                                dropdownExpanded = false
                            },
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    dropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotEmpty(),
                onClick = { onConfirm(title, description, selectedCategoryId) },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CategoryFormDialog(
    dialogTitle: String,
    initialName: String,
    initialDescription: String,
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name (2-60 chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().length >= 2,
                onClick = { onConfirm(name, description) },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
