import SinchRTC
import UIKit

extension UIViewController {

  func prepareViewController<T: UIViewController>(identifier: String, in storyboard: String = "Main") -> T {
    let storyboard = UIStoryboard(name: storyboard, bundle: nil)

    guard let viewController = storyboard.instantiateViewController(withIdentifier: identifier) as? T else {
      preconditionFailure("View controller \(identifier) view controller is expected")
    }

    return viewController
  }
}
