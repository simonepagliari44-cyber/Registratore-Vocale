package com.simonecompany.registratorevocale.data

import android.content.Context
import com.simonecompany.registratorevocale.model.Recording

class RecordingRepository(private val context: Context) {

    fun getRecordings(): List<Recording> = StorageHelper.getRecordings(context)

    fun delete(recording: Recording): Boolean = StorageHelper.delete(context, recording)
}
