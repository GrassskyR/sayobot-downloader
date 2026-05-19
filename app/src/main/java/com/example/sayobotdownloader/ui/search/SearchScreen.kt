package com.example.sayobotdownloader.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.example.sayobotdownloader.DetailRoute
import com.example.sayobotdownloader.model.BeatmapListItem
import com.example.sayobotdownloader.model.SearchFilterState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val searchInteractionState by viewModel.searchInteractionState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val stagedFilterState by viewModel.stagedFilterState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val isSearchActive = searchInteractionState != SearchInteractionState.BROWSING
    val showBrowsingTabs = searchInteractionState == SearchInteractionState.BROWSING ||
        searchInteractionState == SearchInteractionState.INPUT
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300
        }
    }

    LaunchedEffect(listState, uiState, searchInteractionState) {
        val state = uiState as? SearchUiState.Success ?: return@LaunchedEffect
        if (!state.canLoadMore || state.isLoadingMore || searchInteractionState == SearchInteractionState.INPUT) {
            return@LaunchedEffect
        }
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            totalItems > 0 && lastVisible >= totalItems - LOAD_MORE_THRESHOLD
        }
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    viewModel.loadMore()
                }
            }
    }

    fun submitSearch() {
        if (searchQuery.isBlank()) return
        focusManager.clearFocus()
        keyboardController?.hide()
        viewModel.onSearch()
    }

    fun exitSearch() {
        focusManager.clearFocus()
        keyboardController?.hide()
        viewModel.exitSearchMode()
    }

    BackHandler(enabled = isSearchActive, onBack = ::exitSearch)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Sayobot Downloader") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            TextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            viewModel.enterSearchMode()
                        }
                    },
                placeholder = { Text("Search beatmaps...") },
                leadingIcon = {
                    IconButton(
                        onClick = ::submitSearch,
                        enabled = searchQuery.isNotBlank()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = ::exitSearch) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                        BadgedBox(
                            badge = {
                                if (filterState.isFilterActive) {
                                    Badge()
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.openFilterSheet()
                                    showFilterSheet = true
                                }
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Filter")
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() })
            )

            if (showBrowsingTabs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = searchMode == SearchMode.NEW,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.loadNew()
                        },
                        label = { Text("Latest") }
                    )
                    FilterChip(
                        selected = searchMode == SearchMode.HOT,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.loadHot()
                        },
                        label = { Text("Hot") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is SearchUiState.Idle, is SearchUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is SearchUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        when (searchMode) {
                                            SearchMode.SEARCH -> submitSearch()
                                            SearchMode.NEW -> viewModel.loadNew()
                                            SearchMode.HOT -> viewModel.loadHot()
                                        }
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is SearchUiState.Success -> {
                        if (state.items.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchMode == SearchMode.SEARCH) {
                                        "No beatmaps found. Try another keyword."
                                    } else {
                                        "No beatmaps"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.items, key = { it.sid }) { item ->
                                    BeatmapCard(
                                        item = item,
                                        onClick = {
                                            onItemClick(DetailRoute(sid = item.sid, title = item.title))
                                        }
                                    )
                                }
                                if (state.isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (searchInteractionState == SearchInteractionState.INPUT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "Enter a keyword to search beatmaps"
                            } else {
                                "Press the keyboard search key or search icon"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showScrollToTop && searchInteractionState != SearchInteractionState.INPUT) {
            FloatingActionButton(
                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Back to top")
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            stagedFilterState = stagedFilterState,
            onModeSelected = viewModel::updateStagedMode,
            onStatusSelected = viewModel::updateStagedStatus,
            onReset = viewModel::resetStagedFilters,
            onApply = {
                viewModel.applyFilters()
                showFilterSheet = false
            },
            onDismiss = {
                viewModel.dismissFilters()
                showFilterSheet = false
            },
            sheetState = sheetState
        )
    }
}

@Composable
private fun BeatmapCard(
    item: BeatmapListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://a.sayobot.cn/beatmaps/${item.sid}/covers/cover.webp?0",
                contentDescription = item.title,
                modifier = Modifier
                    .width(100.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (item.artist.isNotEmpty()) append(item.artist)
                        if (item.artist.isNotEmpty() && item.creator.isNotEmpty()) append(" - ")
                        if (item.creator.isNotEmpty()) append(item.creator)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = modeText(item.modes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatCount(item.favourite_count),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatCount(item.play_count.toInt()),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    stagedFilterState: SearchFilterState,
    onModeSelected: (String) -> Unit,
    onStatusSelected: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: androidx.compose.material3.SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "高级选项",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Text(
                text = "模式",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchFilterState.MODE_OPTIONS.forEach { mode ->
                    FilterChip(
                        selected = stagedFilterState.selectedMode == mode,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "状态",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchFilterState.STATUS_OPTIONS.forEach { status ->
                    FilterChip(
                        selected = stagedFilterState.selectedStatus == status,
                        onClick = { onStatusSelected(status) },
                        label = { Text(status) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReset) {
                    Text("重置")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApply) {
                    Text("确认")
                }
            }
        }
    }
}

private fun modeText(modes: Int): String {
    val parts = mutableListOf<String>()
    if (modes and 1 != 0) parts.add("Std")
    if (modes and 2 != 0) parts.add("Taiko")
    if (modes and 4 != 0) parts.add("CTB")
    if (modes and 8 != 0) parts.add("Mania")
    return parts.joinToString(" / ").ifEmpty { "Unknown" }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private const val LOAD_MORE_THRESHOLD = 5
