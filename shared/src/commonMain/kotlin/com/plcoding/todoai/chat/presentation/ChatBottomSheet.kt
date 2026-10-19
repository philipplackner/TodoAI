package com.plcoding.todoai.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plcoding.todoai.core.designsystem.AppIcons
import com.plcoding.todoai.agent.domain.ModelState
import com.plcoding.todoai.core.designsystem.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.cd_new_chat
import todoai.shared.generated.resources.cd_send_message
import todoai.shared.generated.resources.chat_error_empty_answer
import todoai.shared.generated.resources.chat_error_generic
import todoai.shared.generated.resources.chat_empty_description
import todoai.shared.generated.resources.chat_empty_title
import todoai.shared.generated.resources.chat_input_placeholder
import todoai.shared.generated.resources.chat_subtitle
import todoai.shared.generated.resources.chat_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBottomSheetRoot(
    onDismiss: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        ChatSheetContent(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.fillMaxHeight(0.85f)
        )
    }
}

@Composable
fun ChatSheetContent(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ChatHeader(
            showNewChatButton = state.messages.isNotEmpty(),
            onNewChatClick = { onAction(ChatAction.OnNewChatClick) },
            modifier = Modifier.padding(start = 24.dp, end = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

        when {
            !state.isModelReady -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ModelSetupCard(
                        modelState = state.modelState,
                        onDownloadClick = { onAction(ChatAction.OnDownloadModelClick) },
                        onRetryLoadClick = { onAction(ChatAction.OnRetryLoadModelClick) }
                    )
                }
            }
            state.messages.isEmpty() && !state.isGenerating -> {
                ChatEmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            else -> {
                ChatMessageList(
                    messages = state.messages,
                    isGenerating = state.isGenerating,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }

        ChatInputBar(
            inputState = state.input,
            isEnabled = state.isModelReady,
            canSend = state.canSend,
            onSendClick = { onAction(ChatAction.OnSendClick) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ChatMessageList(
    messages: List<ChatMessageUi>,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        reverseLayout = true,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
    ) {
        if (isGenerating) {
            item(key = "typing_indicator") {
                TypingIndicatorBubble(modifier = Modifier.animateItem())
            }
        }
        items(
            items = messages.asReversed(),
            key = { it.id }
        ) { message ->
            val itemModifier = Modifier.animateItem()
            when (message) {
                is ChatMessageUi.User -> ChatMessageBubble(
                    text = message.text,
                    isFromUser = true,
                    modifier = itemModifier
                )
                is ChatMessageUi.Assistant -> ChatMessageBubble(
                    text = message.text,
                    isFromUser = false,
                    modifier = itemModifier
                )
                is ChatMessageUi.ToolCall -> ToolCallChip(
                    toolName = message.toolName,
                    arguments = message.arguments,
                    modifier = itemModifier
                )
                is ChatMessageUi.Error -> ChatErrorBubble(
                    text = stringResource(
                        when (message.type) {
                            ChatError.GENERATION_FAILED -> Res.string.chat_error_generic
                            ChatError.EMPTY_ANSWER -> Res.string.chat_error_empty_answer
                        }
                    ),
                    modifier = itemModifier
                )
            }
        }
    }
}

@Composable
private fun ChatHeader(
    showNewChatButton: Boolean,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = AppIcons.Sparkle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.chat_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(Res.string.chat_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showNewChatButton) {
            IconButton(onClick = onNewChatClick) {
                Icon(
                    painter = AppIcons.Refresh,
                    contentDescription = stringResource(Res.string.cd_new_chat)
                )
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.chat_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.chat_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatInputBar(
    inputState: TextFieldState,
    isEnabled: Boolean,
    canSend: Boolean,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            state = inputState,
            enabled = isEnabled,
            placeholder = { Text(text = stringResource(Res.string.chat_input_placeholder)) },
            shape = RoundedCornerShape(24.dp),
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 4),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            onKeyboardAction = { onSendClick() },
            modifier = Modifier.weight(1f)
        )
        FilledIconButton(
            onClick = onSendClick,
            enabled = canSend
        ) {
            Icon(
                painter = AppIcons.Send,
                contentDescription = stringResource(Res.string.cd_send_message)
            )
        }
    }
}

private val previewMessages = listOf(
    ChatMessageUi.User(id = "1", text = "What's due next week?"),
    ChatMessageUi.ToolCall(id = "2", toolName = "get_todos", arguments = "next_week"),
    ChatMessageUi.Assistant(
        id = "3",
        text = "You have 2 to-dos due next week:\n• Buy groceries (Oct 27)\n• Record the on-device AI agent video (Oct 30)"
    ),
    ChatMessageUi.User(id = "4", text = "Mark the groceries one as done"),
    ChatMessageUi.Error(id = "5", type = ChatError.GENERATION_FAILED)
)

@Preview(name = "Conversation")
@Composable
private fun ChatSheetContentPreview() {
    AppTheme {
        ChatSheetContent(
            state = ChatState(messages = previewMessages, input = TextFieldState("Thanks!")),
            onAction = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Generating")
@Composable
private fun ChatSheetContentGeneratingPreview() {
    AppTheme {
        ChatSheetContent(
            state = ChatState(messages = previewMessages, isGenerating = true),
            onAction = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Empty")
@Composable
private fun ChatSheetContentEmptyPreview() {
    AppTheme {
        ChatSheetContent(
            state = ChatState(),
            onAction = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Model not downloaded")
@Composable
private fun ChatSheetContentNotDownloadedPreview() {
    AppTheme {
        ChatSheetContent(
            state = ChatState(modelState = ModelState.NotDownloaded),
            onAction = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun ChatSheetContentDarkPreview() {
    AppTheme(darkTheme = true) {
        ChatSheetContent(
            state = ChatState(messages = previewMessages),
            onAction = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
