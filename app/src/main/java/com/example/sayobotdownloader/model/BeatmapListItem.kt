package com.example.sayobotdownloader.model

import kotlinx.serialization.Serializable

@Serializable
data class BeatmapListResponse(
    val status: Int,
    val endid: Int = 0,
    val results: Int = 0,
    val data: List<BeatmapListItem>? = null
)

@Serializable
data class BeatmapListItem(
    val sid: Int,
    val title: String = "",
    val titleU: String = "",
    val artist: String = "",
    val artistU: String = "",
    val creator: String = "",
    val modes: Int = 0,
    val approved: Int = 0,
    val play_count: Long = 0,
    val favourite_count: Int = 0,
    val lastupdate: Long = 0
)