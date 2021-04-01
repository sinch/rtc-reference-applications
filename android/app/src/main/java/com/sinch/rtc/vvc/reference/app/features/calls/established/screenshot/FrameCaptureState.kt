package com.sinch.rtc.vvc.reference.app.features.calls.established.screenshot

sealed class FrameCaptureState

object Idle : FrameCaptureState()
object Triggered : FrameCaptureState()
object Capturing : FrameCaptureState()
