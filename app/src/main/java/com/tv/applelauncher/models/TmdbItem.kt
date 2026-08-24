package com.tv.applelauncher.api

import com.tv.applelauncher.models.TmdbResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbService {

    @GET("trending/all/day")
    suspend fun trending(@Query("api_key") apiKey: String): TmdbResponse

    @GET("movie/top_rated")
    suspend fun topRated(@Query("api_key") apiKey: String): TmdbResponse

    @GET("search/multi")
    suspend fun search(@Query("api_key") apiKey: String,
                       @Query("query") query: String): TmdbResponse

    companion object {
        // ⬇️ PASTE YOUR FREE TMDB KEY HERE ⬇️
        const val API_KEY = "da31041bab820d6e94a0c0a098a1db46"
        const val IMG_BASE = "https://image.tmdb.org/t/p/"

        fun create(): TmdbService =
            Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TmdbService::class.java)
    }
}