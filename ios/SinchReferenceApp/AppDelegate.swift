import OSLog
import SinchRTC
import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

  var window: UIWindow?

  // Create instance to enable push notification.
  private var sinchPush: SinchManagedPush?

  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    // Sinch Logging setup.
    setupLogger()

    // Sinch Client Mediator is a singleton to have a single instance through the whole application.
    // Assign delegate to handle incoming calls throught the application in AppDelegate.
    SinchClientMediator.instance.delegate = self
    SinchClientMediator.instance.reloginDelegate = self

    guard self.window?.rootViewController is LoginViewController else {
      preconditionFailure("Error LoginViewController is expected")
    }

    // Configure push notifications.
    // https://developers.sinch.com/docs/in-app-calling/ios/push-notifications-callkit/#acquiring-a-push-device-token
    configurePushNotifications()

    return true
  }

  func applicationWillEnterForeground(_ application: UIApplication) {
    // If there is one established or initiating call, show the callView of the
    // current call when the App is brought to foreground. This is mainly to handle
    // the UI transition when clicking the App icon on the lockscreen CallKit UI,
    // and the UI transition when an incoming call is answered from homescreen CallKit UI.
    guard let activeCall = SinchClientMediator.instance.currentCall else { return }

    transitionToCallViewController(for: activeCall)
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
extension AppDelegate: SinchManagedPushDelegate {

  func managedPush(_ managedPush: SinchRTC.SinchManagedPush,
                   didReceiveIncomingPushWithPayload payload: [AnyHashable: Any],
                   for type: String) {
    os_log("Received incoming push notification with payload: %{public}@",
           log: .sinchOSLog(for: AppDelegate.identifier),
           payload.description)

    SinchClientMediator.instance.reportIncomingCall(with: payload, and: { error in
      DispatchQueue.main.async {
        // Forward the incoming push payload to Sinch client.
        SinchClientMediator.instance.sinchClient?.relayPushNotification(withUserInfo: payload)
      }

      guard let error = error else { return }

      os_log("Error when reporting call to CallKit: %{public}@",
             log: .sinchOSLog(for: AppDelegate.identifier),
             type: .error,
             error.localizedDescription)
    })
  }
}

// Implementation of Mediator Delegate to handle incoming call.
extension AppDelegate: SinchClientMediatorDelegate {
  // Navigate to call controller during incoming call.
  func handleIncomingCall(_ call: SinchCall) {
    transitionToCallViewController(for: call)
  }

  private func transitionToCallViewController(for call: SinchCall) {
    guard let rootViewController = window?.rootViewController else { return }

    let presentedViewController = rootViewController.presentedViewController ?? rootViewController

    // In case Call Details or Error Alerts are presented, alerts should be dismissed
    if presentedViewController.presentedViewController is UIAlertController {
      presentedViewController.presentedViewController?.dismiss(animated: true)
    }

    // If it is video call, depending on call type presents different viewControllers: audio or video.
    let presentingViewController = call.details.isVideoOffered
    ? prepareVideoCallViewController(for: call, presentedController: presentedViewController)
    : prepareAudioCallViewController(for: call, presentedController: presentedViewController)

    guard let presentingViewController = presentingViewController else { return }

    presentedViewController.present(presentingViewController, animated: true)
  }

  private func prepareAudioCallViewController(for call: SinchCall,
                                              presentedController: UIViewController) -> AudioCallViewController? {
    let audioCallViewController = presentedController.prepareViewController(identifier: "call") as? AudioCallViewController

    audioCallViewController?.call = call
    audioCallViewController?.callCompletionDelegate = presentedController

    os_log("Preparing AudioCallViewController for presentation, after incoming call [%{public}@] occured",
           log: .sinchOSLog(for: AppDelegate.identifier),
           call.callId)

    return audioCallViewController
  }

  private func prepareVideoCallViewController(for call: SinchCall,
                                              presentedController: UIViewController) -> VideoCallViewController? {
    let videoCallViewController = presentedController.prepareViewController(identifier: "videoCall") as? VideoCallViewController

    videoCallViewController?.call = call
    videoCallViewController?.callCompletionDelegate = presentedController

    os_log("Preparing VideoCallViewController for presentation, after incoming call [%{public}@] occured",
           log: .sinchOSLog(for: AppDelegate.identifier),
           call.callId)

    return videoCallViewController
  }
}

extension AppDelegate: ReloginDelegate {

  // If the application was terminated and the user did not unregister the push notification
  // token (i.e., did not log out), the app will handle this scenario by creating a Sinch client on-the-fly
  // upon receiving a call. After the client is created, the user will be transferred to the MainViewController
  // and after that to the CallViewController.
  func didCreateClientOnIncomingCall(for client: SinchClient?) {
    guard let rootViewController = window?.rootViewController, let client = client else { return }

    let presentedViewController = rootViewController.presentedViewController ?? rootViewController

    let mainViewController = presentedViewController.prepareViewController(identifier: "main") as? MainViewController

    guard let loginViewController = presentedViewController as? LoginViewController,
          let mainViewController = mainViewController else { return }

    mainViewController.userName = client.userId
    mainViewController.cli = UserDefaults.standard.string(forKey: SinchClientMediator.cliKey) ?? ""

    loginViewController.present(mainViewController, animated: true)
  }
}

// Sinch Logging extending with Sinch Log Type.
extension LogSeverity {

  var osLogType: OSLogType {
    switch self {
    case .info, .warning: return .default
    case .critical: return .fault
    case .trace: return .debug
    default: return .default
    }
  }
}

extension OSLog {

  // Predefined log for Sinch, where category is name of the class from where logger was called.
  static func sinchOSLog(for category: String) -> OSLog {
    return OSLog(subsystem: "com.sinch.sdk.app", category: category)
  }
}
