package com.example.carelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.util.DeadlineFormatter

// small pill that shows the deadline as text. overdue items get the
// error colour so they stand out in lists.
@Composable
fun DeadlineChip(deadlineMillis: Long?, modifier: Modifier = Modifier) {
    val overdue = DeadlineFormatter.isOverdue(deadlineMillis)
    val (bg, fg) = if (overdue) {
        MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text = DeadlineFormatter.format(deadlineMillis),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.labelMedium
    )
}