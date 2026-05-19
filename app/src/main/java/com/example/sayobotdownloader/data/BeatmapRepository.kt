package com.example.sayobotdownloader.data

import com.example.sayobotdownloader.model.*
import com.example.sayobotdownloader.network.SayobotApi

interface BeatmapRepositoryContract {
    suspend fun search(
        keyword: String,
        limit: Int = 25,
        offset: Int = 0,
        mode: Int? = null,
        classFilter: Int? = null
    ): BeatmapListResponse
    suspend fun getNew(limit: Int = 25, offset: Int = 0): BeatmapListResponse
    suspend fun getHot(limit: Int = 25, offset: Int = 0): BeatmapListResponse
    suspend fun getDetail(sid: Int): BeatmapDetailResponse
}

class BeatmapRepository(private val api: SayobotApi = SayobotApi()) : BeatmapRepositoryContract {

    override suspend fun search(
        keyword: String,
        limit: Int,
        offset: Int,
        mode: Int?,
        classFilter: Int?
    ): BeatmapListResponse {
        val sid = keyword.trim().toIntOrNull()
        if (sid != null) {
            val detail = api.getBeatmapDetail(sid)
            return BeatmapListResponse(
                status = detail.status,
                endid = 0,
                data = detail.data?.let { listOf(detailToListItem(it)) }
            )
        }
        return api.searchBeatmaps(keyword, limit, offset, mode, classFilter)
    }

    override suspend fun getNew(limit: Int, offset: Int) = api.getNewBeatmaps(limit, offset)
    override suspend fun getHot(limit: Int, offset: Int) = api.getHotBeatmaps(limit, offset)
    override suspend fun getDetail(sid: Int) = api.getBeatmapDetail(sid)

    private fun detailToListItem(d: BeatmapDetail) = BeatmapListItem(
        sid = d.sid, title = d.title, titleU = d.titleU,
        artist = d.artist, artistU = d.artistU, creator = d.creator,
        modes = d.bid_data.firstOrNull()?.mode?.let { 1 shl it } ?: 1,
        approved = d.approved, favourite_count = d.favourite_count, lastupdate = d.last_update
    )
}
