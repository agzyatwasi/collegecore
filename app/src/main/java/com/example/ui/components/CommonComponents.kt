package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CampusCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  content: @Composable ColumnScope.() -> Unit,
) {
  if (onClick != null) {
    Surface(
      onClick = onClick,
      modifier = modifier,
      shape = MaterialTheme.shapes.medium,
      color = containerColor,
      border = BorderStroke(1.dp, borderColor),
      tonalElevation = 1.dp,
    ) {
      Column(modifier = Modifier.padding(16.dp), content = content)
    }
  } else {
    Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.medium,
      color = containerColor,
      border = BorderStroke(1.dp, borderColor),
      tonalElevation = 1.dp,
    ) {
      Column(modifier = Modifier.padding(16.dp), content = content)
    }
  }
}

@Composable
fun StatusPill(
  text: String,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = containerColor,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun SectionHeader(
  title: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )
    if (actionLabel != null && onAction != null) {
      TextButton(
        onClick = onAction,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
      ) {
        Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
      }
    }
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector = Icons.Outlined.Inbox,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier.size(64.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
    }
    Spacer(Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(16.dp))
      FilledTonalButton(
        onClick = onAction,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
      ) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
fun LoadingStateView(
  message: String = "Loading ITG Gopeshwar records...",
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator(strokeWidth = 3.dp)
    Spacer(Modifier.height(14.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun ErrorStateView(
  message: String,
  modifier: Modifier = Modifier,
) {
  CampusCard(
    modifier = modifier.fillMaxWidth(),
    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = "Error",
        tint = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.width(12.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SimpleDropdownField(
  label: String,
  selectedLabel: String,
  options: List<T>,
  optionLabel: (T) -> String,
  onSelect: (T) -> Unit,
  expanded: Boolean,
  onExpandedChange: (Boolean) -> Unit,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
) {
  ExposedDropdownMenuBox(
    expanded = expanded && enabled,
    onExpandedChange = { if (enabled) onExpandedChange(!expanded) },
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = selectedLabel,
      onValueChange = {},
      readOnly = true,
      enabled = enabled,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
    )
    ExposedDropdownMenu(
      expanded = expanded && enabled,
      onDismissRequest = { onExpandedChange(false) },
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(optionLabel(option)) },
          onClick = {
            onSelect(option)
            onExpandedChange(false)
          },
        )
      }
    }
  }
}

@Composable
fun DestructiveConfirmDialog(
  title: String,
  message: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(message) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
      ) {
        Text("Delete")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

fun formatTimestamp(ts: Timestamp?): String {
  if (ts == null) return "Just now"
  val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
  return sdf.format(ts.toDate())
}

fun openExternalUrl(context: Context, rawUrl: String) {
  val trimmed = rawUrl.trim()
  if (trimmed.isEmpty()) {
    Toast.makeText(context, "No document link attached.", Toast.LENGTH_SHORT).show()
    return
  }
  try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed))
    context.startActivity(intent)
  } catch (_: Exception) {
    Toast.makeText(context, "Unable to open link on this device.", Toast.LENGTH_SHORT).show()
  }
}
