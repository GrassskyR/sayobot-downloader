package com.example.sayobotdownloader.ui.search

import com.example.sayobotdownloader.data.BeatmapRepositoryContract
import com.example.sayobotdownloader.model.BeatmapDetailResponse
import com.example.sayobotdownloader.model.BeatmapListItem
import com.example.sayobotdownloader.model.BeatmapListResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsHotBeatmaps() = runTest {
        val repository = FakeBeatmapRepository(
            hotResponses = listOf(successResponse("Hot Beatmap", endId = 25))
        )

        val viewModel = SearchViewModel(repository)

        assertEquals(SearchMode.HOT, viewModel.searchMode.value)
        assertSuccessTitles(viewModel.uiState.value, "Hot Beatmap")
    }

    @Test
    fun init_whenRepositoryFails_emitsError() = runTest {
        val repository = FakeBeatmapRepository(failHot = true)

        val viewModel = SearchViewModel(repository)

        assertTrue(viewModel.uiState.value is SearchUiState.Error)
    }

    @Test
    fun onSearch_loadsSearchResults() = runTest {
        val repository = FakeBeatmapRepository(
            hotResponses = listOf(successResponse("Initial")),
            searchResponses = listOf(successResponse("Search Result"))
        )
        val viewModel = SearchViewModel(repository)

        viewModel.onSearchQueryChange("quaver")
        viewModel.onSearch()

        assertEquals(SearchMode.SEARCH, viewModel.searchMode.value)
        assertSuccessTitles(viewModel.uiState.value, "Search Result")
    }

    @Test
    fun loadMore_appendsItems() = runTest {
        val repository = FakeBeatmapRepository(
            hotResponses = listOf(
                successResponse("First Page", endId = 25, itemCount = 25),
                successResponse("Second Page", endId = 50)
            )
        )
        val viewModel = SearchViewModel(repository)

        viewModel.loadMore()

        val success = viewModel.uiState.value as SearchUiState.Success
        assertEquals(26, success.items.size)
        assertEquals("First Page", success.items.first().title)
        assertEquals("Second Page", success.items.last().title)
    }

    private fun assertSuccessTitles(state: SearchUiState, vararg titles: String) {
        val success = state as SearchUiState.Success
        assertEquals(titles.toList(), success.items.map { it.title })
    }
}

private class FakeBeatmapRepository(
    private val hotResponses: List<BeatmapListResponse> = listOf(successResponse("Hot Beatmap")),
    private val searchResponses: List<BeatmapListResponse> = listOf(successResponse("Search Result")),
    private val failHot: Boolean = false
) : BeatmapRepositoryContract {
    private var hotCallCount = 0
    private var searchCallCount = 0

    override suspend fun search(keyword: String, limit: Int, offset: Int): BeatmapListResponse =
        searchResponses.getOrElse(searchCallCount++) { searchResponses.last() }

    override suspend fun getNew(limit: Int, offset: Int): BeatmapListResponse =
        successResponse("New Beatmap")

    override suspend fun getHot(limit: Int, offset: Int): BeatmapListResponse {
        if (failHot) error("Network error")
        return hotResponses.getOrElse(hotCallCount++) { hotResponses.last() }
    }

    override suspend fun getDetail(sid: Int): BeatmapDetailResponse =
        BeatmapDetailResponse(status = 1)
}

private fun successResponse(title: String, endId: Int = 0, itemCount: Int = 1) = BeatmapListResponse(
    status = 0,
    endid = endId,
    data = List(itemCount) { index ->
        BeatmapListItem(
            sid = title.hashCode() + index,
            title = if (index == 0) title else "$title $index",
            artist = "Artist",
            creator = "Creator"
        )
    }
)
