package com.simonecompany.registratorevocale.data

import android.content.Context
import com.simonecompany.registratorevocale.model.Recording

class RecordingRepository(private val context: Context) {

    fun getRecordings(): List<Recording> =
        StorageHelper.getRecordings(context).map {
            it.copy(favorite = SettingsStore.isFavorite(context, it.name))
        }

    fun delete(recording: Recording): Boolean {
        SettingsStore.setFavorite(context, recording.name, false)
        return StorageHelper.delete(context, recording)
    }

    fun setFavorite(recording: Recording, favorite: Boolean) {
        SettingsStore.setFavorite(context, recording.name, favorite)
    }

    fun rename(recording: Recording, newName: String): Boolean {
        val result = StorageHelper.rename(context, recording, newName) ?: return false
        SettingsStore.renameFavorite(context, recording.name, result)
        return true
    }
}
