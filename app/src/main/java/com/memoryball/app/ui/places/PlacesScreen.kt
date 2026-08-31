package com.memoryball.app.ui.places

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.memoryball.app.data.model.Place
import com.memoryball.app.ui.common.ConfirmDialog
import com.memoryball.app.ui.common.EmptyState
import com.memoryball.app.ui.common.IconBadge
import com.memoryball.app.ui.common.MetaPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    onBack: () -> Unit,
    onNewPlace: () -> Unit,
    onEditPlace: (Long) -> Unit,
    vm: PlacesViewModel = viewModel()
) {
    val places by vm.places.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("常用地点", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewPlace,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                text = { Text("新建地点", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { padding ->
        if (places.isEmpty()) {
            EmptyState(
                title = "还没有常用地点",
                description = "在地图上拖动定位，保存「家」「公司」等\n地点，提醒就能知道你到没到",
                icon = Icons.Default.Place,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(places, key = { it.id }) { place ->
                    PlaceCard(
                        place = place,
                        onClick = { onEditPlace(place.id) },
                        onDelete = { vm.delete(place) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceCard(place: Place, onClick: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Default.Place,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaPill(
                        icon = Icons.Default.Radar,
                        text = "半径 ${place.radiusMeters.toInt()} m"
                    )
                }
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showConfirm) {
        ConfirmDialog(
            title = "删除地点？",
            text = "确定要删除「${place.name}」吗？使用它的提醒也会失去地点条件。",
            icon = Icons.Default.DeleteForever,
            confirmLabel = "删除",
            onConfirm = { showConfirm = false; onDelete() },
            onDismiss = { showConfirm = false }
        )
    }
}
