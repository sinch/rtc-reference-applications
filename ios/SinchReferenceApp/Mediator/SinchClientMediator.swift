import AVFoundation
import CallKit
import Foundation
import OSLog
import SinchRTC
import UIKit

protocol SinchClientMediatorObserver: SinchCallDelegate {}

// Create SinchClientMediatorDelegate to handle incoming calls.
protocol SinchClientMediatorDelegate: AnyObject {

  func handleIncomingCall(_ call: SinchCall)
}

// Create SinchClientMediator to be a wrapper and delegate for SinchClient.
final class SinchClientMediator: NSObject {
    
    enum CallType {
        
        case audio
        case video
        case phone
    }
    
    static let instance = SinchClientMediator()
    
    private(set) var userIdKey = "com.sinch.userId"
    
    var clientStartedCallback: ClientStartedCallback!
    
    private(set) var sinchClient: SinchClient?
    
    var callStartedCallback: CallStartedCallback!
    
    // An object interacts with calls by performing actions and observing calls.
    // Request the creation of new CallKit calls.
    private let callController = CXCallController()
    
    // Represents telephony provider.
    // Request the creation of new CallKit calls.
    private(set) var provider: CXProvider!
    
    // Maps Sinch's call Ids to CallKit's call Id.
    let callRegistry = CallRegistry()
    
    var observers: [SinchClientMediatorObserver?] = []
    
    // Create instance of delegate
    weak var delegate: SinchClientMediatorDelegate?
    
    private(set) var callTypes: [String: CallType] = [:]
    
    var currentCall: SinchCall? {
        callRegistry.activeSinchCalls.first {
            [.initiating, .established].contains($0.state)
        }
    }
    
    // To report and handle actions for outgoing and incoming calls,
    // should setup client delegate, and configure CallKit provider.
    private override init() {
        super.init()
        
        let providerConfiguration: CXProviderConfiguration
        
        if #available(iOS 14.0, *) {
            providerConfiguration = CXProviderConfiguration()
        } else {
            providerConfiguration = CXProviderConfiguration(localizedName: "sinch_ref_app")
        }
        // Identification of user for each call, .generic means that its based on uuid value.
        providerConfiguration.supportedHandleTypes = [.generic]
        // Value that indicates if call supports video.
        providerConfiguration.supportsVideo = true
        providerConfiguration.ringtoneSound = Ringtone.incoming

        self.provider = CXProvider(configuration: providerConfiguration)
        self.provider.setDelegate(self, queue: nil)
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
        // Conform the delegate to perform actions when mic was mute / unmute and speaker enabled / disabled.
        sinchClient.audioController.delegate = self
        sinchClient.start()
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
    private func createClientIfNeeded() {
        guard sinchClient == nil else { return }
        
        guard let userId = UserDefaults.standard.string(forKey: userIdKey) else {
            os_log("Failed to restore user from UserDefaults to create new SinchClient",
                   log: .sinchOSLog(for: SinchClientMediator.identifier))
            return
        }
        
        createAndStartClient(with: userId) { error in
            guard let error = error else {
                os_log("SinchClient started successfully for user: @{public}%, version: @{public}%",
                       log: .sinchOSLog(for: SinchClientMediator.identifier),
                       userId,
                       SinchRTC.version())
                return
            }
            
            os_log("SinchClient started with error: %{public}@",
                   log: .sinchOSLog(for: SinchClientMediator.identifier),
                   type: .error,
                   error.localizedDescription)
        }
    }
    
    // Action to start the call, after success
    // 'provider(_ provider: CXProvider, perform action: CXStartCallAction)'
    // event will be called.
    func call(destination userId: String, type: CallType, with callback: @escaping CallStartedCallback) {
        // Object associated with the call that will be used to identify the users involved with the call.
        let handle = CXHandle(type: .generic, value: userId)
        
        let startCallAction = CXStartCallAction(call: UUID(), handle: handle)
        startCallAction.isVideo = type == .video
        let startCallTransaction = CXTransaction(action: startCallAction)
        
        callStartedCallback = callback
        callTypes[startCallAction.callUUID.uuidString] = type
        
        // Request to start a call to CallKit.
        callController.request(startCallTransaction, completion: { [weak self] error in
            guard let self = self, let error = error else { return }
            
            os_log("Error requesting start call transaction: %{public}@",
                   log: .sinchOSLog(for: SinchClientMediator.identifier),
                   type: .error,
                   error.localizedDescription)
            
            DispatchQueue.main.async {
                self.callStartedCallback(.failure(error))
                self.callStartedCallback = nil
            }
        })
    }
    
    // Action to end the call, after success
    // 'provider(_ provider: CXProvider, perform action: CXEndCallAction)'
    // event will be called.
    func end(call: SinchCall) {
        // End a call by sinch call id.
        guard let uuid = callRegistry.callKitUUID(forSinchId: call.callId) else { return }
        
        let endCallAction = CXEndCallAction(call: uuid)
        let endCallTransaction = CXTransaction(action: endCallAction)
        
        // Request to end a call to CallKit.
        callController.request(endCallTransaction, completion: { [weak self]  error in
            guard let self = self else { return }
            
            if let error = error {
                os_log("Error requesting end call transaction: %{public}@",
                       log: .sinchOSLog(for: SinchClientMediator.identifier),
                       type: .error,
                       error.localizedDescription)
            }
            
            self.callStartedCallback = nil
        })
    }
    
    func logout(with completion: () -> Void) {
        defer {
            completion()
        }
        
        guard let client = sinchClient else { return }
        
        // Termination of client.
        // https://developers.sinch.com/docs/in-app-calling/ios/sinch-client/#life-cycle-management-of-a-sinclient-instance
        if client.isStarted {
            // Remove push registration from Sinch backend.
            client.unregisterPushNotificationDeviceToken()
            client.terminateGracefully()
        }
        
        sinchClient = nil
    }
}

// MARK: - Push notifications handling
extension SinchClientMediator {
    
    // Report new incoming call.
    func reportIncomingCall(with pushPayload: [AnyHashable: Any], and completion: @escaping (Error?) -> Void) {
        // Create client if user logged out / terminated app without unregistering device token.
        createClientIfNeeded()
        
        // Extract call information from the push payload.
        let notification = queryPushNotificationPayload(pushPayload)
        
        guard notification.isCall, notification.isValid else {
            os_log("Notification is call type: %{public}@, notification is valid: %{public}@",
                   log: .sinchOSLog(for: SinchClientMediator.identifier),
                   notification.isCall.description,
                   notification.isValid.description)
            
            return
        }
        
        let callNotification = notification.callResult
        
        guard callRegistry.callKitUUID(forSinchId: callNotification.callId) == nil else {
            os_log("Push notification for %{public}@ call was already processed",
                   log: .sinchOSLog(for: SinchClientMediator.identifier),
                   callNotification.callId)
            
            return
        }
        
        let callKitId = UUID()
        callRegistry.map(callKitId: callKitId, toSinchCallId: callNotification.callId)
        
        os_log("Reporting new incoming call with CallKit id: %{public}@ and push call id: %{public}@",
               log: .sinchOSLog(for: SinchClientMediator.identifier),
               callKitId.description,
               callNotification.callId)
        
        // Request SinchClientProvider to report new call to CallKit.
        let update = CXCallUpdate()
        
        update.remoteHandle = CXHandle(type: .generic, value: callNotification.remoteUserId)
        update.hasVideo = callNotification.isVideoOffered
        
        // Reporting the call to CallKit.
        provider.reportNewIncomingCall(with: callKitId, update: update, completion: { [weak self] (error: Error?) in
            guard let self = self else { return }
            
            if error != nil {
                // If we get an error here from the OS,
                // it is possibly the callee's phone has
                // "Do Not Disturb" turned on;
                // check CXErrorCodeIncomingCallError in CXError.h
                self.hangupCallOnError(with: callNotification.callId)
            }
            
            completion(error)
        })
    }
    
    // If error occured, just finish the call.
    private func hangupCallOnError(with callId: String) {
        guard let call = callRegistry.sinchCall(forCallId: callId) else {
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
