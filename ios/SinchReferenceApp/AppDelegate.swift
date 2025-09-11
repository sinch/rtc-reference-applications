import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

  var window: UIWindow?

  private var appCoordinator: AppCoordinator?

  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    guard let rootController = window?.rootViewController else { return true }

    appCoordinator = AppCoordinator(rootViewController: rootController)
    appCoordinator?.start()

    return true
  }

  func applicationWillEnterForeground(_ application: UIApplication) {
    appCoordinator?.enterForeground()
  }
}
