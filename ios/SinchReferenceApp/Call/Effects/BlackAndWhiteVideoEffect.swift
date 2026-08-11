import CoreVideo
import OSLog
import SinchRTC

/// Demonstrates SinchLocalVideoFrameDelegate and SinchRemoteVideoFrameDelegate by converting
/// each frame to black & white before it is encoded (local path) or rendered (remote path).
///
/// The conversion is done in place on the SDK-owned input buffer: for the bi-planar 4:2:0
/// (NV12) frames the camera and the H.264 decoder produce on iOS, setting the chroma (Cb/Cr)
/// plane to the neutral value 128 removes all color while keeping luma — i.e. grayscale. The
/// same buffer is then handed back to the completion handler (no new buffer needed).
///
/// Register per path:
///   videoController.localVideoFrameDelegate  = localBWEffect   // camera → encoder
///   videoController.remoteVideoFrameDelegate = remoteBWEffect  // decoder → renderer
final class BlackAndWhiteVideoEffect: NSObject, SinchLocalVideoFrameDelegate, SinchRemoteVideoFrameDelegate {

  private let log = OSLog.sinchOSLog(for: "BlackAndWhiteVideoEffect")

  // MARK: - SinchLocalVideoFrameDelegate

  func onLocalVideoFrame(_ cvPixelBuffer: CVPixelBuffer, completionHandler: @escaping (CVPixelBuffer) -> Void) {
    convertToGrayscale(cvPixelBuffer)
    completionHandler(cvPixelBuffer)
  }

  // MARK: - SinchRemoteVideoFrameDelegate

  func onRemoteVideoFrame(_ cvPixelBuffer: CVPixelBuffer, completionHandler: @escaping (CVPixelBuffer) -> Void) {
    convertToGrayscale(cvPixelBuffer)
    completionHandler(cvPixelBuffer)
  }

  // MARK: - Grayscale

  /// In-place grayscale for bi-planar 4:2:0 YCbCr (NV12) buffers, the format produced by the
  /// camera and the H.264 decoder on iOS. Other formats are left unchanged for simplicity;
  /// a production integration would convert them (e.g. via Core Image rendering into a new
  /// buffer, which the completion handler also accepts).
  private func convertToGrayscale(_ pixelBuffer: CVPixelBuffer) {
    let format = CVPixelBufferGetPixelFormatType(pixelBuffer)
    guard format == kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange ||
      format == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange else {
      os_log("Unsupported pixel format %{public}d, leaving frame unchanged", log: log, type: .info, format)
      return
    }

    CVPixelBufferLockBaseAddress(pixelBuffer, [])
    defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, []) }

    // Plane 1 is the interleaved Cb/Cr (chroma) plane; 128 is the neutral (colorless) value.
    guard let chroma = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 1) else { return }
    let bytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 1)
    let height = CVPixelBufferGetHeightOfPlane(pixelBuffer, 1)
    memset(chroma, 128, bytesPerRow * height)
  }
}
