package com.cma.studyplanner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String = "",
    icon: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (icon.isNotEmpty()) {
                Text(icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    size: Int = 48,
    strokeWidth: Int = 5
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size.dp)
    ) {
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.fillMaxSize(),
            color = color.copy(alpha = 0.15f),
            strokeWidth = strokeWidth.dp,
        )
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = strokeWidth.dp,
        )
        Text(
            "${(progress * 100).toInt()}%",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CheckButton(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (checked) Modifier.background(Color(0xFF10B981))
                else Modifier.border(2.dp, Color(0xFFE4E6EF), RoundedCornerShape(6.dp))
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Done",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String = "",
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText.isNotEmpty() && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ScheduleItem(
    subject: String,
    chapter: String,
    time: String,
    completed: Boolean,
    color: Color,
    lightColor: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) Color(0xFFF0FDF4) else lightColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckButton(checked = completed, onToggle = onToggle)
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subject, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(chapter, fontSize = 12.sp, color = Color(0xFF8B8FA3))
                Text(time, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF8B8FA3), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String = "") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF8B8FA3), fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDialog(
    title: String,
    placeholder: String,
    confirmText: String = "Add",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    extraContent: @Composable (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                extraContent?.invoke()
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
