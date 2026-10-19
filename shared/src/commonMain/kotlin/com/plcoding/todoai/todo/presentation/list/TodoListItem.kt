package com.plcoding.todoai.todo.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plcoding.todoai.core.designsystem.AppTheme
import org.jetbrains.compose.resources.stringResource
import todoai.shared.generated.resources.Res
import todoai.shared.generated.resources.cd_mark_done
import todoai.shared.generated.resources.cd_mark_not_done
import todoai.shared.generated.resources.todo_added_at

@Composable
internal fun TodoListItem(
    title: String,
    description: String,
    formattedCreatedAt: String,
    isDone: Boolean,
    onClick: () -> Unit,
    onDoneToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val toggleDescription = stringResource(
                if (isDone) Res.string.cd_mark_not_done else Res.string.cd_mark_done
            )
            Checkbox(
                checked = isDone,
                onCheckedChange = onDoneToggle,
                modifier = Modifier.semantics { contentDescription = toggleDescription }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(Res.string.todo_added_at, formattedCreatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Open")
@Composable
private fun TodoListItemOpenPreview() {
    AppTheme {
        TodoListItem(
            title = "Buy groceries",
            description = "Milk, eggs, bread and some fruit for the week",
            formattedCreatedAt = "Oct 19, 2026, 09:30",
            isDone = false,
            onClick = {},
            onDoneToggle = {}
        )
    }
}

@Preview(name = "Done")
@Composable
private fun TodoListItemDonePreview() {
    AppTheme {
        TodoListItem(
            title = "Book dentist appointment",
            description = "",
            formattedCreatedAt = "Oct 18, 2026, 17:05",
            isDone = true,
            onClick = {},
            onDoneToggle = {}
        )
    }
}

@Preview(name = "Long text")
@Composable
private fun TodoListItemLongTextPreview() {
    AppTheme {
        TodoListItem(
            title = "Prepare the slides and the demo project for the on-device AI agents video",
            description = "Cover tool calling, the agent loop, and how the model reads and updates the to-do list without any network access",
            formattedCreatedAt = "Oct 17, 2026, 11:12",
            isDone = false,
            onClick = {},
            onDoneToggle = {}
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun TodoListItemDarkPreview() {
    AppTheme(darkTheme = true) {
        TodoListItem(
            title = "Buy groceries",
            description = "Milk, eggs, bread and some fruit for the week",
            formattedCreatedAt = "Oct 19, 2026, 09:30",
            isDone = false,
            onClick = {},
            onDoneToggle = {}
        )
    }
}
