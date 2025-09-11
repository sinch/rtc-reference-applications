import AVFoundation
import Foundation
import OSLog
import SinchRTC
import UIKit

typealias CallType = SinchClientMediator.CallType

protocol SinchClientMediatorObserver: SinchCallDelegate {}

protocol SinchClientMediatorDelegate: AnyObject {

  func handleIncomingCall(_ call: SinchCall)
}

protocol ReloginDelegate: AnyObject {

  func didCreateClientOnIncomingCall(for client: SinchClient?)
}

protocol LogoutDelegate: AnyObject {

  func didLogout()
}

final class SinchClientMediator: NSObject {

  enum CallType: String {

    case audio
    case video
    case phone
  }

  var clientStartedCallback: ClientStartedCallback!

  private(set) var sinchClient: SinchClient?

  var callStartedCallback: CallStartedCallback!

  private(set) var communicationKit: CommunicationKit = .callKit
  private(set) var communicationService: CommunicationService?

  // Maps Sinch's call Ids to CallKit's call Id.
  let callRegistry = CallRegistry()

  var observers: [SinchClientMediatorObserver?] = []

  weak var delegate: SinchClientMediatorDelegate?

  weak var reloginDelegate: ReloginDelegate?
  weak var logoutDelegate: LogoutDelegate?

  private(set) var callTypes: [String: CallType] = [:]

  var currentCall: SinchCall? {
    callRegistry.activeSinchCalls.first {
      [.initiating, .progressing, .ringing, .answered, .established].contains($0.state)
    }
  }

  // Creating and starting a client for particular user.
  // https://developers.sinch.com/docs/in-app-calling/ios/sinch-client/#creating-the-sinclient
  func createAndStartClient(with userId: String, cli: String = "", and callback: @escaping (_ error: Error?) -> Void) {
    do {
      sinchClient = try SinchRTC.client(withApplicationKey: APPLICATION_KEY,
                                        environmentHost: ENVIRONMENT_HOST,
                                        userId: userId,
                                        // Setup CLI, number from which call will be performed.
                                        // If CLI is empty, when performing a PSTN call, call will fail.
                                        cli: cli)
    } catch let error as NSError {
      os_log("Failed to create sinchClient",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .info,
             error.localizedDescription)

      callback(error)
    }

    clientStartedCallback = callback

    guard let sinchClient = sinchClient else { return }

    sinchClient.delegate = self
    sinchClient.enableManagedPushNotifications()

    // To react to the creation of a SinchCall, after receiving notification,
    // mediator has to act as delegate of SinchCallClient.
    // https://developers.sinch.com/docs/in-app-calling/ios/sinch-client/#starting-the-sinclient
    sinchClient.callClient.delegate = self

    sinchClient.audioController.delegate = self
    sinchClient.start()

    UserInfo.save(userId: sinchClient.userId, cli: cli)
  }

  // This method is to be sure that client will be created
  // in case user logged out (i.e. terminated Sinch Client)
  // and hadn't unregistered Push Notification Token
  // via `sinchClient.unregisterPushNotificationDeviceToken()`.
  // In such scenario, this will help to create a new client on the fly to handle an incoming push.
  //
  // Example:
  // - 2 clients created on different devices.
  // - Client A is terminated without unregistering push notification device token.
  // - User B calls user A, and the device where user A was logged will receive a push notification.
  // - A client for User A should be created to handle the incoming call.
  private func createClientIfNeededOnIncomingCall() {
    guard sinchClient == nil else { return }

    let userInfo = UserInfo.load()

    guard !userInfo.userId.isEmpty else {
      os_log("Failed to restore user from UserDefaults to create new SinchClient",
             log: .sinchOSLog(for: SinchClientMediator.identifier))
      return
    }

    self.setupCommunication(with: CommunicationKit.load())

    createAndStartClient(with: userInfo.userId, cli: userInfo.cli) { [weak self] error in
      guard let self = self else { return }

      if let error = error {
        os_log("SinchClient started with error: %{public}@",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .error,
               error.localizedDescription)
      } else {
        os_log("SinchClient started successfully for user: %{public}@, version: %{public}@",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               userInfo.userId,
               SinchRTC.version())

        self.reloginDelegate?.didCreateClientOnIncomingCall(for: self.sinchClient)
      }
    }
  }

  func setupCommunication(with communicationKit: CommunicationKit) {
    self.communicationKit = communicationKit

    CommunicationKit.save(communicationKit: communicationKit)

    os_log("Setup communication kit: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           communicationKit.toString())

    guard #available(iOS 17.4, *), communicationKit == .liveCommunicationKit else {
      self.communicationService = SinchCallKitService(delegate: self)
      return
    }

    self.communicationService = SinchLiveCommunicationKitService(delegate: self)
  }

  func call(destination userId: String, type: CallType, with callback: @escaping CallStartedCallback) {
    let uuid = UUID()

    callStartedCallback = callback
    callTypes[uuid.uuidString] = type

    let errorCompletion: (Error?) -> Void = { [weak self] error in
      guard let self = self, let error = error else { return }

      os_log("Error requesting start call transaction: %{public}@",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             error.localizedDescription)

      DispatchQueue.main.async {
        self.callStartedCallback(.failure(error))
        self.callStartedCallback = nil
      }
    }

    os_log("Calling with %{public}@ for userId: %{public}@, uuid: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .info,
           self.communicationKit.toString(),
           userId,
           uuid.uuidString)

    guard let communicationService = self.communicationService else {
      os_log("%{public}@ service is not initialized",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             self.communicationKit.toString())
      return
    }

    communicationService.call(userId: userId, uuid: uuid, isVideo: type == .video, with: errorCompletion)
  }

  func end(call: SinchCall) {
    guard let uuid = callRegistry.uuid(from: call.callId) else { return }

    let errorCompletion: (Error?) -> Void = { [weak self] error in
      guard let self = self else { return }

      if let error = error {
        os_log("Error requesting end call transaction: %{public}@",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .error,
               error.localizedDescription)
      }

      self.callStartedCallback = nil
    }

    guard let communicationService = self.communicationService else {
      os_log("%{public}@ service is not initialized",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             self.communicationKit.toString())
      return
    }

    communicationService.end(uuid: uuid, with: errorCompletion)
  }

  func logout(with completion: () -> Void) {
    defer {
      completion()
    }

    UserInfo.clear()

    guard let client = sinchClient else { return }

    // Termination of client.
    // https://developers.sinch.com/docs/in-app-calling/ios/sinch-client/#life-cycle-management-of-a-sinclient-instance
    if client.isStarted {
      // Remove push registration from Sinch backend.
      client.unregisterPushNotificationDeviceToken()
      client.terminateGracefully()
    }

    sinchClient = nil
    communicationService = nil

    logoutDelegate?.didLogout()
  }
}

// MARK: - Push notifications handling

extension SinchClientMediator {

  func reportIncomingCall(with pushPayload: [AnyHashable: Any], and completion: @escaping (Error?) -> Void) {
    createClientIfNeededOnIncomingCall()

    let notification = queryPushNotificationPayload(pushPayload)

    guard notification.isCall, notification.isValid else {
      os_log("Notification is call type: %{public}@, notification is valid: %{public}@",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             notification.isCall.description,
             notification.isValid.description)

      return
    }

    let callNotification = notification.callResult
    let callId = callNotification.callId

    guard callRegistry.uuid(from: callId) == nil else {
      os_log("Push notification for %{public}@ call was already processed",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             callId)

      return
    }

    let uuid = UUID()
    callRegistry.map(uuid: uuid, to: callId)

    os_log("Reporting new incoming call with %{public}@ with uuid: %{public}@ and push call id: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           self.communicationKit.toString(),
           uuid.description,
           callId)

    let errorCompletion: (Error?) -> Void = self.handleIncomingCallError(for: callId, and: uuid, with: completion)

    guard let communicationService = self.communicationService else {
      os_log("%{public}@ service is not initialized",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             self.communicationKit.toString())
      return
    }

    let userInfo = UserInfo.load(with: "unknown")
    communicationService.reportIncomingCall(localUserId: userInfo.userId,
                                            remoteUserId: callNotification.remoteUserId,
                                            uuid: uuid,
                                            isVideoOffered: callNotification.isVideoOffered,
                                            with: errorCompletion)
  }

  private func handleIncomingCallError(for callId: String,
                                       and uuid: UUID,
                                       with completion: @escaping (Error?) -> Void) -> (Error?) -> Void {
    return { [weak self] error in
      guard let self = self else { return }

      // If we get an error here from the OS, it is possibly the callee's phone has "Do Not Disturb" turned on
      self.hangupCall(with: callId, on: error)
      completion(error)

      guard let communicationService = self.communicationService else {
        os_log("%{public}@ service is not initialized",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               type: .error,
               self.communicationKit.toString())
        return
      }

      if !self.callRegistry.activeSinchCalls.isEmpty {
        communicationService.declineIncomingCallIfBusy(uuid: uuid)
      }
    }
  }

  private func hangupCall(with callId: String, on error: Error?) {
    guard let error = error else { return }

    os_log("Call ended with error: %{public}@",
           log: .sinchOSLog(for: SinchClientMediator.identifier),
           type: .error,
           error.localizedDescription)

    guard let call = callRegistry.sinchCall(for: callId) else {
      os_log("Unable to find sinch call for callId: %{public}@",
             log: .sinchOSLog(for: SinchClientMediator.identifier),
             type: .error,
             callId)

      return
    }

    call.hangup()
    callRegistry.removeSinchCall(withId: callId)
  }
}
