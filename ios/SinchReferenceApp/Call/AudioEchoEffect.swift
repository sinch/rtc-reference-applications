import AVFoundation
import OSLog
import SinchRTC

/// Demonstrates SinchLocalAudioFrameDelegate and SinchRemoteAudioFrameDelegate by applying
/// a delay/echo effect to raw PCM buffers before encoding (local path) or playout (remote path).
///
/// A 250 ms delay line with 40 % feedback — each successive echo is ~8 dB quieter, producing
/// a natural-sounding room echo.
///
/// Register one instance per path:
///   audioController.localAudioFrameDelegate  = localEchoEffect   // mic → encoder
///   audioController.remoteAudioFrameDelegate = remoteEchoEffect  // decoder → speaker
///
/// Each instance maintains an independent delay buffer; the two paths must use separate
/// instances.
final class AudioEchoEffect: NSObject, SinchLocalAudioFrameDelegate, SinchRemoteAudioFrameDelegate {

  var isEnabled = false

  private let delayMs = 250
  private let feedbackGain: Float = 0.4
  private var delayBuffer: [Int16] = []
  private var writeIndex = 0
  private var currentDelaySamples = 0

  private let log = OSLog.sinchOSLog(for: "AudioEchoEffect")

  // MARK: - SinchLocalAudioFrameDelegate

  func onLocalAudioFrame(_ frame: AVAudioPCMBuffer) -> Bool {
    process(frame)
    return true
  }

  func audioRecordingDidStart(with format: AVAudioFormat) {
    os_log("Audio recording did start — %{public}@", log: log, format.description)
  }

  func audioRecordingDidStop(with format: AVAudioFormat) {
    os_log("Audio recording did stop — %{public}@", log: log, format.description)
  }

  // MARK: - SinchRemoteAudioFrameDelegate

  func onRemoteAudioFrame(_ frame: AVAudioPCMBuffer) -> Bool {
    process(frame)
    return true
  }

  func audioPlayoutDidStart(with format: AVAudioFormat) {
    os_log("Audio playout did start — %{public}@", log: log, format.description)
  }

  func audioPlayoutDidStop(with format: AVAudioFormat) {
    os_log("Audio playout did stop — %{public}@", log: log, format.description)
  }

  // MARK: - PCM processing

  private func ensureBuffer(sampleRate: Int, channels: Int) {
    let needed = delayMs * sampleRate / 1000 * channels
    guard currentDelaySamples != needed else { return }
    delayBuffer = [Int16](repeating: 0, count: needed)
    writeIndex = 0
    currentDelaySamples = needed
  }

  private func process(_ buffer: AVAudioPCMBuffer) {
    guard isEnabled, let samples = buffer.int16ChannelData else { return }

    let sampleRate = Int(buffer.format.sampleRate)
    let channels = Int(buffer.format.channelCount)
    let validSamples = Int(buffer.frameLength) * channels // interleaved layout

    ensureBuffer(sampleRate: sampleRate, channels: channels)
    guard currentDelaySamples > 0 else { return }

    let ptr = samples[0]
    for idx in 0 ..< validSamples {
      let delayed = delayBuffer[writeIndex]
      let sum = Float(ptr[idx]) + Float(delayed) * feedbackGain
      let out = Int16(max(Float(Int16.min), min(Float(Int16.max), sum)))
      delayBuffer[writeIndex] = out
      ptr[idx] = out
      writeIndex = (writeIndex + 1) % currentDelaySamples
    }
  }
}
