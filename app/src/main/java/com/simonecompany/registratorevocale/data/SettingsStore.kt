package com.simonecompany.registratorevocale.data

import android.content.Context

object SettingsStore {

    private const val PREFS = "registratore_prefs"
    private const val KEY_QUALITY = "audio_quality"
    private const val KEY_FAVORITES = "favorites"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getQuality(context: Context): AudioQuality =
        AudioQuality.fromName(prefs(context).getString(KEY_QUALITY, null))

    fun setQuality(context: Context, quality: AudioQuality) {
        prefs(context).edit().putString(KEY_QUALITY, quality.name).apply()
    }

    private fun favorites(context: Context): MutableSet<String> =
        prefs(context).getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet()
            ?: mutableSetOf()

    fun isFavorite(context: Context, name: String): Boolean =
        prefs(context).getStringSet(KEY_FAVORITES, emptySet())?.contains(name) == true

    fun setFavorite(context: Context, name: String, favorite: Boolean) {
        val current = favorites(context)
        if (favorite) current.add(name) else current.remove(name)
        prefs(context).edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun renameFavorite(context: Context, oldName: String, newName: String) {
        val current = favorites(context)
        if (current.remove(oldName)) {
            current.add(newName)
            prefs(context).edit().putStringSet(KEY_FAVORITES, current).apply()
        }
    }
}
