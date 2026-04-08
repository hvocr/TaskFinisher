package com.taskfinisher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.taskfinisher.data.model.Task
import com.taskfinisher.ui.theme.*

@Composable
fun SubtaskList(
    subtasks: List<Task>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newSubtaskText by remember { mutableStateOf("") }

    Column(modifier.fillMaxWidth()) {
        Text("Subtasks", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        subtasks.forEach { subtask ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini checkbox
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, TextSecondary, CircleShape)
                        .clickable { onToggle(subtask.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (subtask.isCompleted) {
                        Icon(Icons.Filled.Check, null,
                            tint = AccentTeal, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    subtask.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (subtask.isCompleted) TextSecondary else TextPrimary,
                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onRemove(subtask.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.Close, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Add new subtask row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newSubtaskText,
                onValueChange = { newSubtaskText = it },
                placeholder = { Text("Add subtask…", color = TextSecondary) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    cursorColor = Accent
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newSubtaskText.isNotBlank()) {
                        onAdd(newSubtaskText.trim())
                        newSubtaskText = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent)
            ) {
                Icon(Icons.Filled.Add, "Add subtask", tint = OnAccent)
            }
        }
    }
}
