package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.anilbeesetti.nextplayer.core.model.Journal

@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val expectedDate: Long,
    val state: String,
    val materialsCount: Int,
    val updatedAt: Long,
    val deleted: Boolean,
)

fun JournalEntity.asExternalModel() = Journal(
    id = id,
    name = name,
    expectedDate = expectedDate,
    state = state,
    materialsCount = materialsCount,
    updatedAt = updatedAt,
    deleted = deleted,
)

fun Journal.asEntity() = JournalEntity(
    id = id,
    name = name,
    expectedDate = expectedDate,
    state = state,
    materialsCount = materialsCount,
    updatedAt = updatedAt,
    deleted = deleted,
)
