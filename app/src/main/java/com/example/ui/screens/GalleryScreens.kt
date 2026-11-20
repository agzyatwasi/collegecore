package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.GalleryAlbum
import com.example.model.GalleryPhoto
import com.example.model.UiState
import com.example.ui.components.CampusCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.StatusPill

@Composable
fun GalleryScreen(
  albumsState: UiState<List<GalleryAlbum>>,
  isAdmin: Boolean,
  onOpenAlbum: (GalleryAlbum) -> Unit,
) {
  val categories = remember { listOf("All", "Cultural", "Technical", "Sports", "Campus", "Other") }
  var selectedCategory by remember { mutableStateOf("All") }
  val context = LocalContext.current

  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(categories) { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { selectedCategory = cat },
          label = { Text(cat) },
          modifier = Modifier.testTag("gallery_filter_$cat"),
        )
      }
    }

    when (albumsState) {
      is UiState.Loading -> LoadingStateView("Loading campus gallery...")
      is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorStateView(albumsState.message) }
      is UiState.Success -> {
        val visible =
          albumsState.data
            .filter { isAdmin || it.published }
            .filter {
              selectedCategory == "All" || it.eventCategory.equals(selectedCategory, ignoreCase = true)
            }

        if (visible.isEmpty()) {
          EmptyStateView(
            icon = Icons.Outlined.PhotoLibrary,
            title = "No Photo Albums Yet",
            subtitle = "College events, cultural fests, technical workshops, and sports albums will appear here.",
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            items(visible, key = { it.id }) { album ->
              CampusCard(
                onClick = { onOpenAlbum(album) },
                modifier = Modifier.fillMaxWidth().testTag("album_card_${album.id}"),
              ) {
                if (album.coverImageUrl.isNotBlank()) {
                  AsyncImage(
                    model =
                      ImageRequest.Builder(context)
                        .data(album.coverImageUrl)
                        .crossfade(true)
                        .size(640) // Thumbnail optimization to avoid downloading full-res unnecessarily
                        .build(),
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(170.dp)
                        .clip(MaterialTheme.shapes.small),
                  )
                  Spacer(Modifier.height(12.dp))
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  StatusPill(text = album.eventCategory)
                  StatusPill(
                    text = "${album.photoCount} Photo${if (album.photoCount == 1L) "" else "s"}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Spacer(Modifier.weight(1f))
                  Text(
                    text = album.eventDateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                  text = album.title,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                if (album.description.isNotBlank()) {
                  Spacer(Modifier.height(4.dp))
                  Text(
                    text = album.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
  album: GalleryAlbum?,
  photosState: UiState<List<GalleryPhoto>>,
  isAdmin: Boolean,
  onBack: () -> Unit,
) {
  var lightboxPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = album?.title ?: "Album",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("album_detail_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
    ) {
      if (album != null) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatusPill(text = album.eventCategory)
          StatusPill(
            text = album.eventDateText,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
        if (album.description.isNotBlank()) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = album.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.height(12.dp))
      }

      when (photosState) {
        is UiState.Loading -> LoadingStateView("Loading album photos...")
        is UiState.Error -> ErrorStateView(photosState.message)
        is UiState.Success -> {
          val photos = photosState.data.filter { isAdmin || it.published }
          if (photos.isEmpty()) {
            EmptyStateView(
              icon = Icons.Outlined.PhotoLibrary,
              title = "No Photos in Album",
              subtitle = "Admins can add event photos to this album from the Admin Dashboard.",
            )
          } else {
            LazyVerticalGrid(
              columns = GridCells.Adaptive(minSize = 150.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(bottom = 16.dp),
            ) {
              items(photos, key = { it.id }) { photo ->
                val thumbUrl = photo.thumbnailUrl.ifBlank { photo.imageUrl }
                CampusCard(
                  modifier =
                    Modifier.fillMaxWidth().clickable { lightboxPhoto = photo },
                ) {
                  AsyncImage(
                    model =
                      ImageRequest.Builder(context)
                        .data(thumbUrl)
                        .crossfade(true)
                        .size(480) // Thumbnail optimization
                        .build(),
                    contentDescription = photo.caption.ifBlank { "Event photo" },
                    contentScale = ContentScale.Crop,
                    modifier =
                      Modifier.fillMaxWidth()
                        .height(120.dp)
                        .clip(MaterialTheme.shapes.small),
                  )
                  if (photo.caption.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                      text = photo.caption,
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
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

  if (lightboxPhoto != null) {
    val photo = lightboxPhoto!!
    Dialog(onDismissRequest = { lightboxPhoto = null }) {
      Surface(
        shape = MaterialTheme.shapes.large,
        color = Color(0xFF0F172A),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            IconButton(onClick = { lightboxPhoto = null }) {
              Icon(
                Icons.Outlined.Close,
                contentDescription = "Close photo",
                tint = Color.White,
              )
            }
          }
          AsyncImage(
            model =
              ImageRequest.Builder(context)
                .data(photo.imageUrl)
                .crossfade(true)
                .size(1280)
                .build(),
            contentDescription = photo.caption,
            contentScale = ContentScale.Fit,
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = 220.dp, max = 420.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.Black),
          )
          if (photo.caption.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
              text = photo.caption,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
          }
        }
      }
    }
  }
}
