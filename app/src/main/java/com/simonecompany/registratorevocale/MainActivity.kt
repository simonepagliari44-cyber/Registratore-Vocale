package com.simonecompany.registratorevocale

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simonecompany.registratorevocale.data.AudioRecorder
import com.simonecompany.registratorevocale.data.RecordingRepository
import com.simonecompany.registratorevocale.model.Recording
import com.simonecompany.registratorevocale.ui.ListScreen
import com.simonecompany.registratorevocale.ui.PlayerBottomSheet
import com.simonecompany.registratorevocale.ui.RecordScreen
import com.simonecompany.registratorevocale.ui.theme.RegistratoreVocaleTheme
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: RecordingRepository
) : ViewModel() {

    val recordings = mutableStateOf<List<Recording>>(emptyList())

    fun refresh() {
        viewModelScope.launch {
            recordings.value = repository.getRecordings()
        }
    }

    fun delete(recording: Recording) {
        viewModelScope.launch {
            repository.delete(recording)
            recordings.value = repository.getRecordings()
        }
    }

    fun toggleFavorite(recording: Recording) {
        viewModelScope.launch {
            repository.setFavorite(recording, !recording.favorite)
            recordings.value = repository.getRecordings()
        }
    }

    fun rename(recording: Recording, newName: String) {
        viewModelScope.launch {
            repository.rename(recording, newName)
            recordings.value = repository.getRecordings()
        }
    }
}

class MainActivity : ComponentActivity() {

    private val repository by lazy { RecordingRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(repository) as T
            }
        )[MainViewModel::class.java]

        setContent {
            RegistratoreVocaleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(
                        viewModel = viewModel,
                        repository = repository
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: MainViewModel,
    repository: RecordingRepository
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var playbackRecording by remember { mutableStateOf<Recording?>(null) }

    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context.applicationContext) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    val hasPermission = remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission.value = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    val recordings by viewModel.recordings

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    playbackRecording?.let { recording ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { playbackRecording = null },
            sheetState = sheetState
        ) {
            PlayerBottomSheet(
                recording = recording,
                onClose = { playbackRecording = null },
                onRename = { newName ->
                    viewModel.rename(recording, newName)
                    playbackRecording = null
                }
            )
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                    label = { Text("Registra") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1; viewModel.refresh() },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Lista") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> {
                    if (!hasPermission.value) {
                        PermissionRequest(onRequest = {
                            permissionLauncher.launch(requiredPermissions.toTypedArray())
                        })
                    } else {
                        RecordScreen(
                            recorder = recorder,
                            onRecordingStarted = { },
                            onRecordingSaved = { viewModel.refresh() }
                        )
                    }
                }

                1 -> ListScreen(
                    recordings = recordings,
                    onDelete = { viewModel.delete(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onOpenPlayer = { playbackRecording = it }
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "Permesso microfono richiesto",
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onRequest) {
            Text("Concedi permesso")
        }
    }
}
