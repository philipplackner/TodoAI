package com.plcoding.todoai.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.todoai.agent.data.LocalModel
import com.plcoding.todoai.agent.domain.ModelError
import com.plcoding.todoai.agent.domain.ModelState
import com.plcoding.todoai.core.designsystem.AppIcons
import com.plcoding.todoai.core.designsystem.AppTheme
import org.jetbrains.compose.resources.stringResource
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.model_setup_description
import todoai.shared.generated.resources.model_setup_download
import todoai.shared.generated.resources.model_setup_download_failed
import todoai.shared.generated.resources.model_setup_downloading
import todoai.shared.generated.resources.model_setup_load_failed
import todoai.shared.generated.resources.model_setup_loading
import todoai.shared.generated.resources.model_setup_progress
import todoai.shared.generated.resources.model_setup_retry
import todoai.shared.generated.resources.model_setup_title

@Composable
internal fun ModelSetupCard(
    modelState: ModelState,
    onDownloadClick: () -> Unit,
    onRetryLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (modelState) {
                ModelState.NotDownloaded -> {
                    SetupTitle()
                    SetupText(
                        text = stringResource(
                            Res.string.model_setup_description,
                            LocalModel.DISPLAY_NAME,
                            LocalModel.SIZE_BYTES.toGigabytesText()
                        )
                    )
                    Button(onClick = onDownloadClick) {
                        Icon(
                            painter = AppIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(
                                Res.string.model_setup_download,
                                LocalModel.SIZE_BYTES.toGigabytesText()
                            )
                        )
                    }
                }
                is ModelState.Downloading -> {
                    Text(
                        text = stringResource(Res.string.model_setup_downloading, LocalModel.DISPLAY_NAME),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = { modelState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SetupText(
                        text = stringResource(
                            Res.string.model_setup_progress,
                            modelState.downloadedBytes.toGigabytesText(),
                            modelState.totalBytes.toGigabytesText()
                        )
                    )
                }
                ModelState.Loading -> {
                    CircularProgressIndicator()
                    SetupText(text = stringResource(Res.string.model_setup_loading))
                }
                is ModelState.Failed -> {
                    SetupText(
                        text = stringResource(
                            when (modelState.error) {
                                ModelError.DOWNLOAD_FAILED -> Res.string.model_setup_download_failed
                                ModelError.LOAD_FAILED -> Res.string.model_setup_load_failed
                            }
                        ),
                        isError = true
                    )
                    OutlinedButton(
                        onClick = when (modelState.error) {
                            ModelError.DOWNLOAD_FAILED -> onDownloadClick
                            ModelError.LOAD_FAILED -> onRetryLoadClick
                        }
                    ) {
                        Text(text = stringResource(Res.string.model_setup_retry))
                    }
                }
                ModelState.Ready -> Unit
            }
        }
    }
}

@Composable
private fun SetupTitle() {
    Text(
        text = stringResource(Res.string.model_setup_title),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SetupText(
    text: String,
    isError: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

private fun Long.toGigabytesText(): String {
    val tenthsOfGigabyte = (this + 50_000_000) / 100_000_000
    return "${tenthsOfGigabyte / 10}.${tenthsOfGigabyte % 10} GB"
}

@Preview(name = "Not downloaded")
@Composable
private fun ModelSetupCardNotDownloadedPreview() {
    AppTheme {
        ModelSetupCard(
            modelState = ModelState.NotDownloaded,
            onDownloadClick = {},
            onRetryLoadClick = {}
        )
    }
}

@Preview(name = "Downloading")
@Composable
private fun ModelSetupCardDownloadingPreview() {
    AppTheme {
        ModelSetupCard(
            modelState = ModelState.Downloading(
                downloadedBytes = 1_200_000_000,
                totalBytes = LocalModel.SIZE_BYTES
            ),
            onDownloadClick = {},
            onRetryLoadClick = {}
        )
    }
}

@Preview(name = "Loading")
@Composable
private fun ModelSetupCardLoadingPreview() {
    AppTheme {
        ModelSetupCard(
            modelState = ModelState.Loading,
            onDownloadClick = {},
            onRetryLoadClick = {}
        )
    }
}

@Preview(name = "Download failed")
@Composable
private fun ModelSetupCardFailedPreview() {
    AppTheme(darkTheme = true) {
        ModelSetupCard(
            modelState = ModelState.Failed(ModelError.DOWNLOAD_FAILED),
            onDownloadClick = {},
            onRetryLoadClick = {}
        )
    }
}
