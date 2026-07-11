package com.rover.control.ui.arm

import androidx.lifecycle.ViewModel

class ArmViewModel : ViewModel() {
    val positions = IntArray(6) { 90 }
    var isRecording = false
    var isPlaying = false
    var delayMs = 500
    var repeatCount = 1
}
