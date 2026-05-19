package com.example.sayobotdownloader.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayobotdownloader.data.BeatmapRepository
import com.example.sayobotdownloader.data.BeatmapRepositoryContract
import com.example.sayobotdownloader.model.BeatmapListItem
import com.example.sayobotdownloader.model.SearchFilterState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchMode { NEW, HOT, SEARCH }

enum class SearchInteractionState { BROWSING, INPUT, LOADING, RESULTS }

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val items: List<BeatmapListItem>,
        val isLoadingMore: Boolean = false,
        val canLoadMore: Boolean = true
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val repository: BeatmapRepositoryContract = BeatmapRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.HOT)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private val _searchInteractionState = MutableStateFlow(SearchInteractionState.BROWSING)
    val searchInteractionState: StateFlow<SearchInteractionState> = _searchInteractionState.asStateFlow()

    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    private val _stagedFilterState = MutableStateFlow(SearchFilterState())
    val stagedFilterState: StateFlow<SearchFilterState> = _stagedFilterState.asStateFlow()

    private var nextOffset = 0
    private var lastBrowsingMode = SearchMode.HOT
    private var loadJob: Job? = null
    private val allItems = mutableListOf<BeatmapListItem>()
    private val loadedSids = mutableSetOf<Int>()

    init {
        loadHot()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (_searchInteractionState.value != SearchInteractionState.LOADING) {
            _searchInteractionState.value = SearchInteractionState.INPUT
        }
    }

    fun enterSearchMode() {
        if (_searchInteractionState.value == SearchInteractionState.BROWSING) {
            _searchInteractionState.value = SearchInteractionState.INPUT
        }
    }

    fun onSearch() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return
        if (_searchMode.value != SearchMode.SEARCH) {
            lastBrowsingMode = _searchMode.value
        }
        _searchMode.value = SearchMode.SEARCH
        _searchInteractionState.value = SearchInteractionState.LOADING
        resetPaging()
        _uiState.value = SearchUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val response = repository.search(
                    query,
                    limit = PAGE_SIZE,
                    offset = 0,
                    mode = _filterState.value.modeBitmask,
                    classFilter = _filterState.value.statusBitmask
                )
                if (response.status == 0 && response.data != null) {
                    val addedCount = appendNewItems(response.data)
                    nextOffset += response.data.size
                    emitSuccess(
                        canLoadMore = response.data.size >= PAGE_SIZE && addedCount > 0
                    )
                    _searchInteractionState.value = SearchInteractionState.RESULTS
                } else {
                    _uiState.value = SearchUiState.Error("No results found")
                    _searchInteractionState.value = SearchInteractionState.RESULTS
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
                _searchInteractionState.value = SearchInteractionState.RESULTS
            }
        }
    }

    fun loadHot() {
        _searchMode.value = SearchMode.HOT
        lastBrowsingMode = SearchMode.HOT
        _searchInteractionState.value = SearchInteractionState.BROWSING
        _searchQuery.value = ""
        resetPaging()
        _uiState.value = SearchUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val response = repository.getHot(limit = PAGE_SIZE, offset = 0)
                if (response.status == 0 && response.data != null) {
                    val addedCount = appendNewItems(response.data)
                    nextOffset += response.data.size
                    emitSuccess(
                        canLoadMore = response.data.size >= PAGE_SIZE && addedCount > 0
                    )
                } else {
                    _uiState.value = SearchUiState.Error("Failed to load")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadNew() {
        _searchMode.value = SearchMode.NEW
        lastBrowsingMode = SearchMode.NEW
        _searchInteractionState.value = SearchInteractionState.BROWSING
        _searchQuery.value = ""
        resetPaging()
        _uiState.value = SearchUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val response = repository.getNew(limit = PAGE_SIZE, offset = 0)
                if (response.status == 0 && response.data != null) {
                    val addedCount = appendNewItems(response.data)
                    nextOffset += response.data.size
                    emitSuccess(
                        canLoadMore = response.data.size >= PAGE_SIZE && addedCount > 0
                    )
                } else {
                    _uiState.value = SearchUiState.Error("Failed to load")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is SearchUiState.Success || state.isLoadingMore || !state.canLoadMore) return

        _uiState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val query = _searchQuery.value.trim()
                val response = when (_searchMode.value) {
                    SearchMode.SEARCH -> {
                        if (query.isEmpty()) return@launch
                        repository.search(
                            query,
                            limit = PAGE_SIZE,
                            offset = nextOffset,
                            mode = _filterState.value.modeBitmask,
                            classFilter = _filterState.value.statusBitmask
                        )
                    }
                    SearchMode.HOT -> repository.getHot(limit = PAGE_SIZE, offset = nextOffset)
                    SearchMode.NEW -> repository.getNew(limit = PAGE_SIZE, offset = nextOffset)
                }
                if (response.status == 0 && response.data != null) {
                    val addedCount = appendNewItems(response.data)
                    nextOffset += response.data.size
                    emitSuccess(
                        canLoadMore = response.data.size >= PAGE_SIZE && addedCount > 0
                    )
                } else {
                    emitSuccess(canLoadMore = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emitSuccess(canLoadMore = false)
            }
        }
    }

    fun exitSearchMode() {
        loadJob?.cancel()
        when (lastBrowsingMode) {
            SearchMode.NEW -> loadNew()
            SearchMode.HOT, SearchMode.SEARCH -> loadHot()
        }
    }

    fun openFilterSheet() {
        _stagedFilterState.value = _filterState.value
    }

    fun updateStagedModes(modes: Set<String>) {
        _stagedFilterState.value = _stagedFilterState.value.copy(selectedModes = modes)
    }

    fun updateStagedStatuses(statuses: Set<String>) {
        _stagedFilterState.value = _stagedFilterState.value.copy(selectedStatuses = statuses)
    }

    fun applyFilters() {
        _filterState.value = _stagedFilterState.value
        if (_searchMode.value == SearchMode.SEARCH && _searchQuery.value.isNotBlank()) {
            onSearch()
        } else {
            emitFilteredItems()
        }
    }

    fun resetStagedFilters() {
        _stagedFilterState.value = SearchFilterState()
    }

    fun dismissFilters() {
        // Staged changes discarded; current filter unchanged
    }

    private fun emitFilteredItems() {
        val state = _uiState.value
        if (state is SearchUiState.Success) {
            emitSuccess(canLoadMore = state.canLoadMore, isLoadingMore = state.isLoadingMore)
        }
    }

    private fun emitSuccess(
        canLoadMore: Boolean = true,
        isLoadingMore: Boolean = false
    ) {
        val filtered = filterItems(allItems.toList(), _filterState.value)
        _uiState.value = SearchUiState.Success(
            items = filtered,
            isLoadingMore = isLoadingMore,
            canLoadMore = canLoadMore
        )
    }

    private fun filterItems(
        items: List<BeatmapListItem>,
        filter: SearchFilterState
    ): List<BeatmapListItem> {
        return items.filter { item ->
            (filter.selectedModes.isEmpty() || filter.selectedModes.any { mode ->
                when (mode) {
                    SearchFilterState.MODE_STD -> item.modes and 1 != 0
                    SearchFilterState.MODE_TAIKO -> item.modes and 2 != 0
                    SearchFilterState.MODE_CTB -> item.modes and 4 != 0
                    SearchFilterState.MODE_MANIA -> item.modes and 8 != 0
                    else -> true
                }
            }) && (filter.selectedStatuses.isEmpty() || filter.selectedStatuses.any { status ->
                when (status) {
                    SearchFilterState.STATUS_RANKED_APPROVED -> item.approved == 1 || item.approved == 2
                    SearchFilterState.STATUS_QUALIFIED -> item.approved == 3
                    SearchFilterState.STATUS_LOVED -> item.approved == 4
                    SearchFilterState.STATUS_PENDING_WIP -> item.approved == 0 || item.approved == -1
                    SearchFilterState.STATUS_GRAVEYARD -> item.approved == -2
                    else -> true
                }
            })
        }
    }

    private fun resetPaging() {
        allItems.clear()
        loadedSids.clear()
        nextOffset = 0
    }

    private fun appendNewItems(items: List<BeatmapListItem>): Int {
        var addedCount = 0
        items.forEach { item ->
            if (loadedSids.add(item.sid)) {
                allItems.add(item)
                addedCount++
            }
        }
        return addedCount
    }

    private companion object {
        const val PAGE_SIZE = 25
    }
}
