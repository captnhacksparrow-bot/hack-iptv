package com.example.data

import com.example.data.remote.TmdbApi
import com.example.data.remote.TmdbResult
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class TmdbRepository(private val apiToken: String) {
    private val api: TmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    suspend fun searchMovie(query: String): List<TmdbResult> {
        return try {
            api.searchMovie(query, "Bearer $apiToken").results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchTv(query: String): List<TmdbResult> {
        return try {
            api.searchTv(query, "Bearer $apiToken").results
        } catch (e: Exception) {
            emptyList()
        }
    }
}
