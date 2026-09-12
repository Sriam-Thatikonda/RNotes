package com.baverika.notoir

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baverika.notoir.ui.editor.EditorScreen
import com.baverika.notoir.ui.editor.EditorViewModel
import com.baverika.notoir.ui.home.HomeScreen
import com.baverika.notoir.ui.home.HomeViewModel
import com.baverika.notoir.ui.lock.LockScreen
import com.baverika.notoir.ui.lock.LockState
import com.baverika.notoir.ui.lock.LockViewModel
import com.baverika.notoir.ui.lock.SetupPasswordScreen
import com.baverika.notoir.ui.theme.NotoirTheme

sealed interface Screen {
    data object Home : Screen
    data class Editor(val noteId: String? = null, val instanceKey: String = java.util.UUID.randomUUID().toString()) : Screen
}

class MainActivity : ComponentActivity() {

    private val app by lazy { application as NotoirApplication }

    private val lockViewModel: LockViewModel by viewModels {
        LockViewModel.Factory(app.passwordManager)
    }

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(app.noteRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by homeViewModel.themeMode.collectAsState()
            val lockState by lockViewModel.lockState.collectAsState()
            val unlockError by lockViewModel.unlockError.collectAsState()
            val setupError by lockViewModel.setupError.collectAsState()

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

            NotoirTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = lockState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "vaultLockTransition"
                    ) { state ->
                        when (state) {
                            LockState.Checking -> {
                                // Splash/Loading placeholder
                            }
                            LockState.NeedsSetup -> {
                                SetupPasswordScreen(
                                    errorMessage = setupError,
                                    onSetupCompleted = { pass, confirm, ack ->
                                        lockViewModel.setupPassword(pass, confirm, ack)
                                    },
                                    onSkipSetup = {
                                        lockViewModel.skipSetup()
                                    }
                                )
                            }
                            LockState.Locked -> {
                                LockScreen(
                                    errorMessage = unlockError,
                                    onUnlock = { password ->
                                        lockViewModel.unlock(password)
                                    }
                                )
                            }
                            LockState.Unlocked -> {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "screenNavigationTransition"
                                ) { screen ->
                                    when (screen) {
                                        is Screen.Home -> {
                                            HomeScreen(
                                                viewModel = homeViewModel,
                                                lockViewModel = lockViewModel,
                                                onNoteClick = { noteId ->
                                                    currentScreen = Screen.Editor(noteId)
                                                },
                                                onCreateNoteClick = {
                                                    currentScreen = Screen.Editor(null)
                                                },
                                                onLockApp = {
                                                    lockViewModel.lockNow()
                                                }
                                            )
                                        }
                                        is Screen.Editor -> {
                                            val editorViewModel: EditorViewModel = viewModel(
                                                key = screen.instanceKey,
                                                factory = EditorViewModel.Factory(
                                                    app.noteRepository,
                                                    screen.noteId
                                                )
                                            )
                                            EditorScreen(
                                                viewModel = editorViewModel,
                                                onNavigateBack = {
                                                    currentScreen = Screen.Home
                                                }
                                            )
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

    override fun onStop() {
        super.onStop()
        // Auto-lock on app backgrounding / switching tasks
        lockViewModel.onAppBackgrounded()
    }
}
