import AVFoundation
import CallKit
import Foundation
import OSLog
import SinchRTC

typealias CallStartedCallback = (Result<SinchCall, Error>) -> Void

extension SinchClientMediator: CXProviderDelegate {
  // To change audio session active state.
  func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
    sinchClient?.callClient.provider(provider: provider, didActivateAudioSession: audioSession)
  }

  // To change audio session active state.
  func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
    sinchClient?.callClient.provider(provider: provider, didDeactivateAudioSession: audioSession)
  }

  // CallKit starting action to begin a Sinch Call.
  func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
    defer {
      callStartedCallback = nil
    }

    guard let callClient = sinchClient?.callClient else {
      action.fail()
      callStartedCallback?(.failure(CallError.noClient))
      return
    }

    guard let callType = callTypes[action.callUUID.uuidString] else {
      action.fail()
      callStartedCallback?(.failure(CallError.noCallType(action.callUUID.uuidString)))
      return
    }

    let recipientIdentifier = action.handle.value

    let callResult: Result<SinchCall, Error>

    // To perform different calls (audio, video, phone), different methods should be invoked depending on call type.
    switch callType {
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
      callRegistry.addSinchCall(call)
      callRegistry.map(callKitId: action.callUUID, toSinchCallId: call.callId)

      // Assigning the delegate of the newly created SinchCall.
      // To track call establishment, progress and ending.
      call.delegate = self

      action.fulfill()
    case .failure(let error):
      os_log("Unable to make a call: %s",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             error.localizedDescription)

      action.fail()
    }

    callStartedCallback?(callResult)
  }

  // To be able to answer the call we need to implement method below.
  func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
    guard sinchClient != nil else {
      os_log("SinchClient not assigned when CXAnswerCallAction. Failing action",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)

      action.fail()

      return
    }

    // Fetching SinchCall from registry.
    guard let call = callRegistry.sinchCall(forCallKitUUID: action.callUUID) else {
      action.fail()

      return
    }

    os_log("Provider perform action: CXAnswerCallAction: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           call.callId)

    sinchClient?.audioController.configureAudioSessionForCallKitCall()

    call.answer()
    action.fulfill()
  }

  // CallKit ending action to finish a Sinch Call.
  func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
    guard sinchClient != nil else {
      os_log("SinchClient not assigned when CXEndCallAction. Failing action",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error)

      action.fail()

      return
    }

    guard let call = callRegistry.sinchCall(forCallKitUUID: action.callUUID) else {
      action.fail()

      return
    }

    os_log("Provider perform action: CXEndCallAction: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           call.callId)

    call.hangup()
    action.fulfill()
  }

  func providerDidReset(_ provider: CXProvider) {
    // End any ongoing calls if the provider resets, and remove them from the app's list of calls
    // because they are no longer valid.
    callRegistry.activeSinchCalls.forEach { $0.hangup() }
    // Remove all calls from the app's list of calls.
    callRegistry.reset()
  }
}
