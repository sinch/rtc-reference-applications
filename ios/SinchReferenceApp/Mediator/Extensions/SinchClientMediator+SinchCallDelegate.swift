import CallKit
import Foundation
import OSLog
import SinchRTC
import UIKit

// Make SinchClientMediator a delegate to a call by implementing Observer pattern.
// By implementing this we could handle call establishment, progress and ending whereever observers were added.
extension SinchClientMediator: SinchCallDelegate {
    
    // For each observer callback action will be called.
    private func fanoutDelegateCall(_ callback: (_ observer: SinchClientMediatorObserver?) -> Void) {
        observers.removeAll(where: { $0 === nil })
        observers.forEach { callback($0) }
    }
    
    func addObserver(_ observer: SinchClientMediatorObserver) {
        guard observers.firstIndex(where: { $0 === observer }) == nil else { return }
        
        observers.append(observer)
    }
    
    func removeObserver(_ observer: SinchClientMediatorObserver) {
        guard let index = observers.firstIndex(where: { $0 === observer }) else { return }
        
        observers.remove(at: index)
    }
    
    func callDidProgress(_ call: SinchCall) {
        if let callKitId = self.callRegistry.callKitUUID(forSinchId: call.callId), call.direction == .outgoing {
          self.provider.reportOutgoingCall(with: callKitId, startedConnectingAt: call.details.startedTime)
        }

        self.fanoutDelegateCall { $0?.callDidProgress(call) }
    }
    
    func callDidEstablish(_ call: SinchCall) {
        if let callKitId = self.callRegistry.callKitUUID(forSinchId: call.callId), call.direction == .outgoing {
            self.provider.reportOutgoingCall(with: callKitId, connectedAt: call.details.establishedTime)
        }
        
        self.fanoutDelegateCall { $0?.callDidEstablish(call) }
    }

    func callDidEnd(_ call: SinchCall) {
        defer {
            call.delegate = nil
        }
        
        if let uuid = self.callRegistry.callKitUUID(forSinchId: call.callId) {
            // Report end of the call to CallKit.
            self.provider.reportCall(with: uuid,
                                     endedAt: call.details.endedTime,
                                     reason: call.details.endCause.reason)
        }

        self.callRegistry.removeSinchCall(withId: call.callId)

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
    
    // If application is using video, SinchCallDelegate should be extended with callDidAddVideoTrack, callDidPauseVideoTrack, callDidResumeVideoTrack.
    
    // When video is available, notify all observers and perform specific actions.
    func callDidAddVideoTrack(_ call: SinchCall) {
        self.fanoutDelegateCall { $0?.callDidAddVideoTrack(call) }
    }
    
    // When video was paused, notify all observers and perform specific actions.
    func callDidPauseVideoTrack(_ call: SinchCall) {
        self.fanoutDelegateCall { $0?.callDidPauseVideoTrack(call) }
    }

    // When video was unpaused, notify all observers and perform specific actions.
    func callDidResumeVideoTrack(_ call: SinchCall) {
        self.fanoutDelegateCall { $0?.callDidResumeVideoTrack(call) }
    }
}

extension SinchRTC.SinchCallDetails.EndCause {
    
    var reason: CXCallEndedReason {
        switch self {
            case .error:
                return .failed
            case .denied,
                // .hangUp mapping is not really correct, as it is the end cause also when the local peer ended the call.
                .hungUp,
                // .inactive mapping is not really correct, as it is triggered by the local peer.
                .inactive:
                return .remoteEnded
            case .timeout,
                .canceled,
                .noAnswer:
                return .unanswered
            case .otherDeviceAnswered:
                return .answeredElsewhere
            default:
                return .failed
        }
    }
}
