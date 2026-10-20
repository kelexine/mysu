package dev.kelexine.mysu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * Returns true after the first frame has rendered to allow smooth transitions.
 */
@Composable
fun rememberContentReady(): Boolean {
    val ready = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        ready.value = true
    }

    return ready.value
}
