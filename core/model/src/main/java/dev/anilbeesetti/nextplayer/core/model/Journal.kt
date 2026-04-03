package dev.anilbeesetti.nextplayer.core.model

import java.io.Serializable

data class Journal(
    val id: String,
    val name: String,
    val expectedDate: Long,
    val state: String,
    val materialsCount: Int
) : Serializable {
    companion object {
        val samples = listOf(
            Journal(
                id = "1",
                name = "Jornada de Pruebas A",
                expectedDate = 1775822400000L, // Friday, April 10, 2026
                state = "Pendiente",
                materialsCount = 5
            ),
            Journal(
                id = "2",
                name = "Jornada de Pruebas B",
                expectedDate = 1775908800000L, // Saturday, April 11, 2026
                state = "Completada",
                materialsCount = 3
            )
        )
    }
}
