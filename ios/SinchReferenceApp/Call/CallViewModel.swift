import Combine
import OSLog
import SinchRTC

struct CallState {

  enum Status {

    case event(QualityEvent)
    case addVideoTrack(UIView)
    case end(call: SinchCall?, duration: String)
    case none
  }

  struct QualityEvent {

    var message: String
    var triggered: Bool
  }

  var duration: String = "00:00"
  var info: String? = ""

  var couldHangUp: Bool
  var couldMute: Bool = false
  var couldEnableSpeaker: Bool = false
  var couldPause: Bool = false

  var muted: Bool = false
  var speakerEnabled: Bool = false
  var paused: Bool = false

  var status: Status = .none
}

final class CallViewModel {

  private var call: SinchCall?
  private let clientMediator: SinchClientMediator

  private var timer: Timer?

  @Published private(set) var state: CallState

  private let type: CallType

  init(call: SinchCall?, type: CallType, clientMediator: SinchClientMediator) {
    self.state = CallState(couldHangUp: call?.direction == .outgoing)

    self.call = call
    self.type = type

    self.clientMediator = clientMediator
    self.clientMediator.addObserver(self)
  }

  func toggleMute() {
    let muted = state.muted

    muted
      ? clientMediator.sinchClient?.audioController.unmute()
      : clientMediator.sinchClient?.audioController.mute()

    update { $0.muted = !muted }
  }

  func toggleSpeaker() {
    let speakerEnabled = state.speakerEnabled

    speakerEnabled
      ? clientMediator.sinchClient?.audioController.disableSpeaker()
      : clientMediator.sinchClient?.audioController.enableSpeaker()

    update { $0.speakerEnabled = !speakerEnabled }
  }

  func togglePause() {
    guard let call = call else { return }

    let paused = state.paused

    paused
      ? call.resumeVideo()
      : call.pauseVideo()

    update { $0.paused = !paused }
  }

  func localVideoTrack() -> UIView {
    guard let localView = clientMediator.sinchClient?.videoController.localView else {
      return UIView()
    }

    return localView
  }

  func toggleSwitchCamera() {
    clientMediator.sinchClient?.videoController.captureDevicePosition.toggle()
  }

  func terminate() {
    guard let call = call else {
      os_log("Call was set and ongoing: %{public}@",
             type: .error,
             (call != nil).description)
      return
    }

    clientMediator.end(call: call)
  }

  private func update(state transform: (inout CallState) -> Void) {
    var viewState = self.state
    transform(&viewState)
    self.state = viewState
  }
}

extension CallViewModel: SinchClientMediatorObserver {

  func callDidProgress(_ call: SinchCall) {
    self.update { $0.info = "Initiating..." }

    os_log("Call did progress for call: %{public}@", call.callId)
  }

  func callDidRing(_ call: SinchCall) {
    self.update { $0.info = "Ringing..." }

    os_log("Call did ring for call: %{public}@", call.callId)

    let audio = Ringtone.ringback
    let path = Bundle.main.path(forResource: audio, ofType: nil)

    do {
      try clientMediator.sinchClient?.audioController.startPlayingSoundFile(withPath: path, looping: true)
    } catch {
      os_log("WARNING: path for resource %{public}@ not found in the main bundle",
             type: .error,
             audio)
    }
  }

  func callDidAnswer(_ call: SinchRTC.SinchCall) {
    update { $0.info = "Connecting..." }

    clientMediator.sinchClient?.audioController.stopPlayingSoundFile()

    os_log("Call did answer for call: %{public}@", call.callId)
  }

  func callDidEstablish(_ call: SinchCall) {
    update {
      $0.couldHangUp = true
      $0.couldMute = true
      $0.couldEnableSpeaker = true
    }

    if timer != nil {
      timer?.invalidate()
      timer = nil
    }

    timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { [weak self] _ in
      guard let self = self else { return }

      let interval = Int(Date().timeIntervalSince(call.details.establishedTime ?? Date()))

      let minutes = Int(interval / 60).timePresentation
      let seconds = Int(interval % 60).timePresentation

      self.update {
        $0.duration = "\(minutes):\(seconds)"
        $0.info = "\($0.duration) with \(call.remoteUserId)"
      }
    })

    os_log("Call did establish for call: %{public}@", call.callId)

    guard type == .video, !state.speakerEnabled else { return }

    toggleSpeaker()
  }

  func callDidEmitCallQualityEvent(_ call: SinchCall, event: SinchCallQualityWarningEvent) {
    update {
      $0.status = .event(.init(message: "Call Quality Event:\n" + event.toString,
                               triggered: event.eventType == .trigger))
    }
  }

  func callDidEnd(_ call: SinchCall) {
    timer?.invalidate()
    timer = nil

    clientMediator.sinchClient?.audioController.stopPlayingSoundFile()
    clientMediator.removeObserver(self)

    update { $0.status = .end(call: call, duration: $0.duration) }

    os_log("Call did end for call: %{public}@", call.callId)

    guard type == .video, state.speakerEnabled else { return }

    toggleSpeaker()
  }

  func callDidAddVideoTrack(_ call: SinchCall) {
    guard let remoteView = clientMediator.sinchClient?.videoController.remoteView else { return }

    os_log("Video is available and was added", type: .info)

    update { $0.status = .addVideoTrack(remoteView) }
  }

  func callDidPauseVideoTrack(_ call: SinchCall) {
    os_log("Video was paused", type: .info)
  }

  func callDidResumeVideoTrack(_ call: SinchCall) {
    os_log("Video was resumed", type: .info)
  }
}

extension CallState.Status: Equatable {

  static func == (lhs: CallState.Status, rhs: CallState.Status) -> Bool {
    switch (lhs, rhs) {
      case (.none, .none):
        return true
      case (.event(let lhsEvent), .event(let rhsEvent)):
        return lhsEvent == rhsEvent
      case (.end(let lhsCall, let lhsDuration), .end(let rhsCall, let rhsDuration)):
        return lhsCall?.callId == rhsCall?.callId && lhsDuration == rhsDuration
      default:
        return false
    }
  }
}

extension CallState.QualityEvent: Equatable {

  static func == (lhs: CallState.QualityEvent, rhs: CallState.QualityEvent) -> Bool {
    return lhs.message == rhs.message && lhs.triggered == rhs.triggered
  }
}
