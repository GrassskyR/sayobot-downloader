package com.example.sayobotdownloader.network

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SayobotApiTest {
    @Test
    fun getHotBeatmaps_sendsNumericLimitAndOffset() = runTest {
        var requestBody = ""
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestBody = chain.request().bodyToString()
                okResponse(chain)
            }
            .build()
        val api = SayobotApi(client)

        api.getHotBeatmaps(limit = 25, offset = 50)

        assertTrue(requestBody.contains("\"limit\":25"))
        assertTrue(requestBody.contains("\"offset\":50"))
        assertFalse(requestBody.contains("\"limit\":\"25\""))
        assertFalse(requestBody.contains("\"offset\":\"50\""))
    }

    private fun okhttp3.Request.bodyToString(): String {
        val buffer = Buffer()
        body?.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun okResponse(chain: Interceptor.Chain): Response =
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"status":0,"endid":0,"data":[]}""".toResponseBody("application/json".toMediaType()))
            .build()
}
