package dev.anilbeesetti.nextplayer.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsBundle(
    @SerialName("version")
    val version: Int = 1,
    @SerialName("app_preferences")
    val appPreferences: ApplicationPreferences,
    @SerialName("player_preferences")
    val playerPreferences: PlayerPreferences
)
