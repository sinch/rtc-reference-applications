package com.sinch.rtc.vvc.reference.app.features.calls.established.screenshot

sealed class FrameCaptureResult

data class Captured(val destination: String) : FrameCaptureResult()
data class Error(val error: Throwable) : FrameCaptureResult()