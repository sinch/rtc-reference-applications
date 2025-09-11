import AVFoundation
import Foundation
import LiveCommunicationKit
import OSLog
import SinchRTC

@available(iOS 17.4, *)
extension SinchClientMediator: ConversationManagerDelegate {

  func conversationManagerDidBegin(_ manager: ConversationManager) {
    os_log("ConversationManager did begin",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info)
  }

  func conversationManager(_ manager: ConversationManager, didActivate audioSession: AVAudioSession) {
    self.sinchClient?.callClient.didActivate(audioSession: audioSession)
  }

  func conversationManager(_ manager: ConversationManager, didDeactivate audioSession: AVAudioSession) {
    self.sinchClient?.callClient.didDeactivate(audioSession: audioSession)
  }

  func conversationManager(_ manager: ConversationManager, perform action: ConversationAction) {
    os_log("ConversationManager performs action: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           String(describing: action))

    switch action {
      case let start as StartConversationAction: return self.perform(action: start)
      case let join as JoinConversationAction: return self.perform(action: join)
      case let end as EndConversationAction: return self.perform(action: end)
      default:
        action.fulfill()
    }
  }

  private func perform(action: StartConversationAction) {
    defer { callStartedCallback = nil }

    guard let callClient = self.sinchClient?.callClient else {
      os_log("SinchClient not assigned to start call",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)
      action.fail()
      callStartedCallback?(.failure(CallError.noClient))
      return
    }

    guard let type = self.callTypes[action.conversationUUID.uuidString] else {
      action.fail()
      callStartedCallback?(.failure(CallError.noCallType(action.conversationUUID.uuidString)))
      return
    }

    guard let recipientIdentifier = action.handles.first?.value, !recipientIdentifier.isEmpty else {
      action.fail()
      callStartedCallback?(.failure(CallError.noRecepientIdentifier))
      return
    }

    let callResult: Result<SinchCall, Error>

    // To perform different calls (audio, video, phone), different methods should be invoked depending on call type.
    switch type {
      case .audio:
        callResult = callClient.callUser(withId: recipientIdentifier)
      case .video:
        callResult = callClient.videoCallToUser(withId: recipientIdentifier)
      case .phone:
        // To perform calls, CLI should be set and calling number provided.
        callResult = callClient.callPhoneNumber(recipientIdentifier)
    }

    switch callResult {
      case .success(let call):
        self.callRegistry.addSinchCall(call)
        self.callRegistry.map(uuid: action.conversationUUID, to: call.callId)

        // Assigning the delegate of the newly created SinchCall.
        // To track call establishment, progress and ending.
        call.delegate = self

        os_log("ConversationManager successfully fulfilled start action for call id: %{public}@",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               call.callId)

        action.fulfill()
      case .failure(let error):
        os_log("ConversationManager failed to start a call: %s",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .error,
               error.localizedDescription)

        action.fail()
    }

    callStartedCallback?(callResult)
  }

  private func perform(action: JoinConversationAction) {
    guard self.sinchClient != nil else {
      os_log("SinchClient not assigned to join call",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)
      action.fail()
      return
    }

    guard let call = self.callRegistry.sinchCall(from: action.conversationUUID) else {
      action.fail()
      return
    }

    os_log("ConversationManager successfully fulfilled join action for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           call.callId)

    call.answer()
    action.fulfill()
  }

  private func perform(action: EndConversationAction) {
    guard self.sinchClient != nil else {
      os_log("SinchClient not assigned to end call",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)
      action.fail()
      return
    }

    guard let call = self.callRegistry.sinchCall(from: action.conversationUUID) else {
      action.fail()
      return
    }

    os_log("ConversationManager successfully fulfilled end action for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           call.callId)

    call.hangup()
    action.fulfill()
  }

  func conversationManager(_ manager: ConversationManager, conversationChanged conversation: Conversation) {
    os_log("ConversationManager changed conversation uuid: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           conversation.uuid.uuidString)
  }

  func conversationManager(_ manager: ConversationManager, timedOutPerforming action: ConversationAction) {
    os_log("ConversationManager timed out performing action: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .error,
           String(describing: action))
  }

  func conversationManagerDidReset(_ manager: ConversationManager) {
    self.callRegistry.activeSinchCalls.forEach { $0.hangup() }
    self.callRegistry.reset()

    os_log("ConversationManager reset – hanging up all active calls",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info)
  }
}
