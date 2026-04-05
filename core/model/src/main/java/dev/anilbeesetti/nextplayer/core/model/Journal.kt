package dev.anilbeesetti.nextplayer.core.model

import java.io.Serializable

@kotlinx.serialization.Serializable
data class Journal(
    val id: String,
    val name: String,
    val expectedDate: Long,
    val state: String,
    val materialsCount: Int,
    val updatedAt: Long = 0,
    val deleted: Boolean = false,
) : Serializable {
    companion object {
        val samples = listOf(
            Journal(
                id = "1",
                name = "Jornada de Pruebas A",
                // Friday, April 10, 2026
                expectedDate = 1775822400000L,
                state = "Pendiente",
                materialsCount = 5,
            ),
            Journal(
                id = "2",
                name = "Jornada de Pruebas B",
                // Saturday, April 11, 2026
                expectedDate = 1775908800000L,
                state = "Completada",
                materialsCount = 3,
            ),
        )
    }
}
