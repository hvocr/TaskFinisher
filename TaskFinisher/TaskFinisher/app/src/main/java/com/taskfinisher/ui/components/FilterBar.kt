package com.taskfinisher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.taskfinisher.ui.theme.*
import com.taskfinisher.viewmodel.TaskFilter

@Composable
fun FilterBar(
    activeFilter: TaskFilter,
    onFilterSelect: (TaskFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        TaskFilter.ALL to "All",
        TaskFilter.HIGH_IMPORTANCE to "🔴 High",
        TaskFilter.MEDIUM_IMPORTANCE to "🟡 Medium",
        TaskFilter.LOW_IMPORTANCE to "⚪ Low",
        TaskFilter.HIGH_ENERGY to "⚡ High Energy",
        TaskFilter.OVERDUE to "⏰ Overdue",
        TaskFilter.RECURRING to "🔄 Recurring"
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (filter, label) ->
            val selected = activeFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Accent.copy(alpha = 0.2f) else SurfaceVariant)
                    .border(1.dp, if (selected) Accent else Divider, RoundedCornerShape(20.dp))
                    .clickable { onFilterSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) Accent else TextSecondary
                )
            }
        }
    }
}
