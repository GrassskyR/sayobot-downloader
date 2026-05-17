package com.example.sayobotdownloader.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayobotdownloader.data.BeatmapRepository
import com.example.sayobotdownloader.data.BeatmapRepositoryContract
import com.example.sayobotdownloader.model.BeatmapListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchMode { NEW, HOT, SEARCH }

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

    private var currentEndId = 0
    private val allItems = mutableListOf<BeatmapListItem>()

    init {
        loadHot()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSearch() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return
        _searchMode.value = SearchMode.SEARCH
        allItems.clear()
        currentEndId = 0
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.search(query, offset = 0)
                if (response.status == 0 && response.data != null) {
                    allItems.addAll(response.data)
                    currentEndId = response.endid
                    _uiState.value = SearchUiState.Success(
                        items = allItems.toList(),
                        canLoadMore = response.data.size >= 25
                    )
                } else {
                    _uiState.value = SearchUiState.Error("No results found")
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadHot() {
        _searchMode.value = SearchMode.HOT
        _searchQuery.value = ""
        allItems.clear()
        currentEndId = 0
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getHot(offset = 0)
                if (response.status == 0 && response.data != null) {
                    allItems.addAll(response.data)
                    currentEndId = response.endid
                    _uiState.value = SearchUiState.Success(
                        items = allItems.toList(),
                        canLoadMore = response.data.size >= 25
                    )
                } else {
                    _uiState.value = SearchUiState.Error("Failed to load")
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadNew() {
        _searchMode.value = SearchMode.NEW
        _searchQuery.value = ""
        allItems.clear()
        currentEndId = 0
        _uiState.value = SearchUiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getNew(offset = 0)
                if (response.status == 0 && response.data != null) {
                    allItems.addAll(response.data)
                    currentEndId = response.endid
                    _uiState.value = SearchUiState.Success(
                        items = allItems.toList(),
                        canLoadMore = response.data.size >= 25
                    )
                } else {
                    _uiState.value = SearchUiState.Error("Failed to load")
                }
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
                    SearchMode.SEARCH -> repository.search(query, offset = currentEndId)
                    SearchMode.HOT -> repository.getHot(offset = currentEndId)
                    SearchMode.NEW -> repository.getNew(offset = currentEndId)
                }
                if (response.status == 0 && response.data != null) {
                    allItems.addAll(response.data)
                    currentEndId = response.endid
                    _uiState.value = SearchUiState.Success(
                        items = allItems.toList(),
                        canLoadMore = response.data.size >= 25
                    )
                } else {
                    _uiState.value = SearchUiState.Success(
                        items = allItems.toList(),
                        canLoadMore = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Success(
                    items = allItems.toList(),
                    canLoadMore = false
                )
            }
        }
    }
}
