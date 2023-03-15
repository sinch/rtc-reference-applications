import UIKit

extension UIViewController {
    
    func prepareErrorAlert(with message: String) {
        let alert = UIAlertController(title: "Error", message: message, preferredStyle: .alert)
        let ok = UIAlertAction(title: "OK", style: .cancel)
        
        alert.addAction(ok)
        
        present(alert, animated: true)
    }
    
    func prepareActionSheet(title: String, message: String = "", actions: [UIAlertAction]) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .actionSheet)
        
        actions.forEach { alert.addAction($0) }
        
        self.present(alert, animated: true)
    }
}
