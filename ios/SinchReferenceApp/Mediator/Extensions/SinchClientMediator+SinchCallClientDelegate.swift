import OSLog
import SinchRTC
import UIKit

// MARK: - Call client delegate

extension SinchClientMediator: SinchCallClientDelegate {

  // To react to an incoming call, follow these steps:
  // - assign call delegate, to handle call progress
  // - add call to callRegistry, to be able to fetch it in CallKit callbacks
  // - possibly propagate the "incoming call" event to ViewControllers
  func client(_ client: SinchRTC.SinchCallClient, didReceiveIncomingCall call: SinchRTC.SinchCall) {
    os_log("Did receive incoming call with sinch call with id: %{public}@, from remote user id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           call.callId,
           call.remoteUserId)

    os_log("App state: %{public}d", UIApplication.shared.applicationState.rawValue)

    // To handle call events properly, its important to set call delegate.
    call.delegate = self

    callRegistry.addSinchCall(call)

    guard UIApplication.shared.applicationState != .background else { return }

    delegate?.handleIncomingCall(call)
  }
}
