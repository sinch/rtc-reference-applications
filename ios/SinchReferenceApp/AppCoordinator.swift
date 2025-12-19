import Combine
import OSLog
import SinchRTC
import UIKit

enum Navigation {

  case main
  case audioCall(SinchCall, CallType)
  case videoCall(SinchCall)
}

protocol NavigationDelegate: AnyObject {

  func navigate(to destination: Navigation)
}

final class AppCoordinator: NSObject {

  private var rootViewController: UIViewController

  private var sinchPush: SinchManagedPush?

  private var clientMediator: SinchClientMediator

  private var navigation = PassthroughSubject<Navigation, Never>()
  private var cancellableBag = Set<AnyCancellable>()

  init(rootViewController: UIViewController) {
    self.rootViewController = rootViewController

    self.clientMediator = SinchClientMediator()
  }

  func start() {
    setupLogger()

    // Configure push notifications.
    // https://developers.sinch.com/docs/in-app-calling/ios/push-notifications-callkit/#acquiring-a-push-device-token
    configurePushNotifications()

    // Assign delegate to handle incoming calls throught the application.
    clientMediator.delegate = self
    clientMediator.reloginDelegate = self

    navigation
      .receive(on: DispatchQueue.main)
      .sink { [weak self] destination in
        guard let self = self else { return }

        self.resolvePresentation(from: destination)
      }
      .store(in: &cancellableBag)

    prepareLoginViewController()
  }

  func enterForeground() {
    // If there is one established or initiating call, show the callView of the
    // current call when the App is brought to foreground. This is mainly to handle
    // the UI transition when clicking the App icon on the lockscreen CallKit UI,
    // and the UI transition when an incoming call is answered from homescreen CallKit UI.
    guard let activeCall = clientMediator.currentCall else { return }
    let topViewController = topViewController()
    
    if topViewController is AudioCallViewController || topViewController is VideoCallViewController {
      return
    }

    navigate(to: activeCall.details.isVideoOffered ? .videoCall(activeCall) : .audioCall(activeCall, .audio))
  }

  private func resolvePresentation(from destination: Navigation) {
    let currentViewController = topViewController()

    if currentViewController.presentedViewController is UIAlertController {
      currentViewController.presentedViewController?.dismiss(animated: true)
    }

    switch destination {
      case .main:
        presentMainViewController(from: currentViewController)
      case .audioCall(let call, let type):
        presentAudioCallViewController(for: call, type: type, from: currentViewController)
      case .videoCall(let call):
        presentVideoCallViewController(for: call, from: currentViewController)
    }
  }

  private func topViewController() -> UIViewController {
    var topViewController = rootViewController

    while let presentedViewController = topViewController.presentedViewController {
      topViewController = presentedViewController
    }

    return topViewController
  }

  private func prepareLoginViewController() {
    guard let loginViewController = rootViewController as? LoginViewController else {
      preconditionFailure("Error LoginViewController is expected")
    }

    loginViewController.viewModel = LoginViewModel(clientMediator: clientMediator)
    loginViewController.navigator = self

    os_log("Preparing LoginViewController for presentation", log: .sinchOSLog(for: AppCoordinator.identifier))
  }

  private func presentMainViewController(from currentViewController: UIViewController) {
    let mainViewController: MainViewController? = currentViewController.prepareViewController(identifier: "main")

    guard let mainViewController = mainViewController else {
      preconditionFailure("Error MainViewController is expected")
    }

    mainViewController.viewModel = MainViewModel(clientMediator: clientMediator)
    mainViewController.navigator = self

    os_log("Preparing MainViewController for presentation", log: .sinchOSLog(for: AppCoordinator.identifier))

    currentViewController.present(mainViewController, animated: true)
  }

  private func presentAudioCallViewController(for call: SinchCall,
                                              type: CallType,
                                              from currentViewController: UIViewController) {
    let audioCallViewController: AudioCallViewController? = currentViewController.prepareViewController(identifier: "call")

    guard let audioCallViewController = audioCallViewController else {
      preconditionFailure("Error AudioCallViewController is expected")
    }

    audioCallViewController.viewModel = CallViewModel(call: call, type: type, clientMediator: clientMediator)
    audioCallViewController.callCompletionDelegate = currentViewController

    os_log("Preparing AudioCallViewController for presentation, after incoming call [%{public}@] occured",
           log: .sinchOSLog(for: AppCoordinator.identifier),
           call.callId)

    currentViewController.present(audioCallViewController, animated: true)
  }

  private func presentVideoCallViewController(for call: SinchCall,
                                              from currentViewController: UIViewController) {
    let videoCallViewController: VideoCallViewController? = currentViewController.prepareViewController(identifier: "videoCall")

    guard let videoCallViewController = videoCallViewController else {
      preconditionFailure("Error VideoCallViewController is expected")
    }

    videoCallViewController.viewModel = CallViewModel(call: call, type: .video, clientMediator: clientMediator)
    videoCallViewController.callCompletionDelegate = currentViewController

    os_log("Preparing VideoCallViewController for presentation, after incoming call [%{public}@] occured",
           log: .sinchOSLog(for: AppCoordinator.identifier),
           call.callId)

    currentViewController.present(videoCallViewController, animated: true)
  }

  // Sinch Logging setup.
  private func setupLogger() {
    SinchRTC.setLogCallback { (severity: SinchRTC.LogSeverity, area: String, msg: String, _: Date) in
      os_log("%{public}@", log: .sinchOSLog(for: area), type: severity.osLogType, msg)
    }
  }

  // Connect Sinch Managed Push Service and Apple Push Notification service.
  // To add Signing Key go to Your Created App -> In-app Voice & Video SDKs -> ADD SIGNING KEY and upload your key there.
  private func configurePushNotifications() {
    sinchPush = SinchRTC.managedPush(forAPSEnvironment: .development)
    sinchPush?.delegate = self
    sinchPush?.setDesiredPushType(SinchManagedPush.TypeVoIP)
  }
}

// Conform to SinchManagedPushDelegate to handle VoIP notification.
extension AppCoordinator: SinchManagedPushDelegate {

  func managedPush(_ managedPush: SinchRTC.SinchManagedPush,
                   didReceiveIncomingPushWithPayload payload: [AnyHashable: Any],
                   for type: String) {
    os_log("Received incoming push notification with payload: %{public}@",
           log: .sinchOSLog(for: AppCoordinator.identifier),
           payload.description)

    clientMediator.reportIncomingCall(with: payload, and: { [weak self] error in
      DispatchQueue.main.async { [weak self] in
        // Forward the incoming push payload to Sinch client.
        self?.clientMediator.sinchClient?.relayPushNotification(withUserInfo: payload)
      }

      guard let error = error else { return }

      os_log("Error when reporting call to CallKit: %{public}@",
             log: .sinchOSLog(for: AppCoordinator.identifier),
             type: .error,
             error.localizedDescription)
    })
  }
}

extension AppCoordinator: NavigationDelegate {

  func navigate(to destination: Navigation) {
    navigation.send(destination)
  }
}

extension AppCoordinator: ReloginDelegate {

  // If the application was terminated and the user did not unregister the push notification
  // token (i.e., did not log out), the app will handle this scenario by creating a Sinch client on-the-fly
  // upon receiving a call. After the client is created, the user will be transferred to the MainViewController
  // and after that to the CallViewController.
  func didCreateClientOnIncomingCall(for client: SinchClient?) {
    navigate(to: .main)
  }
}

// Implementation of Mediator Delegate to handle incoming call.
extension AppCoordinator: SinchClientMediatorDelegate {

  func handleIncomingCall(_ call: SinchCall) {
    navigate(to: call.details.isVideoOffered ? .videoCall(call) : .audioCall(call, .audio))
  }
}
