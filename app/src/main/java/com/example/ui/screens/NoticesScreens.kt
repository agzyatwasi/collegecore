package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Notice
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.ui.components.CampusCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.StatusPill
import com.example.ui.components.formatTimestamp
import com.example.ui.components.openExternalUrl

@Composable
fun NoticesScreen(
  noticesState: UiState<List<Notice>>,
  profile: StudentProfile?,
  isAdmin: Boolean,
  readNoticeIds: Set<String>,
  onOpenNotice: (Notice) -> Unit,
) {
  val categories = remember { listOf("All", "Unread", "General", "Academic", "Exam", "Event", "Urgent") }
  var selectedFilter by remember { mutableStateOf("All") }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(categories) { cat ->
        FilterChip(
          selected = selectedFilter == cat,
          onClick = { selectedFilter = cat },
          label = { Text(cat) },
          modifier = Modifier.testTag("notice_filter_$cat"),
        )
      }
    }

    when (noticesState) {
      is UiState.Loading -> LoadingStateView("Loading notices...")
      is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorStateView(noticesState.message) }
      is UiState.Success -> {
        val visible =
          noticesState.data
            .filter { if (isAdmin) true else it.published && it.matchesStudent(profile) }
            .filter { notice ->
              when (selectedFilter) {
                "All" -> true
                "Unread" -> notice.id !in readNoticeIds
                else -> notice.category.equals(selectedFilter, ignoreCase = true)
              }
            }

        if (visible.isEmpty()) {
          EmptyStateView(
            icon = Icons.Outlined.Campaign,
            title = "No Notices Found",
            subtitle = "No notices match the selected filter right now.",
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(visible, key = { it.id }) { notice ->
              val isUnread = notice.id !in readNoticeIds
              CampusCard(
                onClick = { onOpenNotice(notice) },
                modifier = Modifier.fillMaxWidth().testTag("notice_card_${notice.id}"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (isUnread) {
                    Box(
                      modifier =
                        Modifier.size(9.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.error)
                    )
                    Spacer(Modifier.width(8.dp))
                  }
                  StatusPill(
                    text = notice.category,
                    containerColor =
                      if (notice.important) MaterialTheme.colorScheme.errorContainer
                      else MaterialTheme.colorScheme.primaryContainer,
                    contentColor =
                      if (notice.important) MaterialTheme.colorScheme.onErrorContainer
                      else MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  if (!notice.published) {
                    Spacer(Modifier.width(6.dp))
                    StatusPill(
                      text = "DRAFT",
                      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                      contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                  }
                  Spacer(Modifier.weight(1f))
                  Text(
                    text = formatTimestamp(notice.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                  text = notice.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                  text = notice.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 3,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "By ${notice.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (notice.attachmentUrl.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Attachment",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(Modifier.width(4.dp))
                      Text(
                        text = notice.attachmentName.ifBlank { "Attachment" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDetailScreen(
  notice: Notice?,
  isRead: Boolean,
  onMarkRead: (String) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current

  LaunchedEffect(notice?.id) {
    if (notice != null && !isRead) {
      onMarkRead(notice.id)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Notice Details") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("notice_detail_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { innerPadding ->
    if (notice == null) {
      Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        EmptyStateView(
          title = "Notice Not Available",
          subtitle = "This notice may have been removed or unpublished.",
          actionLabel = "Go Back",
          onAction = onBack,
        )
      }
    } else {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
      ) {
        CampusCard(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            StatusPill(
              text = notice.category,
              containerColor =
                if (notice.important) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
              contentColor =
                if (notice.important) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (notice.important) {
              StatusPill(
                text = "IMPORTANT",
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
              )
            }
            Spacer(Modifier.weight(1f))
            Icon(
              imageVector = Icons.Outlined.MarkEmailRead,
              contentDescription = "Read",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
          }
          Spacer(Modifier.height(12.dp))
          Text(
            text = notice.title,
            style = MaterialTheme.typography.headlineSmall,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Published ${formatTimestamp(notice.createdAt)} • By ${notice.authorName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(14.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Spacer(Modifier.height(14.dp))
          Text(
            text = notice.message,
            style = MaterialTheme.typography.bodyLarge,
          )

          if (notice.attachmentUrl.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Button(
              onClick = { openExternalUrl(context, notice.attachmentUrl) },
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            ) {
              Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(notice.attachmentName.ifBlank { "Open Attachment" })
            }
          }
        }
      }
    }
  }
}
