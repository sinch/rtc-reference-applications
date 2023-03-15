import UIKit
import SinchRTC
import OSLog

final class LoginViewController: UIViewController {
    
    @IBOutlet private var userNameTextField: UITextField! {
        didSet {
            userNameTextField.delegate = self
            
            userNameTextField.keyboardType = .default
            userNameTextField.textContentType = .username
            userNameTextField.returnKeyType = .done
        }
    }
    
    @IBOutlet private var cliTextField: UITextField! {
        didSet {
            cliTextField.delegate = self
            
            cliTextField.keyboardType = .phonePad
            cliTextField.textContentType = .telephoneNumber
            cliTextField.returnKeyType = .done
        }
    }
    
    @IBOutlet private var noteLabel: UILabel! {
        didSet {
            noteLabel.numberOfLines = 10
            noteLabel.adjustsFontSizeToFitWidth = true
            noteLabel.lineBreakMode = .byClipping
            noteLabel.text = """
            \u{2022} For App2App(audio, video) calls just provide user name
            
            \u{2022} For App2PSTN calls, provide both user name and CLI number
            """
        }
    }
    
    @IBOutlet private var loginButton: UIButton!
    
    private var hasValidSinchAppConfiguration: Bool {
        APPLICATION_KEY != "<APPLICATION KEY>" && APPLICATION_SECRET != "<APPLICATION SECRET>"
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Background tap gesture is for numpad keyboard, since it doesn't have 'done' or 'return' key.
        let backgroundTapGesture = UITapGestureRecognizer(target: self, action: #selector(hideKeyboard))
        view.addGestureRecognizer(backgroundTapGesture)
    }
    
    @IBAction private func login(_ sender: Any) {
        guard hasValidSinchAppConfiguration else {
            prepareErrorAlert(with: "APPLICATION_KEY and APPLICATION_SECRET must be set to valid values in Config.swift")
            return
        }
        
        guard let name = userNameTextField.text, !name.isEmpty else {
            userNameTextField.placeholder = "Username couldn't be empty"
            
            return
        }
        
        // CLI (Connected Line Identification) is needed for PSTN calls, to provide an ID for the caller.
        // If empty, the call will most likely fail.
        let cli = cliTextField.text ?? ""
        
        // Sinch client registration for specific user.
        SinchClientMediator.instance.createAndStartClient(with: name, cli: cli) { [weak self] error in
            guard let self = self else { return }

            if (error != nil) {
                os_log("SinchClient started with error: %{public}@",
                       log: .sinchOSLog(for: LoginViewController.identifier),
                       type: .error,
                       error?.localizedDescription ?? "error")
                // If is fails, presents pop-up with error.
                self.prepareErrorAlert(with: error?.localizedDescription ?? "Not able to login")
            } else {
                os_log("SinchClient started successfully: (version:%{public}@)",
                       log: .sinchOSLog(for: LoginViewController.identifier),
                       SinchRTC.version())
                
                self.userNameTextField.resignFirstResponder()
                
                // If success, transfers to the next screen, to make a call.
                let mainViewController: MainViewController = self.prepareViewController(identifier: "main")

                mainViewController.userName = name
                mainViewController.cli = cli
                
                self.present(mainViewController, animated: true, completion: nil)
            }
        }
    }
    
    @objc private func hideKeyboard() {
        userNameTextField.resignFirstResponder()
        cliTextField.resignFirstResponder()
    }
}

extension LoginViewController: UITextFieldDelegate {
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        
        return true
    }
}
