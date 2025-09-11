import Foundation
import LiveCommunicationKit
import OSLog
import UIKit

@available(iOS 17.4, *)
final class SinchLiveCommunicationKitService: NSObject, CommunicationService {

  private var conversationManager: ConversationManager!

  init(delegate: ConversationManagerDelegate) {
    super.init()

    let configuration = ConversationManager.Configuration(ringtoneName: Ringtone.incoming,
                                                          iconTemplateImageData: UIImage(named: "AppIcon")?.pngData(),
                                                          maximumConversationGroups: 1,
                                                          maximumConversationsPerConversationGroup: 1,
                                                          includesConversationInRecents: false,
                                                          supportsVideo: true,
                                                          // Identification of user for each call, .generic means
                                                          // that its based on uuid value.
                                                          supportedHandleTypes: [.generic])
    self.conversationManager = ConversationManager(configuration: configuration)
    os_log("ConversationManager initialized with configuration: %{public}@",
           log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
           type: .info,
           String(describing: configuration))

    self.conversationManager.delegate = delegate
  }

  // Initiates an outgoing LiveCommunicationKit call using ConversationManager, after success
  // 'conversationManager(_ manager: ConversationManager, perform action: ConversationAction)'
  // for 'StartConversationAction' will be called.
  func call(userId: String, uuid: UUID, isVideo: Bool, with completion: @escaping (Error?) -> Void) {
    let handle = Handle(type: .generic, value: userId)

    let startConversationAction = StartConversationAction(conversationUUID: uuid, handles: [handle], isVideo: isVideo)
    os_log("Performing StartConversationAction for userId: %{public}@, uuid: %{public}@",
           log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
           type: .info,
           userId,
           uuid.uuidString)

    Task {
      do {
        try await self.conversationManager.perform([startConversationAction])

        os_log("StartConversationAction performed successfully for uuid: %{public}@",
               log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
               type: .info,
               uuid.uuidString)

        completion(nil)
      } catch {
        os_log("Failed to start outgoing call for uuid: %{public}@, error: %{public}@",
               log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
               type: .error,
               uuid.uuidString,
               error.localizedDescription)

        completion(error)
      }
    }
  }

  func reportOutgoingCall(uuid: UUID, time: Date?, for state: OutgoingCallState) {
    guard let time = time else {
      os_log("Called reportOutgoingCall with nil time for uuid: %{public}@",
             log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
             type: .error,
             uuid.uuidString)
      return
    }

    guard let conversation = self.conversationManager.conversations.first(where: { $0.uuid == uuid }) else {
      os_log("Unknown conversation uuid in reportOutgoingCall: %{public}@",
             log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
             type: .error,
             uuid.uuidString)
      return
    }

    os_log("Reporting to conversation manager outgoing call for %{public}@ state for uuid: %{public}@",
           log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
           type: .info,
           String(describing: state),
           uuid.uuidString)

    switch state {
      case .progress:
        self.conversationManager.reportConversationEvent(.conversationStartedConnecting(time), for: conversation)
      case .answer:
        self.conversationManager.reportConversationEvent(.conversationConnected(time), for: conversation)
    }
  }

  func reportIncomingCall(localUserId: String,
                          remoteUserId: String,
                          uuid: UUID,
                          isVideoOffered: Bool,
                          with completion: @escaping (Error?) -> Void) {
    let localHandle = Handle(type: .generic, value: localUserId)
    let remoteHandle = Handle(type: .generic, value: remoteUserId)

    let update = Conversation.Update(localMember: localHandle,
                                     activeRemoteMembers: [remoteHandle],
                                     capabilities: isVideoOffered ? [.video] : nil)

    os_log("Report incoming call for localUserId: %{public}@, remoteUserId: %{public}@, uuid: %{public}@",
           log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
           type: .info,
           localUserId,
           remoteUserId,
           uuid.uuidString)

    Task {
      do {
        try await self.conversationManager.reportNewIncomingConversation(uuid: uuid, update: update)

        os_log("Incoming call reported successfully for uuid: %{public}@",
               log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
               type: .info,
               uuid.uuidString)

        completion(nil)
      } catch {
        os_log("Failed to report incoming call for uuid: %{public}@, error: %{public}@",
               log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
               type: .error,
               uuid.uuidString,
               error.localizedDescription)

        completion(error)
      }
    }
  }

  // Ends an active LiveCommunicationKit conversation, after success
  // 'conversationManager(_ manager: ConversationManager, perform action: ConversationAction)'
  // for 'EndConversationAction' will be called.
  func end(uuid: UUID, with completion: @escaping (Error?) -> Void) {
    let endConversationAction = EndConversationAction(conversationUUID: uuid)

    os_log("Performing EndConversationAction for uuid: %{public}@",
           log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
           type: .info,
           uuid.uuidString)

    Task {
      do {
        // Request to end a call to LiveCommunicationKit.
        try await self.conversationManager.perform([endConversationAction])

        os_log("EndConversationAction performed successfully for uuid: %{public}@",
               log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
               type: .info,
               uuid.uuidString)

        completion(nil)
      } catch {
        os_log("Failed to end conversation for uuid: %{public}@, error: %{public}@",
               log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
               type: .error,
               uuid.uuidString,
               error.localizedDescription)

        completion(error)
      }
    }
  }

  func reportCallEnd(uuid: UUID, time: Date?, endCause: EndCause) {
    guard let time = time else {
      os_log("Called reportCallEnd with nil time for uuid: %{public}@",
             log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
             type: .error,
             uuid.uuidString)
      return
    }

    guard let conversation = self.conversationManager.conversations.first(where: { $0.uuid == uuid }) else {
      os_log("Unknown conversation uuid in reportCallEnd: %{public}@",
             log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
             type: .error,
             uuid.uuidString)
      return
    }

    os_log("Reporting conversationEnded for uuid: %{public}@, reason: %{public}@",
           log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
           type: .info,
           uuid.uuidString,
           String(describing: endCause.conversationEndReason))

    self.conversationManager.reportConversationEvent(.conversationEnded(time, endCause.conversationEndReason),
                                                     for: conversation)
  }

  func declineIncomingCallIfBusy(uuid: UUID) {
    guard let conversation = self.conversationManager.conversations.first(where: { $0.uuid == uuid }) else {
      os_log("Unknown conversation uuid in declineIncomingCallIfBusy: %{public}@",
             log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
             type: .error,
             uuid.uuidString)
      return
    }

    os_log("Declining incoming call for conversation manager for uuid: %{public}@ due to active call",
           log: .sinchOSLog(for: SinchLiveCommunicationKitService.identifier),
           type: .info,
           uuid.uuidString)

    self.conversationManager.reportConversationEvent(.conversationEnded(Date(), .declinedElsewhere), for: conversation)
  }
}
