import AVFoundation
import CallKit
import Foundation
import OSLog
import SinchRTC

typealias CallStartedCallback = (Result<SinchCall, Error>) -> Void

extension SinchClientMediator: CXProviderDelegate {

  func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
    self.sinchClient?.callClient.didActivate(audioSession: audioSession)
  }

  func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
    self.sinchClient?.callClient.didDeactivate(audioSession: audioSession)
  }

  func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
    defer { callStartedCallback = nil }

    guard let callClient = self.sinchClient?.callClient else {
      os_log("SinchClient not assigned to start call",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)
      action.fail()
      callStartedCallback?(.failure(CallError.noClient))
      return
    }

    guard let type = callTypes[action.callUUID.uuidString] else {
      action.fail()
      callStartedCallback?(.failure(CallError.noCallType(action.callUUID.uuidString)))
      return
    }

    let recipientIdentifier = action.handle.value
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
        self.callRegistry.map(uuid: action.callUUID, to: call.callId)

        // Assigning the delegate of the newly created SinchCall.
        // To track call establishment, progress and ending.
        call.delegate = self

        os_log("Provider successfully fulfilled start action for call id: %{public}@",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               call.callId)

        action.fulfill()
      case .failure(let error):
        os_log("Provider failed to start a call: %{public}@",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .error,
               error.localizedDescription)

        action.fail()
    }

    callStartedCallback?(callResult)
  }

  func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
    guard self.sinchClient != nil else {
      os_log("SinchClient not assigned to answer call",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)
      action.fail()
      return
    }

    guard let call = self.callRegistry.sinchCall(from: action.callUUID) else {
      action.fail()
      return
    }

    os_log("Provider successfully fulfilled answer action for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           call.callId)

    self.sinchClient?.audioController.configureAudioSessionForCallKitCall()

    call.answer()
    action.fulfill()
  }

  func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
    guard self.sinchClient != nil else {
      os_log("SinchClient not assigned to end call",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)

      action.fail()
      return
    }

    guard let call = self.callRegistry.sinchCall(from: action.callUUID) else {
      action.fail()
      return
    }

    os_log("Provider successfully fulfilled end action for call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           call.callId)

    call.hangup()
    action.fulfill()
  }

  func providerDidReset(_ provider: CXProvider) {
    self.callRegistry.activeSinchCalls.forEach { $0.hangup() }
    self.callRegistry.reset()

    os_log("Provider reset – hanging up all active calls",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info)
  }
}
