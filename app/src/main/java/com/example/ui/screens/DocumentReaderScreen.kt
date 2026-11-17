package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DocumentDto
import com.example.ui.viewmodel.ReaderUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentReaderScreen(
    readerState: ReaderUiState,
    onClose: () -> Unit,
    onLoadPage: (Int) -> Unit,
    onDownloadPdf: () -> Unit,
    onAskAboutDocument: (DocumentDto) -> Unit,
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    var viewOriginalPdf by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInput by remember(readerState.currentPage) { mutableStateOf(readerState.currentPage.toString()) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pdfTotalPages by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(viewOriginalPdf, readerState.downloadedPdfFile, readerState.currentPage) {
        val file = readerState.downloadedPdfFile
        if (viewOriginalPdf && file != null && file.exists()) {
            val result = renderPdfPageBitmap(file, readerState.currentPage - 1)
            renderedBitmap = result.first
            pdfTotalPages = result.second
        }
    }

    val effectivePageCount = maxOf(readerState.pageCount, pdfTotalPages ?: 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = readerState.documentTitle.ifBlank { "Document Reader" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Page ${readerState.currentPage} of $effectivePageCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("reader_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    readerState.document?.let { doc ->
                        IconButton(
                            onClick = { onAskAboutDocument(doc) },
                            modifier = Modifier.testTag("reader_ask_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = "Ask about this PDF",
                            )
                        }
                    }
                    if (readerState.downloadedPdfFile != null) {
                        IconButton(
                            onClick = {
                                openExternalPdf(context, readerState.downloadedPdfFile)
                            },
                            modifier = Modifier.testTag("reader_open_external_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open PDF in external viewer",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { onLoadPage((readerState.currentPage - 1).coerceAtLeast(1)) },
                        enabled = readerState.currentPage > 1 && !readerState.isLoadingPage,
                        modifier = Modifier.testTag("reader_prev_page"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                        Text("Prev")
                    }

                    TextButton(
                        onClick = { showJumpDialog = true },
                        modifier = Modifier.testTag("reader_jump_page"),
                    ) {
                        Text("Page ${readerState.currentPage} / $effectivePageCount")
                    }

                    OutlinedButton(
                        onClick = { onLoadPage((readerState.currentPage + 1).coerceAtMost(effectivePageCount)) },
                        enabled = readerState.currentPage < effectivePageCount && !readerState.isLoadingPage,
                        modifier = Modifier.testTag("reader_next_page"),
                    ) {
                        Text("Next")
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Mode Switcher: Extracted Text vs Original PDF Render
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = !viewOriginalPdf,
                    onClick = { viewOriginalPdf = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("reader_tab_text"),
                ) {
                    Text("Extracted Text")
                }
                SegmentedButton(
                    selected = viewOriginalPdf,
                    onClick = {
                        viewOriginalPdf = true
                        if (readerState.downloadedPdfFile == null && !readerState.isDownloadingPdf) {
                            onDownloadPdf()
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("reader_tab_pdf"),
                ) {
                    Text("Original PDF")
                }
            }

            if (!readerState.error.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = readerState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (!viewOriginalPdf) {
                when {
                    readerState.isLoadingPage -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = readerState.pageText.ifBlank { "No text available for this page." },
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when {
                    readerState.isDownloadingPdf -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator()
                                Text("Downloading PDF from Cloudflare R2...")
                            }
                        }
                    }

                    readerState.downloadedPdfFile == null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Button(onClick = onDownloadPdf) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Download Original PDF")
                            }
                        }
                    }

                    renderedBitmap != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                bitmap = renderedBitmap!!.asImageBitmap(),
                                contentDescription = "PDF page ${readerState.currentPage}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                OutlinedTextField(
                    value = jumpInput,
                    onValueChange = { jumpInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Page (1 - $effectivePageCount)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = jumpInput.toIntOrNull()?.coerceIn(1, effectivePageCount) ?: 1
                        showJumpDialog = false
                        onLoadPage(target)
                    },
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private suspend fun renderPdfPageBitmap(file: File, pageIndex: Int): Pair<Bitmap?, Int?> =
    withContext(Dispatchers.IO) {
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count <= 0) return@use null to 0
                    val safeIndex = pageIndex.coerceIn(0, count - 1)
                    renderer.openPage(safeIndex).use { page ->
                        val scale = 2
                        val width = (page.width * scale).coerceIn(400, 2000)
                        val height = (page.height * scale).coerceIn(400, 2800)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap to count
                    }
                }
            }
        }.getOrElse { null to null }
    }

private fun openExternalPdf(context: android.content.Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF"))
    }
}
