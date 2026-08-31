package com.memoryball.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A tinted circular badge that sits behind a leading icon in cards / empty states. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 44,
    iconSize: Int = 22,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(containerColor, RoundedCornerShape((size * 0.34).dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

/** A compact tonal pill used to show a single condition (time / place / repeat). */
@Composable
fun MetaPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/** A titled, grouped card used to organize the edit / settings forms. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    accentIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (accentIcon != null) {
                    Icon(
                        accentIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/** A polished confirm dialog with an icon badge, tonal cancel and filled confirm. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Default.DeleteForever,
    confirmLabel: String = "删除",
    cancelLabel: String = "取消",
    danger: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = cs.surfaceContainerHigh
        ) {
            Column(Modifier.padding(24.dp)) {
                IconBadge(
                    icon = icon,
                    size = 52,
                    iconSize = 26,
                    containerColor = if (danger) cs.errorContainer else cs.primaryContainer,
                    contentColor = if (danger) cs.onErrorContainer else cs.onPrimaryContainer
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, cs.outlineVariant),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(cancelLabel) }
                    Button(
                        onClick = onConfirm,
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        colors = if (danger) ButtonDefaults.buttonColors(
                            containerColor = cs.error,
                            contentColor = cs.onError
                        ) else ButtonDefaults.buttonColors(),
                        modifier = Modifier.weight(1f)
                    ) { Text(confirmLabel, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.NotificationsActive
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
