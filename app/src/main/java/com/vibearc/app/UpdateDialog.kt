package com.vibearc.app

import com.vibearc.app.GlassButton as Button
import com.vibearc.app.GlassTextButton as OutlinedButton
import com.vibearc.app.GlassTextButton as TextButton
import com.vibearc.app.GlassButton as FilledTonalButton
import com.vibearc.app.GlassIconButton as IconButton
import com.vibearc.app.GlassFilledIconButton as FilledIconButton

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@Composable
internal fun UpdateDialog(update: AppUpdate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember(update) { mutableStateOf(false) }
    var error by remember(update) { mutableStateOf<String?>(null) }
    var downloadedApk by remember(update) { mutableStateOf<File?>(null) }

    fun install(apk: File) {
        when (VerifiedUpdateInstaller.launchInstaller(context, apk)) {
            InstallLaunchResult.Started -> onDismiss()
            InstallLaunchResult.PermissionRequired -> error = "Allow installs from VibeArc, return here, then tap Install again."
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("VibeArc ${update.version} is available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (busy) "Downloading and verifying the signed APK…" else "VibeArc will verify the GitHub SHA-256 checksum, package name, version, and signing certificate before opening Android's installer.")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                error = null
                downloadedApk?.takeIf(File::isFile)?.let(::install) ?: scope.launch {
                    busy = true
                    val verified = withContext(Dispatchers.IO) {
                        runCatching { VerifiedUpdateInstaller.downloadAndVerify(context, update) }
                    }
                    busy = false
                    verified.onSuccess { apk -> downloadedApk = apk; install(apk) }
                        .onFailure { error = it.message ?: "The update could not be verified." }
                }
            }) { Text(if (downloadedApk == null) "Verify & install" else "Install") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl))) }
            }) { Text("Release page") }
        },
    )
}
