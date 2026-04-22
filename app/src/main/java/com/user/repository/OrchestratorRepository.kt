package com.user.repository

import com.google.gson.Gson
import com.user.data.CachedResponse
import com.user.data.CachedResponseDao

class OrchestratorRepository(private val cachedResponseDao: CachedResponseDao) {

    private val gson = Gson()

    suspend fun <T> fetchWithCache(
        key: String,
        fetch: suspend () -> Result<T>,
        deserialize: (String) -> T
    ): Result<T> {
        val networkResult = fetch()
        if (networkResult.isSuccess) {
            val value = networkResult.getOrThrow()
            val json = gson.toJson(value)
            cachedResponseDao.put(CachedResponse(key, json, System.currentTimeMillis()))
            return networkResult
        }
        val cached = cachedResponseDao.get(key)
        if (cached != null) {
            return Result.success(deserialize(cached.json))
        }
        return networkResult
    }

    suspend fun getCachedAt(key: String): Long? = cachedResponseDao.get(key)?.cachedAt
}
