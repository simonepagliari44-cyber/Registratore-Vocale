package com.simonecompany.registratorevocale.model

import android.net.Uri

data class Recording(
    val id: String,
    val name: String,
    val uri: Uri,
    val durationMillis: Long,
    val createdAt: Long,
    val favorite: Boolean = false
)
