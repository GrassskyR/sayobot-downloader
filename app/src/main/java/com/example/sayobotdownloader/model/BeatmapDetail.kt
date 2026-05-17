package com.example.sayobotdownloader.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BeatmapDetailResponse(
    val status: Int,
    val data: BeatmapDetail? = null
)

@Serializable
data class BeatmapDetail(
    val sid: Int,
    val title: String = "",
    val titleU: String = "",
    val artist: String = "",
    val artistU: String = "",
    val creator: String = "",
    val approved: Int = 0,
    val bpm: Double = 0.0,
    val tags: String = "",
    val source: String = "",
    val favourite_count: Int = 0,
    val last_update: Long = 0,
    val preview: Int = 0,
    val video: Int = 0,
    val storyboard: Int = 0,
    val bids_amount: Int = 0,
    val bid_data: List<BeatmapDifficulty> = emptyList()
)

@Serializable
data class BeatmapDifficulty(
    val bid: Int,
    val version: String = "",
    val mode: Int = 0,
    val star: Double = 0.0,
    @SerialName("AR") val ar: Double = 0.0,
    @SerialName("CS") val cs: Double = 0.0,
    @SerialName("OD") val od: Double = 0.0,
    @SerialName("HP") val hp: Double = 0.0,
    val circles: Int = 0,
    val sliders: Int = 0,
    val spinners: Int = 0,
    val length: Int = 0,
    val passcount: Int = 0,
    val playcount: Int = 0,
    val audio: String = "",
    val bg: String = ""
)