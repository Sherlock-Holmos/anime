package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.withFrameNanos

actual suspend fun awaitFrame() {
    withFrameNanos { }
}
