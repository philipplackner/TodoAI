package com.plcoding.todoai.chat.presentation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.plcoding.todoai.core.designsystem.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.cd_send_message
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
        ChatHeader(modifier = Modifier.padding(horizontal = 24.dp))
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

        if (state.messages.isEmpty() && !state.isGenerating) {
            ChatEmptyState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                if (state.isGenerating) {
                    item(key = "typing_indicator") {
                        TypingIndicatorBubble(modifier = Modifier.animateItem())
                    }
                }
                items(
                    items = state.messages.asReversed(),
                    key = { it.id }
                ) { message ->
                    ChatMessageBubble(
                        text = message.text,
                        isFromUser = message.isFromUser,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        ChatInputBar(
            input = state.input,
            canSend = state.canSend,
            onInputChange = { onAction(ChatAction.OnInputChange(it)) },
            onSendClick = { onAction(ChatAction.OnSendClick) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ChatHeader(
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
        Column {
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
    input: String,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text(text = stringResource(Res.string.chat_input_placeholder)) },
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { onSendClick() }),
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
    ChatMessageUi(id = "1", text = "What's still open on my list?", isFromUser = true),
    ChatMessageUi(
        id = "2",
        text = "You have 2 open to-dos: \"Buy groceries\" and \"Record the on-device AI agent video\".",
        isFromUser = false
    ),
    ChatMessageUi(id = "3", text = "Mark the groceries one as done", isFromUser = true)
)

@Preview(name = "Conversation")
@Composable
private fun ChatSheetContentPreview() {
    AppTheme {
        ChatSheetContent(
            state = ChatState(messages = previewMessages, input = "Thanks!"),
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
