package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anilbeesetti.nextplayer.core.database.entities.JournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals WHERE deleted = 0")
    fun getActiveJournals(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getJournalById(id: String): JournalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournals(journals: List<JournalEntity>)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteById(id: String)
}
