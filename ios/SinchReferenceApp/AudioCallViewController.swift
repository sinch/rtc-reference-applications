import OSLog
import SinchRTC
import UIKit

final class AudioCallViewController: UIViewController {

  private enum Constant {

    static let cornerRadius: CGFloat = 6
  }

  @IBOutlet private var callInfoLabel: UILabel!

  @IBOutlet private var muteButton: UIButton! {
    didSet {
      muteButton.layer.cornerRadius = Constant.cornerRadius
    }
  }

  @IBOutlet private var speakerButton: UIButton! {
    didSet {
      speakerButton.layer.cornerRadius = Constant.cornerRadius
    }
  }

  @IBOutlet private var hangupButton: UIButton! {
    didSet {
      hangupButton.setup(with: .hangup)
    }
  }

  // To end call, should be passed to AudioCallViewController.
  var call: SinchCall?

  weak var callCompletionDelegate: CallCompletionDelegate?

  private var timer: Timer?
  private var duration: String = "00:00"

  private var muted: Bool = false
  private var speakerEnabled: Bool = false

  override func viewDidLoad() {
    super.viewDidLoad()

    hangupButton.isEnabled = call?.direction == .outgoing
    muteButton.isEnabled = false
    speakerButton.isEnabled = false

    // Add observer to track call actions.
    SinchClientMediator.instance.addObserver(self)
  }

  @IBAction private func toggleMute(_ sender: Any) {
    muted
      ? SinchClientMediator.instance.sinchClient?.audioController.unmute()
      : SinchClientMediator.instance.sinchClient?.audioController.mute()

    muted = !muted

    let mutedImage = muted ? UIImage(systemName: "mic.slash") : UIImage(systemName: "mic")
    muteButton.setImage(mutedImage, for: .normal)
  }

  @IBAction private func toggleSpeaker(_ sender: Any) {
    speakerEnabled
      ? SinchClientMediator.instance.sinchClient?.audioController.disableSpeaker()
      : SinchClientMediator.instance.sinchClient?.audioController.enableSpeaker()

    speakerEnabled = !speakerEnabled

    let speakerImage = speakerEnabled ? UIImage(systemName: "speaker.wave.2") : UIImage(systemName: "speaker.slash")
    speakerButton.setImage(speakerImage, for: .normal)
  }

  // End call implementation.
  @IBAction private func hangup(_ sender: Any) {
    terminateCall()

    dismiss(animated: true)
  }

  override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)

    terminateCall()
  }

  private func terminateCall() {
    guard let call = call else {
      os_log("Call was set and ongoing: %{public}@",
             log: .sinchOSLog(for: AudioCallViewController.identifier),
             type: .error,
             (call != nil).description)
      return
    }

    SinchClientMediator.instance.end(call: call)
  }
}

// SinchCallDelegate methods implementation and handling of call progression.
extension AudioCallViewController: SinchClientMediatorObserver {

  func callDidProgress(_ call: SinchCall) {
    self.callInfoLabel.text = "Initiating..."
    os_log("Call did progress for call: %{public}@", call.callId)
  }

  func callDidRing(_ call: SinchCall) {
    self.callInfoLabel.text = "Ringing..."
    os_log("Call did ring for call: %{public}@", call.callId)

    let audio = Ringtone.ringback
    let path = Bundle.main.path(forResource: audio, ofType: nil)

    do {
      try SinchClientMediator.instance.sinchClient?
        .audioController
        .startPlayingSoundFile(withPath: path, looping: true)
    } catch {
      os_log("WARNING: path for resource %{public}@ not found in the main bundle",
             log: .sinchOSLog(for: AudioCallViewController.identifier),
             type: .error,
             audio)
    }
  }

  func callDidAnswer(_ call: SinchRTC.SinchCall) {
    hangupButton.isEnabled = true
    self.callInfoLabel.text = "Connecting..."

    // Stop playing sound when call was answered.
    SinchClientMediator.instance.sinchClient?
      .audioController
      .stopPlayingSoundFile()

    os_log("Call did answer for call: %{public}@", call.callId)
  }

  func callDidEstablish(_ call: SinchCall) {
    hangupButton.isEnabled = true
    muteButton.isEnabled = true
    speakerButton.isEnabled = true

    guard timer == nil else { return }

    timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { _ in
      let interval = Int(Date().timeIntervalSince(call.details.establishedTime ?? Date()))

      let minutes = Int(interval / 60).timePresentation
      let seconds = Int(interval % 60).timePresentation

      // Displays some call information when call was established.
      self.duration = "\(minutes):\(seconds)"
      self.callInfoLabel.text = "\(self.duration) with \(call.remoteUserId)"
    })

    os_log("Call did establish for call: %{public}@", call.callId)
  }

  func callDidEmitCallQualityEvent(_ call: SinchCall, event: SinchCallQualityWarningEvent) {
    let message = "Call Quality Event:\n" + event.toString
    let banner = CallQualityEventBanner(message: message, triggered: event.eventType == .trigger, for: view)

    banner.show()
  }

  func callDidEnd(_ call: SinchCall) {
    timer?.invalidate()
    timer = nil

    // Finish call, by stop playing sound, dimissing and removing observers.
    dismiss(animated: true)
    SinchClientMediator.instance.sinchClient?
      .audioController
      .stopPlayingSoundFile()
    SinchClientMediator.instance.removeObserver(self)

    dismiss(animated: true, completion: { [weak self] in
      guard let self = self else { return }

      self.callCompletionDelegate?.provideCallDetails(for: self.call, with: self.duration)
    })
  }
}
