import CallKit
import Foundation
import LiveCommunicationKit
import OSLog
import SinchRTC
import UIKit

enum OutgoingCallState {

  case progress, answer
}

// Makes SinchClientMediator an observer-based call delegate.
// This lets us handle call establishment, progress, and ending through registered observers.
extension SinchClientMediator: SinchCallDelegate {

  private func fanoutDelegateCall(_ callback: (_ observer: SinchClientMediatorObserver?) -> Void) {
    self.observers.removeAll(where: { $0 === nil })
    self.observers.forEach { callback($0) }
  }

  func addObserver(_ observer: SinchClientMediatorObserver) {
    guard self.observers.firstIndex(where: { $0 === observer }) == nil else { return }

    self.observers.append(observer)
  }

  func removeObserver(_ observer: SinchClientMediatorObserver) {
    guard let index = self.observers.firstIndex(where: { $0 === observer }) else { return }

    self.observers.remove(at: index)
  }

  func callDidProgress(_ call: SinchCall) {
    if let uuid = self.callRegistry.uuid(from: call.callId), call.direction == .outgoing {
      self.reportCallProgressed(call, with: uuid)
    }

    os_log("Call did progress for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           call.callId)

    self.fanoutDelegateCall { $0?.callDidProgress(call) }
  }

  func callDidRing(_ call: SinchCall) {
    os_log("Call did ring for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           call.callId)

    self.fanoutDelegateCall { $0?.callDidRing(call) }
  }

  func callDidAnswer(_ call: SinchRTC.SinchCall) {
    if let uuid = self.callRegistry.uuid(from: call.callId), call.direction == .outgoing {
      self.reportCallAnswered(call, with: uuid)
    }

    os_log("Call did answer for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           call.callId)

    self.fanoutDelegateCall { $0?.callDidAnswer(call) }
  }

  func callDidEstablish(_ call: SinchCall) {
    os_log("Call did establish for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           call.callId)

    self.fanoutDelegateCall { $0?.callDidEstablish(call) }
  }

  func callDidEnd(_ call: SinchCall) {
    defer {
      call.delegate = nil
    }

    if let uuid = self.callRegistry.uuid(from: call.callId) {
      self.reportCallEnded(call, with: uuid)
    }

    os_log("Call did end for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           call.callId)

    self.callRegistry.removeSinchCall(withId: call.callId)

    os_log("%{public}@", call.details.toString)

    if call.details.endCause == .error {
      os_log("""
             Call ended failed with reason: %{public}@, error: %{public}@.
             Removing sinch call with id: %{public}@
             """,
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             call.details.endCause.rawValue,
             call.details.error?.localizedDescription ?? "?",
             call.callId)
    } else {
      os_log("Call ended succeed with reason: %{public}@. Removing sinch call with id: %{public}@",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             call.details.endCause.rawValue,
             call.callId)
    }

    self.fanoutDelegateCall { $0?.callDidEnd(call) }
  }

  func callDidEmitCallQualityEvent(_ call: SinchCall, event: SinchCallQualityWarningEvent) {
    os_log("Event: %{public}@ Call id: %{public}@.", event.toString, call.callId)
    self.fanoutDelegateCall { $0?.callDidEmitCallQualityEvent(call, event: event) }
  }

  // If application is using video, SinchCallDelegate should be extended with
  // callDidAddVideoTrack, callDidPauseVideoTrack, callDidResumeVideoTrack.

  func callDidAddVideoTrack(_ call: SinchCall) {
    self.fanoutDelegateCall { $0?.callDidAddVideoTrack(call) }
  }

  func callDidPauseVideoTrack(_ call: SinchCall) {
    self.fanoutDelegateCall { $0?.callDidPauseVideoTrack(call) }
  }

  func callDidResumeVideoTrack(_ call: SinchCall) {
    self.fanoutDelegateCall { $0?.callDidResumeVideoTrack(call) }
  }
}

// Support methods for reporting call progress, answer, and end to the communication kit services.
extension SinchClientMediator {

  private func reportCallProgressed(_ call: SinchCall, with uuid: UUID) {
    guard let communicationService = self.communicationService else {
      os_log("%{public}@ service is not initialized",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             self.communicationKit.toString())
      return
    }

    communicationService.reportOutgoingCall(uuid: uuid, time: call.details.startedTime, for: .progress)
  }

  private func reportCallAnswered(_ call: SinchCall, with uuid: UUID) {
    guard let communicationService = self.communicationService else {
      os_log("%{public}@ service is not initialized",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             self.communicationKit.toString())
      return
    }

    communicationService.reportOutgoingCall(uuid: uuid, time: call.details.establishedTime, for: .answer)
  }

  private func reportCallEnded(_ call: SinchCall, with uuid: UUID) {
    guard let communicationService = self.communicationService else {
      os_log("%{public}@ service is not initialized",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             self.communicationKit.toString())
      return
    }

    communicationService.reportCallEnd(uuid: uuid, time: call.details.endedTime, endCause: call.details.endCause)
  }
}
