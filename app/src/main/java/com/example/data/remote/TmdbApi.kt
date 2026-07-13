package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Header

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,
        @Header("Authorization") authorization: String
    ): TmdbResponse

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Header("Authorization") authorization: String
    ): TmdbResponse
}

data class TmdbResponse(
    val results: List<TmdbResult>
)

data class TmdbResult(
    val id: Int,
    val title: String?,
    val name: String?,
    val poster_path: String?,
    val overview: String?
)
