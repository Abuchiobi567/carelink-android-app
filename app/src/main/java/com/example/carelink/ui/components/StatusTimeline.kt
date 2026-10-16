package com.example.carelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.models.RequestStatus

// dots + labels showing how far the request has got
@Composable
fun StatusTimeline(currentStatus: String) {
    val steps = RequestStatus.TIMELINE_ORDER
    val currentIndex = steps.indexOf(currentStatus)
    // ESCALATED is a side branch - treat it as "visit completed" for the progress bar
    val effectiveIndex = if (currentStatus == RequestStatus.ESCALATED) {
        steps.indexOf(RequestStatus.VISIT_COMPLETED)
    } else {
        currentIndex
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            steps.forEachIndexed { index, _ ->
                val reached = index <= effectiveIndex
                StatusDot(reached = reached)
                if (index != steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (index < effectiveIndex) MaterialTheme.colorScheme.primary
                                else Color.LightGray
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEach { status ->
                Text(
                    text = RequestStatus.displayName(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status == currentStatus) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (status == currentStatus) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        if (currentStatus == RequestStatus.ESCALATED) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Escalated",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun StatusDot(reached: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(
                if (reached) MaterialTheme.colorScheme.primary
                else Color.LightGray
            )
    )
}