package com.taskfinisher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.taskfinisher.ui.theme.*
import com.taskfinisher.utils.RecurrenceHelper
import java.time.DayOfWeek

@Composable
fun RecurrencePicker(
    rule: String?,
    onRuleChanged: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(rule != null) }
    var enabled by remember { mutableStateOf(rule != null) }
    var freq by remember { mutableStateOf(RecurrenceHelper.Frequency.DAILY) }
    var interval by remember { mutableStateOf(1) }
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var monthDay by remember { mutableStateOf(1) }
    var showFreqMenu by remember { mutableStateOf(false) }

    // Parse existing rule on first composition
    LaunchedEffect(rule) {
        rule?.let {
            RecurrenceHelper.parseRule(it)?.let { p ->
                freq = p.freq; interval = p.interval
                selectedDays = p.byDay.toSet()
                monthDay = p.byMonthDay ?: 1
            }
        }
    }

    fun emitRule() {
        if (!enabled) { onRuleChanged(null); return }
        onRuleChanged(RecurrenceHelper.buildRule(freq, interval,
            if (freq == RecurrenceHelper.Frequency.WEEKLY) selectedDays.toList() else emptyList(),
            if (freq == RecurrenceHelper.Frequency.MONTHLY) monthDay else null
        ))
    }

    Column(modifier.fillMaxWidth()) {
        // ── Header row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Repeat", style = MaterialTheme.typography.titleMedium, color = TextPrimary,
                modifier = Modifier.weight(1f))
            if (rule != null) {
                Text(RecurrenceHelper.describeRule(rule),
                    style = MaterialTheme.typography.bodyMedium, color = AccentTeal)
                Spacer(Modifier.width(8.dp))
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null, tint = TextSecondary)
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enable toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable recurrence", style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it; emitRule()
                    }, colors = SwitchDefaults.colors(checkedThumbColor = Accent,
                        checkedTrackColor = Accent.copy(alpha = 0.3f)))
                }

                if (enabled) {
                    // Frequency selector
                    Text("Frequency", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Box {
                        OutlinedButton(
                            onClick = { showFreqMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text(freq.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                        DropdownMenu(expanded = showFreqMenu,
                            onDismissRequest = { showFreqMenu = false },
                            modifier = Modifier.background(SurfaceVariant)) {
                            RecurrenceHelper.Frequency.entries.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = TextPrimary) },
                                    onClick = { freq = f; showFreqMenu = false; emitRule() }
                                )
                            }
                        }
                    }

                    // Interval
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Every", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        OutlinedTextField(
                            value = interval.toString(),
                            onValueChange = { v ->
                                interval = v.toIntOrNull()?.coerceIn(1, 99) ?: 1
                                emitRule()
                            },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                            )
                        )
                        val unitLabel = when (freq) {
                            RecurrenceHelper.Frequency.DAILY,
                            RecurrenceHelper.Frequency.CUSTOM -> "day(s)"
                            RecurrenceHelper.Frequency.WEEKLY -> "week(s)"
                            RecurrenceHelper.Frequency.MONTHLY -> "month(s)"
                        }
                        Text(unitLabel, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }

                    // Weekly day selector
                    if (freq == RecurrenceHelper.Frequency.WEEKLY) {
                        Text("On days", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(DayOfWeek.MONDAY to "M", DayOfWeek.TUESDAY to "T",
                                DayOfWeek.WEDNESDAY to "W", DayOfWeek.THURSDAY to "T",
                                DayOfWeek.FRIDAY to "F", DayOfWeek.SATURDAY to "S",
                                DayOfWeek.SUNDAY to "S").forEach { (day, label) ->
                                val sel = day in selectedDays
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sel) Accent else SurfaceVariant)
                                        .border(1.dp, if (sel) Accent else Divider, RoundedCornerShape(6.dp))
                                        .clickable {
                                            selectedDays = if (sel) selectedDays - day else selectedDays + day
                                            emitRule()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall,
                                        color = if (sel) OnAccent else TextSecondary)
                                }
                            }
                        }
                    }

                    // Monthly day picker
                    if (freq == RecurrenceHelper.Frequency.MONTHLY) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("On day", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            OutlinedTextField(
                                value = monthDay.toString(),
                                onValueChange = { v ->
                                    monthDay = v.toIntOrNull()?.coerceIn(1, 31) ?: 1
                                    emitRule()
                                },
                                modifier = Modifier.width(72.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                )
                            )
                            Text("of each month", style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
