import Foundation
import SinchRTC

typealias ClientStartedCallback = (_ error: Error?) -> Void

// Extension to implement credential provisioning and monitoring of Sinch client status.
extension SinchClientMediator: SinchClientDelegate {

  // Client authorizing with JWT
  // https://developers.sinch.com/docs/in-app-calling/ios/sinch-client/#authorizing-the-client
  func clientRequiresRegistrationCredentials(_ client: SinchRTC.SinchClient,
                                             withCallback callback: SinchRTC.SinchClientRegistration) {
    do {
      // WARNING: test implementation to create JWT token, shouldn't be done for production application.
      let jwt = try SinchJWT.sinchJWTForUserRegistration(withApplicationKey: APPLICATION_KEY,
                                                         applicationSecret: APPLICATION_SECRET,
                                                         userId: client.userId)

      callback.register(withJWT: jwt)
    } catch {
      callback.registerDidFail(error: error)
    }
  }

  func clientDidStart(_ client: SinchClient) {
    guard clientStartedCallback != nil else { return }

    clientStartedCallback(nil)
    clientStartedCallback = nil
  }

  func clientDidFail(_ client: SinchClient, error: Error) {
    UserDefaults.standard.removeObject(forKey: SinchClientMediator.userIdKey)
    UserDefaults.standard.removeObject(forKey: SinchClientMediator.cliKey)

    guard clientStartedCallback != nil else { return }

    clientStartedCallback(error)
    clientStartedCallback = nil
  }
}
