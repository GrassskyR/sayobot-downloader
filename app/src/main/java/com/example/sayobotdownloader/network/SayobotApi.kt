package com.example.sayobotdownloader.network

import com.example.sayobotdownloader.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SayobotApi(
    private val client: OkHttpClient = OkHttpClient.Builder().build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun searchBeatmaps(keyword: String, limit: Int = 25, offset: Int = 0): BeatmapListResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(mapOf(
                "cmd" to "beatmaplist",
                "limit" to limit.toString(),
                "offset" to offset.toString(),
                "type" to "search",
                "keyword" to keyword
            ))
            val request = Request.Builder()
                .url("https://api.sayobot.cn/?post")
                .post(body.toRequestBody("text/plain".toMediaType()))
                .build()
            parseResponse(client.newCall(request).execute())
        }

    suspend fun getNewBeatmaps(limit: Int = 25, offset: Int = 0): BeatmapListResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(mapOf(
                "cmd" to "beatmaplist",
                "limit" to limit.toString(),
                "offset" to offset.toString(),
                "type" to "new"
            ))
            val request = Request.Builder()
                .url("https://api.sayobot.cn/?post")
                .post(body.toRequestBody("text/plain".toMediaType()))
                .build()
            parseResponse(client.newCall(request).execute())
        }

    suspend fun getHotBeatmaps(limit: Int = 25, offset: Int = 0): BeatmapListResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(mapOf(
                "cmd" to "beatmaplist",
                "limit" to limit.toString(),
                "offset" to offset.toString(),
                "type" to "hot"
            ))
            val request = Request.Builder()
                .url("https://api.sayobot.cn/?post")
                .post(body.toRequestBody("text/plain".toMediaType()))
                .build()
            parseResponse(client.newCall(request).execute())
        }

    suspend fun getBeatmapDetail(sid: Int): BeatmapDetailResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.sayobot.cn/v2/beatmapinfo?0=$sid")
                .build()
            parseResponse(client.newCall(request).execute())
        }

    private inline fun <reified T> parseResponse(response: okhttp3.Response): T {
        response.use {
            val body = it.body?.string() ?: throw Exception("Empty response")
            return json.decodeFromString(body)
        }
    }
}