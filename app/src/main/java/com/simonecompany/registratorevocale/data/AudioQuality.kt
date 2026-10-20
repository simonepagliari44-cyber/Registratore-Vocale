package com.simonecompany.registratorevocale.data

enum class AudioQuality(val bitRate: Int, val samplingRate: Int, val label: String) {
    LOW(64000, 44100, "Bassa"),
    MEDIUM(128000, 44100, "Media"),
    HIGH(192000, 44100, "Alta");

    companion object {
        fun fromName(name: String?): AudioQuality =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}
