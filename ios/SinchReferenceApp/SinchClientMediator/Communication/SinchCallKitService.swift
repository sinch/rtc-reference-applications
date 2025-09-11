import CallKit
import Foundation
import OSLog

final class SinchCallKitService: NSObject, CommunicationService {

  private var callController: CXCallController!

  private var provider: CXProvider!

  init(delegate: CXProviderDelegate) {
    super.init()

    self.callController = CXCallController()

    let configuration: CXProviderConfiguration

    if #available(iOS 14.0, *) {
      configuration = CXProviderConfiguration()
    } else {
      configuration = CXProviderConfiguration(localizedName: "sinch_ref_app")
    }

    configuration.supportedHandleTypes = [.generic]
    configuration.supportsVideo = true
    configuration.ringtoneSound = Ringtone.incoming

    self.provider = CXProvider(configuration: configuration)
    self.provider.setDelegate(delegate, queue: nil)

    os_log("Provider initialized with configuration: %{public}@",
           log: .sinchOSLog(for: SinchCallKitService.identifier),
           type: .info,
           String(describing: configuration))
  }

  // Initiates an outgoing CallKit call using CXCallController, after success
  // 'provider(_ provider: CXProvider, perform action: CXStartCallAction)'
  // event will be called.
  func call(userId: String, uuid: UUID, isVideo: Bool, with completion: @escaping (Error?) -> Void) {
    let handle = CXHandle(type: .generic, value: userId)

    let startCallAction = CXStartCallAction(call: uuid, handle: handle)
    startCallAction.isVideo = isVideo
    let startCallTransaction = CXTransaction(action: startCallAction)

    os_log("Request callController to start call for userId: %{public}@, uuid: %{public}@",
           log: .sinchOSLog(for: SinchCallKitService.identifier),
           type: .info,
           userId,
           uuid.uuidString)

    self.callController.request(startCallTransaction, completion: completion)
  }

  func reportOutgoingCall(uuid: UUID, time: Date?, for state: OutgoingCallState) {
    os_log("Reporting to provider outgoing call for %{public}@ state for uuid: %{public}@",
           log: .sinchOSLog(for: SinchCallKitService.identifier),
           type: .info,
           String(describing: state),
           uuid.uuidString)

    switch state {
      case .progress:
        self.provider.reportOutgoingCall(with: uuid, startedConnectingAt: time)
      case .answer:
        self.provider.reportOutgoingCall(with: uuid, connectedAt: time)
    }
  }

  func reportIncomingCall(localUserId: String,
                          remoteUserId: String,
                          uuid: UUID,
                          isVideoOffered: Bool,
                          with completion: @escaping (Error?) -> Void) {
    let update = CXCallUpdate()

    update.remoteHandle = CXHandle(type: .generic, value: remoteUserId)
    update.hasVideo = isVideoOffered

    os_log("Report incoming call for remoteUserId: %{public}@, uuid: %{public}@",
           log: .sinchOSLog(for: SinchCallKitService.identifier),
           type: .info,
           remoteUserId,
           uuid.uuidString)

    self.provider.reportNewIncomingCall(with: uuid, update: update, completion: completion)
  }

  // Ends an active CallKit call via CXCallController., after success
  // 'provider(_ provider: CXProvider, perform action: CXEndCallAction)'
  // event will be called.
  func end(uuid: UUID, with completion: @escaping (Error?) -> Void) {
    let endCallAction = CXEndCallAction(call: uuid)
    let endCallTransaction = CXTransaction(action: endCallAction)

    os_log("Request callController to end call for uuid: %{public}@",
           log: .sinchOSLog(for: SinchCallKitService.identifier),
           type: .info,
           uuid.uuidString)

    self.callController.request(endCallTransaction, completion: completion)
  }

  func reportCallEnd(uuid: UUID, time: Date?, endCause: EndCause) {
    os_log("Reporting call endedAt for uuid: %{public}@, reason: %{public}@",
           log: .sinchOSLog(for: SinchCallKitService.identifier),
           type: .info,
           uuid.uuidString,
           String(describing: endCause.callEndReason))

    self.provider.reportCall(with: uuid, endedAt: time, reason: endCause.callEndReason)
  }

  func declineIncomingCallIfBusy(uuid: UUID) {
    os_log("Declining incoming call for provider for uuid: %{public}@ due to active call",
           log: .sinchOSLog(for: SinchCallKitService.identifier),
           type: .info,
           uuid.uuidString)

    self.provider.reportCall(with: uuid, endedAt: nil, reason: .declinedElsewhere)
  }
}
