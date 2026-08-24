package com.tv.applelauncher.streaming

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ---------- Models ----------
data class JfAuthResponse(val User: JfUser?, val AccessToken: String?)
data class JfUser(val Id: String, val Name: String)
data class JfLoginRequest(val Username: String, val Pw: String)
data class JfItemsResponse(val Items: List<JfItem>, val TotalRecordCount: Int)
data class JfItem(
    val Id: String,
    val Name: String,
    val Type: String?,
    val SeriesName: String?,
    val Overview: String?,
    val ProductionYear: Int?,
    val CommunityRating: Double?,
    val OfficialRating: String?,
    val RunTimeTicks: Long?,
    val UserData: JfUserData?,
    val ImageTags: Map<String, String>?
)
data class JfUserData(
    val PlayedPercentage: Double?,
    val PlaybackPositionTicks: Long?,
    val Played: Boolean?
)

interface JellyfinApi {

    @POST("Users/AuthenticateByName")
    suspend fun login(
        @Header("X-Emby-Authorization") authHeader: String,
        @Body body: JfLoginRequest
    ): JfAuthResponse

    @GET("Users/{userId}/Views")
    suspend fun getLibraries(@Path("userId") userId: String): JfItemsResponse

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String?,
        @Query("IncludeItemTypes") types: String = "Movie,Series",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Limit") limit: Int = 30,
        @Query("SortBy") sortBy: String = "DateCreated",
        @Query("SortOrder") sortOrder: String = "Descending",
        @Query("Fields") fields: String = "Overview,ProductionYear,CommunityRating,OfficialRating"
    ): JfItemsResponse

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String
    ): JfItemsResponse

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResume(
        @Path("userId") userId: String,
        @Query("Limit") limit: Int = 15,
        @Query("Fields") fields: String = "Overview,ProductionYear"
    ): JfItemsResponse

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatest(@Path("userId") userId: String, @Query("Limit") limit: Int = 15): List<JfItem>

    @POST("Sessions/Playing/Progress")
    suspend fun reportProgress(@Query("itemId") itemId: String,
                               @Query("positionTicks") positionTicks: Long)

    companion object {
        const val CLIENT_NAME = "AppleTV Launcher"

        fun create(baseUrl: String, token: String? = null): JellyfinApi {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().apply {
                        if (!token.isNullOrBlank()) header("X-Emby-Token", token)
                        header("Accept", "application/json")
                    }.build())
                }
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl.trimEnd('/') + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(JellyfinApi::class.java)
        }

        fun streamUrl(serverUrl: String, itemId: String, token: String): String =
            "serverUrl/Videos/serverUrl/Videos/serverUrl/Videos/itemId/stream?static=true&mediaSourceId=itemId&api_key=token"

        fun hlsUrl(serverUrl: String, itemId: String, token: String): String =
            "serverUrl/videos/serverUrl/videos/serverUrl/videos/itemId/hls1/main/-1?VideoCodec=h264&AudioCodec=aac" +
                    "&api_key=$token&maxStreamingBitrate=8000000"

        fun imageUrl(serverUrl: String, item: JfItem, type: String = "Primary", w: Int = 500): String {
            val tag = item.ImageTags?.get(type) ?: return ""
            return "serverUrl/Items/serverUrl/Items/serverUrl/Items/{item.Id}/Images/type?fillWidth=type?fillWidth=type?fillWidth=w&quality=90&tag=$tag"
        }
    }
}