package com.example.sayobotdownloader.ui.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.sayobotdownloader.data.BeatmapRepositoryContract
import com.example.sayobotdownloader.model.BeatmapDetailResponse
import com.example.sayobotdownloader.model.BeatmapListItem
import com.example.sayobotdownloader.model.BeatmapListResponse
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun hotBeatmap_isDisplayed() {
        val viewModel = SearchViewModel(FakeBeatmapRepository())

        composeTestRule.setContent {
            SearchScreen(
                onItemClick = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("Instrumented Beatmap").assertIsDisplayed()
    }
}

private class FakeBeatmapRepository : BeatmapRepositoryContract {
    override suspend fun search(keyword: String, limit: Int, offset: Int): BeatmapListResponse =
        response()

    override suspend fun getNew(limit: Int, offset: Int): BeatmapListResponse =
        response()

    override suspend fun getHot(limit: Int, offset: Int): BeatmapListResponse =
        response()

    override suspend fun getDetail(sid: Int): BeatmapDetailResponse =
        BeatmapDetailResponse(status = 1)

    private fun response() = BeatmapListResponse(
        status = 0,
        data = listOf(
            BeatmapListItem(
                sid = 1,
                title = "Instrumented Beatmap",
                artist = "Artist",
                creator = "Creator",
                modes = 1
            )
        )
    )
}
