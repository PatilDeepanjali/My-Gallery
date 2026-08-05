package com.example.mygallery.model

import android.content.IntentSender

sealed class DeleteResult {

    object Success : DeleteResult()

    data class ConfirmDelete(val intentSender: IntentSender) : DeleteResult()

    data class GrantPermissionThenRetry(
        val intentSender: IntentSender,
        val remainingUris: List<android.net.Uri>
    ) : DeleteResult()

    data class Error(val message: String) : DeleteResult()
}