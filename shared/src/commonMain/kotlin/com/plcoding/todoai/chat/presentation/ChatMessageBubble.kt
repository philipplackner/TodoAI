package com.plcoding.todoai.chat.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.todoai.core.designsystem.AppIcons
import com.plcoding.todoai.core.designsystem.AppTheme

@Composable
internal fun ChatMessageBubble(
    text: String,
    isFromUser: Boolean,
    modifier: Modifier = Modifier
) {
    ChatBubbleContainer(
        isFromUser = isFromUser,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
internal fun ChatErrorBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Shows which tool the agent called, so it's visible how the answer came about.
 */
@Composable
internal fun ToolCallChip(
    toolName: String,
    arguments: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = AppIcons.Build,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (arguments.isBlank()) toolName else "$toolName · $arguments",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun TypingIndicatorBubble(
    modifier: Modifier = Modifier
) {
    ChatBubbleContainer(
        isFromUser = false,
        modifier = modifier
    ) {
        val transition = rememberInfiniteTransition()
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 500),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(offsetMillis = index * 150)
                    )
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleContainer(
    isFromUser: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isFromUser) 20.dp else 4.dp,
                bottomEnd = if (isFromUser) 4.dp else 20.dp
            ),
            color = if (isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (isFromUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.widthIn(max = 300.dp),
            content = content
        )
    }
}

@Preview(name = "User message")
@Composable
private fun ChatMessageBubbleUserPreview() {
    AppTheme {
        ChatMessageBubble(
            text = "Add a to-do to buy milk tomorrow",
            isFromUser = true
        )
    }
}

@Preview(name = "Assistant message")
@Composable
private fun ChatMessageBubbleAssistantPreview() {
    AppTheme {
        ChatMessageBubble(
            text = "Done! I added \"Buy milk\" to your list.",
            isFromUser = false
        )
    }
}

@Preview(name = "Long message")
@Composable
private fun ChatMessageBubbleLongPreview() {
    AppTheme {
        ChatMessageBubble(
            text = "Please mark everything related to the video as done, and then add a new to-do for editing with a short description of the cuts I still need to make.",
            isFromUser = true
        )
    }
}

@Preview(name = "Tool call")
@Composable
private fun ToolCallChipPreview() {
    AppTheme {
        ToolCallChip(
            toolName = "get_todos",
            arguments = "next_week"
        )
    }
}

@Preview(name = "Error")
@Composable
private fun ChatErrorBubblePreview() {
    AppTheme {
        ChatErrorBubble(text = "Something went wrong while generating an answer. Please try again.")
    }
}

@Preview(name = "Typing indicator")
@Composable
private fun TypingIndicatorBubblePreview() {
    AppTheme {
        TypingIndicatorBubble()
    }
}

@Preview(name = "Dark")
@Composable
private fun ChatMessageBubbleDarkPreview() {
    AppTheme(darkTheme = true) {
        ChatMessageBubble(
            text = "Add a to-do to buy milk tomorrow",
            isFromUser = true
        )
    }
}
