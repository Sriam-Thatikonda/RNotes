package com.baverika.notoir.ui.home

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.baverika.notoir.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.notoir.domain.model.Note
import com.baverika.notoir.ui.home.components.NoteCard
import com.baverika.notoir.ui.home.components.SearchBarView
import com.baverika.notoir.ui.settings.AboutDialog
import com.baverika.notoir.ui.settings.DuplicateStrategyDialog
import com.baverika.notoir.ui.settings.ExportSecurityNoticeDialog
import com.baverika.notoir.ui.settings.SecuritySettingsDialog
import com.baverika.notoir.ui.lock.LockViewModel
import androidx.compose.material.icons.rounded.Security
import com.baverika.notoir.ui.theme.ThemeMode
import com.baverika.notoir.util.import.DuplicateStrategy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    lockViewModel: LockViewModel,
    onNoteClick: (String) -> Unit,
    onCreateNoteClick: () -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showExportWarningDialog by remember { mutableStateOf(false) }
    var showDuplicateStrategyDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val isLockEnabled by lockViewModel.isLockEnabled.collectAsState()
    val isPasswordConfigured by lockViewModel.isPasswordConfigured.collectAsState()

    // SAF Document Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    viewModel.exportNotes(outputStream) { result ->
                        result.onSuccess { count ->
                            Toast.makeText(context, "Exported $count notes successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(context, "Export failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showDuplicateStrategyDialog = true
        }
    }

    // Handle Snackbar messages with Undo
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = if (msg == "Note deleted") "Undo" else null,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                }
                viewModel.clearSnackbarMessage()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                SearchBarView(
                    query = searchQuery,
                    onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onCloseSearch = { viewModel.setSearchActive(false) }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    },
                    actions = {
                        // Search action
                        IconButton(onClick = { viewModel.setSearchActive(true) }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Layout toggle (Grid vs List)
                        IconButton(onClick = { viewModel.toggleLayoutMode() }) {
                            Icon(
                                imageVector = if (layoutMode == ViewLayoutMode.STAGGERED_GRID) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                contentDescription = "Toggle layout",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Overflow Menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import Notes") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch(arrayOf("application/json", "*/*"))
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Export Notes") },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showExportWarningDialog = true
                                    }
                                )

                                if (isLockEnabled) {
                                    DropdownMenuItem(
                                        text = { Text("Lock Vault Now") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Lock, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            onLockApp()
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Security & Password") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Security, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showSecurityDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (themeMode) {
                                                ThemeMode.SYSTEM -> "Theme: System"
                                                ThemeMode.LIGHT -> "Theme: Light"
                                                ThemeMode.DARK -> "Theme: Dark"
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            when (themeMode) {
                                                ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        val nextMode = when (themeMode) {
                                            ThemeMode.SYSTEM -> ThemeMode.DARK
                                            ThemeMode.DARK -> ThemeMode.LIGHT
                                            ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                        }
                                        viewModel.setThemeMode(nextMode)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("About ${stringResource(R.string.app_name)}") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Shield, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showAboutDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNoteClick,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New note"
                    )
                },
                text = {
                    Text(
                        text = "New note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Empty state when search produces 0 results
                isSearchActive && notes.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notes found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try a different search term or check spelling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Empty state on fresh install / 0 notes
                notes.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "No notes yet",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Capture an idea, task, or thought.\nYour notes stay completely private on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = onCreateNoteClick,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create note")
                        }
                    }
                }

                // Responsive Grid Presentation
                layoutMode == ViewLayoutMode.STAGGERED_GRID -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = notes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(
                                note = note,
                                searchQuery = searchQuery,
                                onClick = { onNoteClick(note.id) },
                                onLongClick = {
                                    viewModel.deleteNote(note) { msg ->
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = msg,
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Single Column List Presentation
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = notes,
                            key = { it.id }
                        ) { note ->
                            NoteCard(
                                note = note,
                                searchQuery = searchQuery,
                                onClick = { onNoteClick(note.id) },
                                onLongClick = {
                                    viewModel.deleteNote(note) { msg ->
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = msg,
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showExportWarningDialog) {
        ExportSecurityNoticeDialog(
            onConfirmExport = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                exportLauncher.launch("rnotes_backup_$timestamp.json")
            },
            onDismiss = { showExportWarningDialog = false }
        )
    }

    if (showDuplicateStrategyDialog) {
        DuplicateStrategyDialog(
            onStrategySelected = { strategy ->
                showDuplicateStrategyDialog = false
                pendingImportUri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            viewModel.importNotes(inputStream, strategy) { result ->
                                result.onSuccess { summary ->
                                    val msg = "Imported: ${summary.importedCount}, Replaced: ${summary.replacedCount}, Skipped: ${summary.skippedCount}"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }.onFailure { error ->
                                    Toast.makeText(context, "Import failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error reading file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    } finally {
                        pendingImportUri = null
                    }
                }
            },
            onDismiss = {
                showDuplicateStrategyDialog = false
                pendingImportUri = null
            }
        )
    }

    if (showSecurityDialog) {
        SecuritySettingsDialog(
            isLockEnabled = isLockEnabled,
            isPasswordConfigured = isPasswordConfigured,
            onDisableLock = { password ->
                lockViewModel.disableLock(password)
            },
            onEnableLock = {
                lockViewModel.enableLock()
            },
            onSetupPassword = { p, c, ack ->
                lockViewModel.setupPassword(p, c, ack)
            },
            onChangePassword = { old, new, c, ack ->
                lockViewModel.changePassword(old, new, c, ack)
            },
            onRemovePassword = { password ->
                lockViewModel.removePassword(password)
            },
            onDismiss = { showSecurityDialog = false }
        )
    }
}
