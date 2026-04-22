package com.user.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "cached_responses")
data class CachedResponse(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val cachedAt: Long
)

@Dao
interface CachedResponseDao {
    @Query("SELECT * FROM cached_responses WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): CachedResponse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(response: CachedResponse)

    @Query("DELETE FROM cached_responses WHERE cacheKey = :key")
    suspend fun delete(key: String)
}
