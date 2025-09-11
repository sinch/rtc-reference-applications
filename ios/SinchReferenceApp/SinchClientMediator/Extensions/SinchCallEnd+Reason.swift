import CallKit
import Foundation
import LiveCommunicationKit
import SinchRTC

typealias EndCause = SinchRTC.SinchCallDetails.EndCause

extension SinchRTC.SinchCallDetails.EndCause {

  var callEndReason: CXCallEndedReason {
    switch self {
      // .hangUp mapping is not really correct, as it is the end cause also when the local peer ended the call.
      // .inactive mapping is not really correct, as it is triggered by the local peer.
      case .denied, .hungUp, .inactive:
        return .remoteEnded
      case .timeout, .canceled, .noAnswer:
        return .unanswered
      case .otherDeviceAnswered:
        return .answeredElsewhere
      case .error, .voipCallDetected, .gsmCallDetected:
        return .failed
      default:
        return .failed
    }
  }
}

@available(iOS 17.4, *)
extension SinchRTC.SinchCallDetails.EndCause {

  var conversationEndReason: Conversation.EndedReason {
    switch self {
      // .hangUp mapping is not really correct, as it is the end cause also when the local peer ended the call.
      // .inactive mapping is not really correct, as it is triggered by the local peer.
      case .denied, .hungUp, .inactive:
        return .remoteEnded
      case .timeout, .canceled, .noAnswer:
        return .unanswered
      case .otherDeviceAnswered:
        return .joinedElsewhere
      case .error, .voipCallDetected, .gsmCallDetected:
        return .failed
      default:
        return .failed
    }
  }
}
