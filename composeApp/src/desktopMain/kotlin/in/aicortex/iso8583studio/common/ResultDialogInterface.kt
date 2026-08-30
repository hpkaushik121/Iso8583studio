package `in`.aicortex.iso8583studio.data

import androidx.compose.runtime.Composable

interface ResultDialogInterface {
    fun onError(item: @Composable () -> Unit)
    fun onSuccess(item: @Composable () -> Unit)
}