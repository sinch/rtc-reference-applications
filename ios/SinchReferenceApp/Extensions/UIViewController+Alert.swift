import UIKit

extension UIViewController {

  func prepareAlert(title: String, message: String) {
    let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
    let okAction = UIAlertAction(title: "OK", style: .cancel)

    alert.addAction(okAction)

    present(alert, animated: true)
  }

  func prepareErrorAlert(with message: String) {
    prepareAlert(title: "Error", message: message)
  }

  func prepareActionSheet(title: String, message: String = "", actions: [UIAlertAction]) {
    let alert = UIAlertController(title: title, message: message, preferredStyle: .actionSheet)

    actions.forEach { alert.addAction($0) }

    self.present(alert, animated: true)
  }
}
