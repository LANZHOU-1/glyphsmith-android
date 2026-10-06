package com.lanzhou.zj

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ArtViewModel : ViewModel() {
    var bitmap by mutableStateOf<Bitmap?>(null)
    var sourceName by mutableStateOf("示例图")
}
