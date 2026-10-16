package com.example.carelink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.models.RequestStatus

// pill showing the current status, colour coded so the row reads at a glance
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val bg = when (status) {
        RequestStatus.DRAFT -> Color(0xFFE0E0E0)
        RequestStatus.SUBMITTED -> Color(0xFFBBDEFB)
        RequestStatus.UNDER_REVIEW -> Color(0xFFFFF59D)
        RequestStatus.ASSIGNED -> Color(0xFFFFCC80)
        RequestStatus.VISIT_COMPLETED -> Color(0xFFC5E1A5)
        RequestStatus.VERIFIED -> Color(0xFFA5D6A7)
        RequestStatus.ESCALATED -> Color(0xFFEF9A9A)
        else -> Color.LightGray
    }
    Text(
        text = RequestStatus.displayName(status),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = Color.Black,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.labelMedium
    )
}
