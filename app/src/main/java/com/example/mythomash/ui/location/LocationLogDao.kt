package com.example.mythomash.ui.location

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LocationLogDao {
    @Insert
    suspend fun insert(log: LocationLog)

    @Query("DELETE FROM location_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM location_logs")
    fun getAllLogs(): LiveData<List<LocationLog>>
}